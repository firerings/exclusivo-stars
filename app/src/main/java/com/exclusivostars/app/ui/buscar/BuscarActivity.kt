package com.exclusivostars.app.ui.buscar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Pelicula
import com.exclusivostars.app.model.Serie
import com.exclusivostars.app.network.PeliculasApi
import com.exclusivostars.app.network.SeriesApi
import com.exclusivostars.app.ui.inicio.FilaAdapter
import com.exclusivostars.app.ui.inicio.FilaItem
import com.exclusivostars.app.ui.pelicula.PeliculaDetalleActivity

/**
 * Búsqueda por título sobre películas y series — mismo alcance que
 * Inicio (ver InicioFragment: Reels y Música todavía no tienen /api
 * propia). A diferencia de /buscar en la web (que cruza personas,
 * reels y canciones con varias secciones de resultados), acá se filtra
 * en el cliente sobre el catálogo que ya devuelven PeliculasApi/
 * SeriesApi — no hace falta un endpoint de búsqueda propio para esto,
 * y evita duplicar la lógica multi-tipo de app.buscar() en Kotlin.
 *
 * Reusa FilaAdapter/FilaItem (ya genéricos por diseño, ver
 * InicioFragment) en vez de un adapter nuevo — mismo criterio de
 * "una sola clase para pintar una tarjeta", ahora en grilla en vez de
 * fila horizontal.
 */
class BuscarActivity : AppCompatActivity() {

    private val main = Handler(Looper.getMainLooper())
    private val adapter = FilaAdapter()

    private var peliculas: List<Pelicula> = emptyList()
    private var series: List<Serie> = emptyList()
    private var catalogoListo = false

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var campoBuscar: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar)

        recycler = findViewById(R.id.recycler_resultados)
        progress = findViewById(R.id.progress)
        emptyState = findViewById(R.id.empty_state)
        campoBuscar = findViewById(R.id.campo_buscar)

        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = adapter

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }

        campoBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = filtrar(s?.toString().orEmpty())
        })

        cargarCatalogo()
    }

    private fun cargarCatalogo() {
        progress.visibility = View.VISIBLE
        Thread {
            val resultPeliculas = PeliculasApi.listar()
            val resultSeries = SeriesApi.listar()
            main.post {
                if (isFinishing) return@post
                progress.visibility = View.GONE
                peliculas = (resultPeliculas as? PeliculasApi.Result.Ok)?.peliculas.orEmpty()
                series = (resultSeries as? SeriesApi.Result.Ok)?.series.orEmpty()
                catalogoListo = true
                filtrar(campoBuscar.text.toString())
            }
        }.start()
    }

    /** Mismo mínimo de 2 caracteres que /buscar en la web — antes de
     * eso no muestra ni "sin resultados", solo la grilla vacía. */
    private fun filtrar(query: String) {
        if (!catalogoListo) return
        val q = query.trim().lowercase()
        if (q.length < 2) {
            adapter.submitList(emptyList())
            recycler.visibility = View.GONE
            emptyState.visibility = View.GONE
            return
        }

        val items = buildList {
            peliculas.filter { it.titulo.lowercase().contains(q) }.forEach { pelicula ->
                add(FilaItem(
                    id = pelicula.nombre,
                    titulo = pelicula.titulo,
                    subtitulo = pelicula.anio,
                    tipo = "Película",
                    posterUrl = pelicula.posterUrl,
                    onClick = { startActivity(PeliculaDetalleActivity.crearIntent(this@BuscarActivity, pelicula.nombre)) },
                ))
            }
            series.filter { it.titulo.lowercase().contains(q) }.forEach { serie ->
                add(FilaItem(
                    id = serie.slug,
                    titulo = serie.titulo,
                    subtitulo = serie.anio,
                    tipo = "Serie",
                    posterUrl = serie.posterUrl,
                    // Todavía no hay SerieDetalleActivity — mismo criterio
                    // que la fila de Series en InicioFragment.
                    onClick = {},
                ))
            }
        }

        if (items.isEmpty()) {
            recycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            adapter.submitList(items)
            emptyState.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }
    }
}
