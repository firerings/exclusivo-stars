package com.exclusivostars.app.ui.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.exclusivostars.app.R
import com.exclusivostars.app.model.PeliculaDetalle

/**
 * Reproductor standalone (pantalla completa, orientación horizontal
 * forzada). Recibe la ficha entera vía Serializable en vez de solo la
 * URL: modoDirecto/sourceUrl/subtitulos ya vienen decididos por el
 * backend (ver _datos_reproduccion_pelicula) — la app no tiene que
 * volver a inferir nada.
 *
 * Único punto de entrada: "expandir" desde el reproductor inline de
 * PeliculaDetalleActivity. Ya NO crea su propio ExoPlayer ni hace
 * prepare/seek — toma el reproductor compartido de [PlayerManager],
 * que es literalmente el mismo que ya venía sonando en el inline, así
 * que no hay demora de buffer al entrar ni pérdida de posición al
 * salir (ver PlayerManager para el porqué).
 *
 * `crop` (barras negras por lado) se corrige igual que en el
 * reproductor inline — ver CropCorrector, misma fórmula que
 * player.js en la web.
 *
 * Cookie de sesión: NO se pasa a mano. El CookieHandler global (seteado
 * en MediaflixApp.onCreate) ya la agrega solo a cualquier
 * HttpURLConnection del proceso — así es como cargan bien fondo.jpg/
 * tarjeta.jpg vía Glide, sin código especial. Media3 usa ese mismo
 * HttpURLConnection por debajo, así que también le llega solo.
 */
class PlayerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PELICULA = "pelicula"
        const val EXTRA_POSICION_MS = "posicion_ms"

        /**
         * Devuelve el Intent para lanzar con
         * registerForActivityResult (en vez de startActivity): el
         * caller necesita el resultCode/posición de vuelta para
         * reanudar el inline en el punto exacto donde quedó, incluso
         * en el caso borde de que el reproductor compartido se haya
         * liberado (fin de la película, error) mientras estaba en
         * fullscreen.
         */
        fun crearIntent(context: Context, pelicula: PeliculaDetalle): Intent =
            Intent(context, PlayerActivity::class.java).putExtra(EXTRA_PELICULA, pelicula)
    }

    private lateinit var playerView: PlayerView
    private lateinit var progress: android.widget.ProgressBar
    private lateinit var errorMessage: TextView
    private var pelicula: PeliculaDetalle? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            progress.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
        }

        override fun onPlayerError(error: PlaybackException) {
            mostrarError("ExoPlayer: ${error.errorCodeName} — ${error.localizedMessage}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El landscape forzado ahora se declara en el Manifest
        // (android:screenOrientation="sensorLandscape") en vez de acá
        // en código: así el sistema arranca la animación de rotación
        // en cuanto crea el ActivityRecord, en paralelo con el resto
        // del arranque de la Activity, en vez de esperar a que
        // corra esta línea dentro de onCreate() -- unos ms menos de
        // espera antes de ver el video.
        //
        // Sin LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, Android reserva una
        // franja para el recorte de la cámara (notch/punch-hole) en
        // landscape y corre todo el
        // contenido hacia el otro lado -- de ahí el video desplazado
        // con una franja vacía. LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        // permite dibujar debajo del cutout siempre, así el video usa
        // el 100% del ancho y la cámara queda flotando sobre la imagen.
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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
        adjuntarPlayer()
    }

    override fun onStop() {
        super.onStop()
        val posicion = PlayerManager.player?.currentPosition ?: 0L
        setResult(RESULT_OK, Intent().putExtra(EXTRA_POSICION_MS, posicion))
        if (isFinishing) {
            // Volviendo a la ficha (back / botón atrás): el mismo
            // reproductor sigue vivo, solo hay que soltar la vista —
            // PeliculaDetalleActivity lo vuelve a adjuntar al inline.
            // NO liberar acá, o se pierde el player que queremos
            // que siga reproduciéndose reutilizado.
            playerView.player?.removeListener(listener)
            playerView.player = null
            // Sin esto, Android anima la transición por default (fade
            // + un frame de la Activity de abajo quedando expuesta
            // antes de tiempo) -- se siente como demora extra aunque
            // el reproductor ya esté listo del otro lado.
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            // No se está cerrando (ej. Home): misma red de seguridad
            // de siempre, no dejar audio sonando de fondo sin UI.
            PlayerManager.liberar()
        }
    }

    private fun adjuntarPlayer() {
        try {
            @Suppress("DEPRECATION")
            val pelicula = intent.getSerializableExtra(EXTRA_PELICULA) as? PeliculaDetalle
                ?: return mostrarError(getString(R.string.error_cargar_pelicula))
            this.pelicula = pelicula

            val exoPlayer = PlayerManager.obtener(this, pelicula)
            playerView.player = exoPlayer
            // ZOOM en vez del FIT por default: FIT deja barras/huecos
            // cuando el AR crudo del video no coincide exacto con el
            // del contenedor (esto es aparte del crop de barras
            // horneadas -- pasa incluso sin ninguna barra en el
            // archivo). ZOOM siempre llena el contenedor recortando
            // el sobrante, igual criterio que object-fit:cover.
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            CropCorrector.instalar(playerView, pelicula.crop)

            exoPlayer.addListener(listener)
            // Sincronizar el estado de buffering visible ya-mismo, por
            // si venía buffereando desde el inline (el listener recién
            // se agrega ahora, se perdió el callback anterior).
            progress.visibility = if (exoPlayer.playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
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
}
