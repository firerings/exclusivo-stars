package com.exclusivostars.app

import android.content.Context
import java.net.CookieManager
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI

/**
 * CookieStore que persiste a SharedPreferences además de guardar en
 * memoria. El CookieManager de la JDK (java.net.CookieManager) trae
 * un CookieStore en memoria que ya resuelve bien el matching por
 * dominio/path y el filtrado de cookies vencidas — envolvemos ese
 * (delegate) y solo agregamos el paso de guardar/restaurar en disco,
 * para no reimplementar esa lógica a mano.
 *
 * Sin esto la sesión vive nada más en RAM (ver MediaflixApp): cerrar
 * la app del todo, o que Android mate el proceso en segundo plano
 * (común en equipos con poca RAM), la borra sin avisar y hay que
 * loguearse de nuevo aunque la sesión siga siendo válida en el
 * servidor.
 */
class PersistentCookieStore(context: Context) : CookieStore {

    companion object {
        private const val PREFS = "mediaflix_cookies"
        private const val KEY_COOKIES = "cookies"
        private const val SEP = "\u0001" // separador de campos, no puede aparecer en nombre/valor de una cookie real
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val delegate: CookieStore = CookieManager().cookieStore

    init {
        prefs.getStringSet(KEY_COOKIES, null)?.forEach { serializado ->
            deserializar(serializado)?.let { delegate.add(null, it) }
        }
    }

    override fun add(uri: URI?, cookie: HttpCookie) {
        delegate.add(uri, cookie)
        persistir()
    }

    override fun get(uri: URI?): MutableList<HttpCookie> = delegate.get(uri)

    override fun getCookies(): MutableList<HttpCookie> = delegate.cookies

    override fun getURIs(): MutableList<URI> = delegate.uRIs

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        val huboRemocion = delegate.remove(uri, cookie)
        if (huboRemocion) persistir()
        return huboRemocion
    }

    override fun removeAll(): Boolean {
        val huboRemocion = delegate.removeAll()
        persistir()
        return huboRemocion
    }

    private fun persistir() {
        val serializadas = delegate.cookies.map(::serializar).toSet()
        prefs.edit().putStringSet(KEY_COOKIES, serializadas).apply()
    }

    private fun serializar(cookie: HttpCookie): String = listOf(
        cookie.name, cookie.value, cookie.domain ?: "", cookie.path ?: "", cookie.maxAge.toString()
    ).joinToString(SEP)

    private fun deserializar(serializado: String): HttpCookie? {
        val partes = serializado.split(SEP)
        if (partes.size != 5) return null
        return try {
            HttpCookie(partes[0], partes[1]).apply {
                domain = partes[2].ifEmpty { null }
                path = partes[3].ifEmpty { null }
                maxAge = partes[4].toLongOrNull() ?: -1
            }
        } catch (e: IllegalArgumentException) {
            null // nombre de cookie inválido u otro dato corrupto — se descarta esa cookie sola, no rompe el resto
        }
    }
}
