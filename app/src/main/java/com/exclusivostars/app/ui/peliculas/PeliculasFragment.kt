package com.exclusivostars.app.ui.peliculas

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exclusivostars.app.R
import com.exclusivostars.app.model.Pelicula
import com.exclusivostars.app.network.PeliculasApi
import com.exclusivostars.app.ui.pelicula.PeliculaDetalleActivity

class PeliculasFragment : Fragment(R.layout.fragment_peliculas) {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var adapter: PeliculasAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView
    private lateinit var emptyState: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_peliculas)
        progress = view.findViewById(R.id.progress)
        errorState = view.findViewById(R.id.error_state)
        errorMessage = view.findViewById(R.id.error_message)
        emptyState = view.findViewById(R.id.empty_state)

        adapter = PeliculasAdapter { pelicula ->
            startActivity(PeliculaDetalleActivity.crearIntent(requireContext(), pelicula.nombre))
        }
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        view.findViewById<Button>(R.id.retry_button).setOnClickListener { cargar() }

        cargar()
    }

    private fun cargar() {
        mostrarCargando()
        Thread {
            val result = PeliculasApi.listar()
            main.post {
                if (!isAdded) return@post // el fragment pudo haberse destruido mientras cargaba
                when (result) {
                    is PeliculasApi.Result.Ok -> mostrarResultado(result.peliculas)
                    is PeliculasApi.Result.Error -> mostrarError(result.mensaje)
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

    private fun mostrarResultado(peliculas: List<Pelicula>) {
        progress.visibility = View.GONE
        errorState.visibility = View.GONE

        if (peliculas.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            return
        }

        adapter.submitList(peliculas)
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
