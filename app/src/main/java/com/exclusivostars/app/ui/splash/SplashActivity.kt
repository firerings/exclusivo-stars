package com.exclusivostars.app.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.exclusivostars.app.R
import com.exclusivostars.app.ui.auth.MainActivity

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
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
