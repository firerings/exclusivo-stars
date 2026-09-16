package com.exclusivostars.app.ui.series

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Serie
import com.exclusivostars.app.network.SeriesApi

/**
 * Mismo patrón exacto que PeliculasFragment. Reemplaza al
 * ComingSoonFragment que tenía la pestaña Series hasta ahora — ya usa
 * /api/series (que el backend viene sirviendo desde el principio, ver
 * SeriesApi.kt) para la grilla.
 *
 * El click en una serie todavía no navega a ninguna ficha: a
 * diferencia de /api/pelicula/<nombre>, el backend no tiene un
 * endpoint JSON equivalente para el detalle de una serie (temporadas/
 * episodios) — está pendiente del lado del servidor, ver conversación
 * de diseño. Cuando exista, esto pasa a abrir una SerieDetalleActivity
 * igual que PeliculaDetalleActivity.
 */
class SeriesFragment : Fragment(R.layout.fragment_series) {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var adapter: SeriesAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView
    private lateinit var emptyState: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_series)
        progress = view.findViewById(R.id.progress)
        errorState = view.findViewById(R.id.error_state)
        errorMessage = view.findViewById(R.id.error_message)
        emptyState = view.findViewById(R.id.empty_state)

        adapter = SeriesAdapter { serie -> onClickSerie(serie) }
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        view.findViewById<Button>(R.id.retry_button).setOnClickListener { cargar() }

        cargar()
    }

    private fun onClickSerie(serie: Serie) {
        // TODO: navegar a la ficha de la serie una vez que el backend
        // tenga un endpoint JSON de detalle (temporadas/episodios).
    }

    private fun cargar() {
        mostrarCargando()
        Thread {
            val result = SeriesApi.listar()
            main.post {
                if (!isAdded) return@post // el fragment pudo haberse destruido mientras cargaba
                when (result) {
                    is SeriesApi.Result.Ok -> mostrarResultado(result.series)
                    is SeriesApi.Result.Error -> mostrarError(result.mensaje)
                }
            }
        }.start()
    }

    private fun mostrarCargando() {
        progress.visibility = View.VISIBLE
        recycler.visibility = View.GONE
        errorState.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun mostrarResultado(series: List<Serie>) {
        progress.visibility = View.GONE
        errorState.visibility = View.GONE

        if (series.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            return
        }

        adapter.submitList(series)
        emptyState.visibility = View.GONE
        recycler.alpha = 0f
        recycler.visibility = View.VISIBLE
        recycler.animate().alpha(1f).setDuration(280).start()
    }

    private fun mostrarError(mensaje: String) {
        progress.visibility = View.GONE
        recycler.visibility = View.GONE
        emptyState.visibility = View.GONE
        errorMessage.text = mensaje
        errorState.visibility = View.VISIBLE
    }
}
