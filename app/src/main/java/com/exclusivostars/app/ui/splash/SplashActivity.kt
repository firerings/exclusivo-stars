package com.exclusivostars.app.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.exclusivostars.app.MediaflixApp
import com.exclusivostars.app.R
import com.exclusivostars.app.ui.auth.MainActivity
import com.exclusivostars.app.ui.home.HomeActivity

class SplashActivity : Activity() {

    companion object {
        private const val PREFS = "exclusivo_stars_prefs"
        private const val KEY_SPLASH_COMPLETO_VISTO = "splash_completo_visto"

        private const val DURACION_PRIMERA_VEZ_MS = 3000L
        private const val DURACION_SIGUIENTES_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val brand = findViewById<TextView>(R.id.splash_brand)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val esPrimeraVez = !prefs.getBoolean(KEY_SPLASH_COMPLETO_VISTO, false)

        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        brand.alpha = 0f

        if (esPrimeraVez) {
            // Primera apertura de la app en la vida de la instalación:
            // show completo de logo + marca, ~3s antes de pasar al login.
            logo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(1.6f))
                .start()
            brand.animate()
                .alpha(1f)
                .setStartDelay(250)
                .setDuration(450)
                .start()

            Handler(Looper.getMainLooper()).postDelayed({
                prefs.edit().putBoolean(KEY_SPLASH_COMPLETO_VISTO, true).apply()
                irALogin()
            }, DURACION_PRIMERA_VEZ_MS)
        } else {
            // Ya se vio el splash completo alguna vez: versión corta,
            // solo un fade rápido del ícono, sin retener al usuario.
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start()
            brand.animate().alpha(1f).setDuration(300).start()

            Handler(Looper.getMainLooper()).postDelayed({ irALogin() }, DURACION_SIGUIENTES_MS)
        }
    }

    private fun irALogin() {
        // Antes esto SIEMPRE mandaba a MainActivity, sin importar que
        // PersistentCookieStore ya tuviera guardada una sesión de 30 días
        // válida (checkbox "recordarme") — la cookie sobrevivía perfecto
        // en disco, pero la app nunca la miraba antes de pedir login de
        // nuevo. Ahora, si hay una cookie "session" sin vencer, se salta
        // directo a HomeActivity.
        val destino = if (haySesionValida()) HomeActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, destino))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    /** true si hay una cookie de sesión de Flask ("session", ver
     * SESSION_COOKIE_NAME default en config.py) guardada y no vencida.
     * No confirma con el servidor que el user_id todavía sea válido
     * (cuenta borrada, etc.) — si esa cookie resulta inválida, HomeActivity
     * va a recibir 401 en el primer request y ahí se puede resolver
     * mandando de vuelta a MainActivity (pendiente, no es este bug). */
    private fun haySesionValida(): Boolean =
        MediaflixApp.cookieManager.cookieStore.cookies.any { it.name == "session" && !it.hasExpired() }
}
