package com.exclusivostars.app.ui.inicio

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Pelicula
import com.exclusivostars.app.model.Serie
import com.exclusivostars.app.network.PeliculasApi
import com.exclusivostars.app.network.SeriesApi
import com.exclusivostars.app.ui.pelicula.PeliculaDetalleActivity
import com.exclusivostars.app.util.Etiqueta

/**
 * Portada estilo streaming, mismo espíritu que index.html en la web
 * (hero + filas por categoría) pero con los patrones nativos de
 * siempre en vez de portar el CSS/JS: ViewPager2 para el hero
 * rotativo, RecyclerViews horizontales anidados para las filas.
 *
 * A propósito solo trae Películas y Series: se decidió dejar Reels y
 * Música fuera del alcance de la app Android (insostenible mantener
 * todo eso en un cliente nativo aparte). Si algún día cambia, se
 * agregan como filas nuevas reusando FilaAdapter/FilaItem tal cual.
 */
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var heroAdapter: HeroAdapter
    private lateinit var filaPeliculasAdapter: FilaAdapter
    private lateinit var filaSeriesAdapter: FilaAdapter

    private lateinit var scroll: View
    private lateinit var heroPager: ViewPager2
    private lateinit var progress: ProgressBar
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView
    private lateinit var tituloSeries: View
    private lateinit var filaSeries: RecyclerView

    private val rotarHero = object : Runnable {
        override fun run() {
            val adapter = heroPager.adapter ?: return
            if (adapter.itemCount == 0) return
            heroPager.currentItem = (heroPager.currentItem + 1) % adapter.itemCount
            main.postDelayed(this, 6000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scroll = view.findViewById(R.id.scroll_inicio)
        heroPager = view.findViewById(R.id.hero_pager)
        progress = view.findViewById(R.id.progress)
        errorState = view.findViewById(R.id.error_state)
        errorMessage = view.findViewById(R.id.error_message)
        tituloSeries = view.findViewById(R.id.titulo_series)
        filaSeries = view.findViewById(R.id.fila_series)
        val filaPeliculas = view.findViewById<RecyclerView>(R.id.fila_peliculas)

        heroAdapter = HeroAdapter { pelicula ->
            startActivity(PeliculaDetalleActivity.crearIntent(requireContext(), pelicula.nombre))
        }
        heroPager.adapter = heroAdapter

        filaPeliculasAdapter = FilaAdapter()
        filaPeliculas.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        filaPeliculas.adapter = filaPeliculasAdapter

        filaSeriesAdapter = FilaAdapter()
        filaSeries.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        filaSeries.adapter = filaSeriesAdapter

        view.findViewById<Button>(R.id.retry_button).setOnClickListener { cargar() }

        cargar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        main.removeCallbacks(rotarHero)
    }

    private fun cargar() {
        mostrarCargando()
        Thread {
            val resultPeliculas = PeliculasApi.listar()
            val resultSeries = SeriesApi.listar()
            main.post {
                if (!isAdded) return@post
                val peliculas = (resultPeliculas as? PeliculasApi.Result.Ok)?.peliculas
                if (peliculas == null) {
                    val mensaje = (resultPeliculas as PeliculasApi.Result.Error).mensaje
                    mostrarError(mensaje)
                    return@post
                }
                val series = (resultSeries as? SeriesApi.Result.Ok)?.series.orEmpty()
                mostrarResultado(peliculas, series)
            }
        }.start()
    }

    private fun mostrarCargando() {
        progress.visibility = View.VISIBLE
        scroll.visibility = View.GONE
        errorState.visibility = View.GONE
    }

    private fun mostrarResultado(peliculas: List<Pelicula>, series: List<Serie>) {
        progress.visibility = View.GONE
        errorState.visibility = View.GONE

        val candidatas = peliculas.filter { it.fondoUrl != null || it.posterUrl != null }
        val conLogo = candidatas.filter { it.logoUrl != null }.shuffled()
        val sinLogo = candidatas.filter { it.logoUrl == null }.shuffled()
        val candidatasHero = (conLogo + sinLogo).take(6)
        heroAdapter.submitList(candidatasHero)
        main.removeCallbacks(rotarHero)
        if (candidatasHero.size > 1) main.postDelayed(rotarHero, 6000)

        filaPeliculasAdapter.submitList(peliculas.map { pelicula ->
            FilaItem(
                id = pelicula.nombre,
                titulo = pelicula.titulo,
                etiqueta = Etiqueta.formatear(pelicula.pais, pelicula.anio, "Película"),
                posterUrl = pelicula.posterUrl,
                onClick = { startActivity(PeliculaDetalleActivity.crearIntent(requireContext(), pelicula.nombre)) },
            )
        })

        if (series.isNotEmpty()) {
            tituloSeries.visibility = View.VISIBLE
            filaSeries.visibility = View.VISIBLE
            filaSeriesAdapter.submitList(series.map { serie ->
                FilaItem(
                    id = serie.slug,
                    titulo = serie.titulo,
                    etiqueta = Etiqueta.formatear(serie.pais, serie.anio, "Serie"),
                    posterUrl = serie.posterUrl,
                    // Mismo motivo que en SeriesFragment: sin ficha propia
                    // todavía (falta el endpoint JSON de detalle).
                    onClick = {},
                )
            })
        }

        scroll.alpha = 0f
        scroll.visibility = View.VISIBLE
        scroll.animate().alpha(1f).setDuration(280).start()
    }

    private fun mostrarError(mensaje: String) {
        progress.visibility = View.GONE
        scroll.visibility = View.GONE
        errorMessage.text = mensaje
        errorState.visibility = View.VISIBLE
    }
}
