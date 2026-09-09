package com.exclusivostars.app.ui.crash

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView

/**
 * Pantalla de diagnóstico temporal (ver MediaflixApp.instalarManejadorDeCrashes).
 * A propósito no tiene layout XML propio ni depende de ninguna vista de la
 * Activity que crasheó — arma todo por código así funciona sin importar en
 * qué estado haya quedado el resto de la app. Sacarla una vez que ya no
 * haga falta cazar crashes a ciegas sin PC/logcat.
 */
class CrashActivity : Activity() {

    companion object {
        const val EXTRA_TRACE = "trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "(sin stacktrace)"

        val texto = TextView(this).apply {
            text = "La app crasheó. Copiá este texto y pasáselo a Claude:\n\n$trace"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 13f
            setPadding(32, 64, 32, 64)
            setTextIsSelectable(true)
            gravity = Gravity.START
            autoLinkMask = 0
            Linkify.addLinks(this, Linkify.ALL)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(texto)
        }

        setContentView(scroll)
    }
}
