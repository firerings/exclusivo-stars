package com.exclusivostars.app.ui.peliculas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exclusivostars.app.R
import com.exclusivostars.app.network.PeliculasApi
import com.exclusivostars.app.ui.auth.MainActivity

class PeliculasActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var adapter: PeliculasAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView
    private lateinit var emptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peliculas)

        recycler = findViewById(R.id.recycler_peliculas)
        progress = findViewById(R.id.progress)
        errorState = findViewById(R.id.error_state)
        errorMessage = findViewById(R.id.error_message)
        emptyState = findViewById(R.id.empty_state)

        adapter = PeliculasAdapter { pelicula ->
            // TODO: abrir ficha de la película cuando exista esa pantalla.
            // Por ahora, confirmación mínima de que el click y los datos
            // reales llegaron bien de punta a punta.
            Toast.makeText(this, pelicula.titulo, Toast.LENGTH_SHORT).show()
        }
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = adapter

        findViewById<ImageButton>(R.id.logout_button).setOnClickListener {
            MainActivity.cookieManager.cookieStore.removeAll()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.retry_button).setOnClickListener { cargar() }

        cargar()
    }

    private fun cargar() {
        mostrarCargando()
        Thread {
            val result = PeliculasApi.listar()
            main.post {
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

    private fun mostrarResultado(peliculas: List<com.exclusivostars.app.model.Pelicula>) {
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
