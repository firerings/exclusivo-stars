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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.exclusivostars.app.R
import com.exclusivostars.app.model.PeliculaDetalle
import com.exclusivostars.app.network.PeliculaApi
import com.exclusivostars.app.ui.player.CropCorrector
import com.exclusivostars.app.ui.player.MediaItemFactory
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
    private lateinit var heroPlayIcon: ImageView
    private lateinit var playerViewInline: PlayerView
    private lateinit var heroInlineProgress: ProgressBar
    private lateinit var heroFullscreenButton: ImageButton
    private var inlinePlayer: ExoPlayer? = null
    private lateinit var titulo: TextView
    private lateinit var meta: TextView
    private lateinit var director: TextView
    private lateinit var puntuacion: TextView
    private lateinit var avisoDirecto: TextView
    private lateinit var sinopsis: TextView
    private lateinit var repartoTitulo: TextView
    private lateinit var recyclerReparto: RecyclerView

    private lateinit var nombre: String
    private var peliculaActual: PeliculaDetalle? = null

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
        heroPlayIcon = findViewById(R.id.hero_play_icon)
        playerViewInline = findViewById(R.id.player_view_inline)
        heroInlineProgress = findViewById(R.id.hero_inline_progress)
        heroFullscreenButton = findViewById(R.id.hero_fullscreen_button)
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
        peliculaActual = pelicula
        heroTarjeta.setOnClickListener { iniciarInline(pelicula) }
        heroFullscreenButton.setOnClickListener { expandirAFullscreen() }

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

    /**
     * Reproducción inline: reemplaza la miniatura+play por el
     * reproductor, EN EL MISMO contenedor (hero_tarjeta) — mismo
     * criterio que el reproductor embebido de la web (_player.html),
     * en vez de navegar a PlayerActivity de entrada. La pantalla
     * completa queda como acción explícita (hero_fullscreen_button).
     */
    private fun iniciarInline(pelicula: PeliculaDetalle) {
        if (inlinePlayer != null) return // ya está reproduciendo/inicializado

        heroPlayIcon.visibility = View.GONE
        playerViewInline.visibility = View.VISIBLE
        heroFullscreenButton.visibility = View.VISIBLE
        heroInlineProgress.visibility = View.VISIBLE

        val exoPlayer = ExoPlayer.Builder(this).build()
        inlinePlayer = exoPlayer
        playerViewInline.player = exoPlayer
        CropCorrector.instalar(playerViewInline, pelicula.crop)

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                heroInlineProgress.visibility =
                    if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            }

            override fun onPlayerError(error: PlaybackException) {
                // Fallback: si el inline falla (ej. formato no soportado
                // por el resize del contenedor chico), vuelve a la
                // miniatura+play en vez de dejar la tarjeta rota —
                // el usuario puede reintentar o abrir en pantalla
                // completa directamente si prefiere.
                liberarInline()
            }
        })

        exoPlayer.setMediaItem(MediaItemFactory.build(pelicula))
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    /**
     * "Expandir": pasa lo que se está viendo inline a PlayerActivity
     * (pantalla completa horizontal) sin reiniciar el video — mismo
     * criterio que el botón de fullscreen del reproductor web, que no
     * reinicia el <video> al pedir el fullscreen del navegador.
     */
    private fun expandirAFullscreen() {
        val pelicula = peliculaActual ?: return
        val posicionMs = inlinePlayer?.currentPosition ?: 0L
        liberarInline()
        PlayerActivity.iniciar(this, pelicula, posicionMs)
    }

    private fun liberarInline() {
        inlinePlayer?.release()
        inlinePlayer = null
        playerViewInline.player = null
        playerViewInline.visibility = View.GONE
        heroInlineProgress.visibility = View.GONE
        heroFullscreenButton.visibility = View.GONE
        heroPlayIcon.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        // Red de seguridad: si el usuario sale de la pantalla (Home,
        // back, o al abrir PlayerActivity) sin pasar por
        // expandirAFullscreen(), igual hay que soltar el ExoPlayer acá
        // — si no, sigue reproduciendo audio de fondo sin UI visible.
        liberarInline()
    }
}
