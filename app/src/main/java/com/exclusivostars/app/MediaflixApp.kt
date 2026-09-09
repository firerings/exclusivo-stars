package com.exclusivostars.app

import android.app.Application
import android.content.Intent
import com.exclusivostars.app.ui.crash.CrashActivity
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

/**
 * Inicializa acá (no en MainActivity) el CookieManager global que
 * guarda la cookie de sesión de Flask y que ApiClient, Glide y Media3
 * (ver PlayerActivity) necesitan para mandar requests autenticados.
 *
 * Application.onCreate() corre SIEMPRE, una sola vez por proceso, sin
 * importar con qué Activity decida Android arrancar. MainActivity.onCreate()
 * NO tiene esa garantía: si el proceso muere en segundo plano (común
 * en equipos con poca RAM) y volvés a la app por "recientes" en vez
 * del ícono, Android recrea directamente la Activity que estaba en
 * pantalla (HomeActivity, la ficha de una película, etc.) sin volver
 * a pasar por MainActivity — dejando CookieHandler.getDefault() en
 * null y todas las rutas de la API responderían 401 "no autenticado"
 * aunque el login siga siendo válido en el servidor.
 *
 * La cookie además se guarda en disco (PersistentCookieStore), no
 * solo en memoria — así la sesión sobrevive a cerrar la app del todo,
 * no solo a que Android recicle el proceso en segundo plano.
 */
class MediaflixApp : Application() {

    companion object {
        lateinit var cookieManager: CookieManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        cookieManager = CookieManager(PersistentCookieStore(this), CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)
        instalarManejadorDeCrashes()
    }

    /**
     * Diagnóstico temporal: sin esto, un crash en un momento en que la
     * Activity todavía no terminó de armar sus vistas (ej. PlayerActivity
     * "vuelve a películas" sin ningún cartel) se pierde del todo — el
     * try/catch de una función puntual no alcanza a cubrir eso, y sin PC
     * no hay forma de leer logcat. Este handler es GLOBAL: agarra
     * cualquier excepción no capturada en cualquier hilo/Activity de toda
     * la app y abre una pantalla mostrando el stacktrace completo, antes
     * de que el proceso muera.
     */
    private fun instalarManejadorDeCrashes() {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_TRACE, throwable.stackTraceToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Si ni siquiera esto anduvo, que siga el manejador
                // original (el diálogo default de Android) en vez de
                // dejar el proceso en un estado raro sin ningún aviso.
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            anterior?.uncaughtException(thread, throwable)
        }
    }
}
