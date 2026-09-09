package com.exclusivostars.app.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.exclusivostars.app.R
import com.exclusivostars.app.model.PeliculaDetalle

/**
 * Reproductor standalone (pantalla completa, orientación horizontal
 * forzada). Recibe la ficha entera vía Serializable en vez de solo la
 * URL: modoDirecto/sourceUrl/subtitulos ya vienen decididos por el
 * backend (ver _datos_reproduccion_pelicula) — la app no tiene que
 * volver a inferir nada, solo pasárselo a Media3.
 *
 * Media3 elige el tipo de fuente según la URL: master.m3u8 -> HLS (con
 * el módulo media3-exoplayer-hls agregado en build.gradle.kts),
 * cualquier otra extensión de video -> extractor progresivo estándar.
 * No hace falta que la app distinga los dos casos a mano.
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PELICULA = "pelicula"

        fun iniciar(context: Context, pelicula: PeliculaDetalle) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).putExtra(EXTRA_PELICULA, pelicula)
            )
        }
    }

    private lateinit var playerView: PlayerView
    private lateinit var progress: android.widget.ProgressBar
    private lateinit var errorMessage: TextView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ocultarBarrasSistema()
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        progress = findViewById(R.id.progress)
        errorMessage = findViewById(R.id.error_message)
    }

    private fun ocultarBarrasSistema() {
        // Reproducción a pantalla completa, igual criterio que el
        // reproductor embebido de la web (oculta chrome del navegador
        // al entrar en fullscreen vía player.js).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    override fun onStart() {
        super.onStart()
        inicializarPlayer()
    }

    override fun onStop() {
        super.onStop()
        liberarPlayer()
    }

    private fun inicializarPlayer() {
        @Suppress("DEPRECATION")
        val pelicula = intent.getSerializableExtra(EXTRA_PELICULA) as? PeliculaDetalle
            ?: return mostrarError(getString(R.string.error_cargar_pelicula))

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        playerView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                progress.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            }

            override fun onPlayerError(error: PlaybackException) {
                mostrarError(error.localizedMessage ?: getString(R.string.error_cargar_pelicula))
            }
        })

        val builder = MediaItem.Builder().setUri(Uri.parse(pelicula.sourceUrl))
        if (pelicula.subtitulos.isNotEmpty()) {
            val subs = pelicula.subtitulos.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.lang)
                    .setLabel(sub.label)
                    .build()
            }
            builder.setSubtitleConfigurations(subs)
        }

        exoPlayer.setMediaItem(builder.build())
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    private fun mostrarError(mensaje: String) {
        progress.visibility = View.GONE
        errorMessage.text = mensaje
        errorMessage.visibility = View.VISIBLE
    }

    private fun liberarPlayer() {
        player?.release()
        player = null
    }
}
