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
 *
 * Cookie de sesión: NO se pasa a mano. El CookieHandler global (seteado
 * en MediaflixApp.onCreate) ya la agrega solo a cualquier
 * HttpURLConnection del proceso — así es como cargan bien fondo.jpg/
 * tarjeta.jpg vía Glide, sin código especial. Media3 usa ese mismo
 * HttpURLConnection por debajo, así que también le llega solo.
 * Habíamos agregado un cookieHeadersPara() manual pensando que ese
 * mecanismo automático estaba roto para esta URL — no lo estaba: el
 * agregado manual quedaba SUMADO al automático, mandando la cookie
 * duplicada y separada por coma en vez de "; " (confirmado con un log
 * temporal del lado server: "session=X,session=X" en el header Cookie
 * crudo), lo cual rompía el parseo de Werkzeug y volvía "no
 * autenticado" — de ahí el 302 a /login. Sacar el código manual (no
 * agregar más) fue la solución.
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
        setContentView(R.layout.activity_player)
        // window.insetsController (dentro de ocultarBarrasSistema) exige
        // que la ventana ya tenga su DecorView armada -- eso recién pasa
        // después de setContentView(), nunca antes. Llamarla antes tira
        // NullPointerException en PhoneWindow.getInsetsController() y
        // crashea la Activity en cada intento de reproducir, sin ningún
        // request llegar al backend (por eso el server no mostraba nada).
        ocultarBarrasSistema()

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
        try {
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
                    mostrarError("ExoPlayer: ${error.errorCodeName} — ${error.localizedMessage}")
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
        } catch (e: Exception) {
            // Antes esto tiraba la Activity entera sin avisar nada (por
            // eso "volvía a películas" sin mensaje). Mostrarlo acá saca a
            // ciegas el diagnóstico real la próxima vez que se reproduzca.
            mostrarError("CRASH: ${e::class.simpleName} — ${e.message}")
        }
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
