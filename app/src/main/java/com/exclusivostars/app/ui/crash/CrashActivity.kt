package com.exclusivostars.app.ui.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

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
        val mensaje = "La app crasheó. Mantené presionado el texto para copiarlo\ny pasáselo a Claude:\n\n$trace"

        val texto = TextView(this).apply {
            text = mensaje
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 13f
            setPadding(32, 64, 32, 64)
            gravity = Gravity.START
            // NO usar setTextIsSelectable(true) acá: tiene un bug conocido
            // de Android ("setSpan (-1...-1) starts before 0" en
            // Editor$SelectionStartHandleView) que crashea esta misma
            // pantalla de diagnóstico. En vez de seleccionar texto,
            // copiamos todo con un long-press.
            setOnLongClickListener {
                val portapapeles = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                portapapeles.setPrimaryClip(ClipData.newPlainText("crash", mensaje))
                Toast.makeText(this@CrashActivity, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(texto)
        }

        setContentView(scroll)
    }
}
