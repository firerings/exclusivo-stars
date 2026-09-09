package com.exclusivostars.app.ui.pelicula

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.PeliculaDetalle
import com.exclusivostars.app.network.PeliculaApi
import com.exclusivostars.app.ui.player.PlayerActivity
import kotlin.math.roundToInt

class PeliculaDetalleActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_NOMBRE = "nombre"

        fun crearIntent(context: Context, nombre: String): Intent =
            Intent(context, PeliculaDetalleActivity::class.java).putExtra(EXTRA_NOMBRE, nombre)
    }

    private val main = Handler(Looper.getMainLooper())

    private lateinit var scroll: NestedScrollView
    private lateinit var progress: android.widget.ProgressBar
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView

    private lateinit var heroFondo: ImageView
    private lateinit var heroTarjeta: FrameLayout
    private lateinit var heroTarjetaImg: ImageView
    private lateinit var titulo: TextView
    private lateinit var meta: TextView
    private lateinit var director: TextView
    private lateinit var puntuacion: TextView
    private lateinit var avisoDirecto: TextView
    private lateinit var sinopsis: TextView
    private lateinit var repartoTitulo: TextView
    private lateinit var recyclerReparto: RecyclerView

    private lateinit var nombre: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelicula_detalle)

        nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: throw IllegalStateException("PeliculaDetalleActivity requiere el extra '$EXTRA_NOMBRE'")

        scroll = findViewById(R.id.scroll)
        progress = findViewById(R.id.progress)
        errorState = findViewById(R.id.error_state)
        errorMessage = findViewById(R.id.error_message)

        heroFondo = findViewById(R.id.hero_fondo)
        heroTarjeta = findViewById(R.id.hero_tarjeta)
        heroTarjetaImg = findViewById(R.id.hero_tarjeta_img)
        titulo = findViewById(R.id.titulo)
        meta = findViewById(R.id.meta)
        director = findViewById(R.id.director)
        puntuacion = findViewById(R.id.puntuacion)
        avisoDirecto = findViewById(R.id.aviso_directo)
        sinopsis = findViewById(R.id.sinopsis)
        repartoTitulo = findViewById(R.id.reparto_titulo)
        recyclerReparto = findViewById(R.id.recycler_reparto)
        recyclerReparto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        findViewById<Button>(R.id.retry_button).setOnClickListener { cargar() }

        cargar()
    }

    private fun cargar() {
        mostrarCargando()
        Thread {
            val result = PeliculaApi.obtener(nombre)
            main.post {
                if (isFinishing) return@post
                when (result) {
                    is PeliculaApi.Result.Ok -> mostrarFicha(result.pelicula)
                    is PeliculaApi.Result.Error -> mostrarError(result.mensaje)
                }
            }
        }.start()
    }

    private fun mostrarCargando() {
        progress.visibility = View.VISIBLE
        scroll.visibility = View.GONE
        errorState.visibility = View.GONE
    }

    private fun mostrarError(mensaje: String) {
        progress.visibility = View.GONE
        scroll.visibility = View.GONE
        errorMessage.text = mensaje
        errorState.visibility = View.VISIBLE
    }

    private fun mostrarFicha(pelicula: PeliculaDetalle) {
        progress.visibility = View.GONE
        errorState.visibility = View.GONE
        scroll.visibility = View.VISIBLE

        Glide.with(heroFondo).load(pelicula.fondoUrl).centerCrop().into(heroFondo)
        Glide.with(heroTarjetaImg)
            .load(pelicula.tarjetaUrl)
            .placeholder(R.drawable.poster_placeholder_background)
            .error(R.drawable.poster_placeholder_background)
            .centerCrop()
            .into(heroTarjetaImg)
        heroTarjeta.setOnClickListener { PlayerActivity.iniciar(this, pelicula) }

        titulo.text = pelicula.titulo

        // Mismo orden que pelicula-detalle-meta en la web: duración · género,
        // o "Sin datos todavía" si no hay ninguno de los dos.
        val partesMeta = buildList {
            pelicula.duracion?.let { add(it) }
            if (pelicula.genero.isNotEmpty()) add(pelicula.genero.joinToString(", "))
        }
        meta.text = if (partesMeta.isNotEmpty()) partesMeta.joinToString(" · ") else getString(R.string.sin_datos_meta)

        if (pelicula.director != null) {
            director.text = getString(R.string.dirigida_por, pelicula.director)
            director.visibility = View.VISIBLE
        } else {
            director.visibility = View.GONE
        }

        if (pelicula.puntuacion != null && pelicula.puntuacion > 0) {
            val pct = (pelicula.puntuacion * 10).roundToInt()
            puntuacion.text = "$pct%"
            puntuacion.visibility = View.VISIBLE
        } else {
            puntuacion.visibility = View.GONE
        }

        if (pelicula.modoDirecto) {
            avisoDirecto.text = if (pelicula.audioTexto != null) {
                getString(R.string.aviso_directo_con_audio, pelicula.audioTexto)
            } else {
                getString(R.string.aviso_directo_sin_audio)
            }
            avisoDirecto.visibility = View.VISIBLE
        } else {
            avisoDirecto.visibility = View.GONE
        }

        sinopsis.text = if (!pelicula.sinopsis.isNullOrBlank()) pelicula.sinopsis else getString(R.string.sin_sinopsis)

        if (pelicula.reparto.isNotEmpty()) {
            recyclerReparto.adapter = RepartoAdapter(pelicula.reparto)
            repartoTitulo.visibility = View.VISIBLE
            recyclerReparto.visibility = View.VISIBLE
        } else {
            repartoTitulo.visibility = View.GONE
            recyclerReparto.visibility = View.GONE
        }
    }
}
