package com.exclusivostars.app

import android.app.Application
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
 * null y todo /api/* respondiendo 401 "no autenticado" aunque el
 * login siga siendo válido en el servidor.
 */
class MediaflixApp : Application() {

    companion object {
        lateinit var cookieManager: CookieManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)
    }
}
