package com.exclusivostars.app.network

import com.exclusivostars.app.model.PeliculaDetalle
import java.net.URLEncoder

/**
 * Contrato esperado del backend (blueprints/peliculas.py, api_pelicula):
 *
 * GET /api/pelicula/<nombre>
 *   200: {"ok": true, "pelicula": { ...ver PeliculaDetalle... }}
 *   404: {"ok": false, "error": str}   // la carpeta no existe en el índice
 *   401: si no hay sesión (mismo criterio que /api/peliculas)
 *
 * Mismo patrón que PeliculasApi: un objeto por endpoint, sin librería
 * HTTP externa (ApiClient ya resuelve conexión/timeouts/parseo).
 */
object PeliculaApi {

    sealed class Result {
        data class Ok(val pelicula: PeliculaDetalle) : Result()
        data class Error(val mensaje: String) : Result()
    }

    /** Llamar siempre desde background (Thread/executor), nunca desde el hilo principal. */
    fun obtener(nombre: String): Result {
        // nombre es el nombre de carpeta en movies/ (puede traer espacios,
        // acentos, paréntesis del año, etc.) — hay que codificarlo para el
        // path del request, no solo interpolarlo tal cual.
        //
        // OJO: URLEncoder.encode() está pensado para query strings
        // (application/x-www-form-urlencoded), donde el espacio se
        // codifica como "+". En un segmento de PATH ese "+" es literal:
        // Werkzeug solo decodifica "+" como espacio en el query string,
        // nunca en el path. Sin este reemplazo, "Prey (2022)" llega al
        // backend como "Prey+(2022)", no matchea ninguna carpeta del
        // índice y responde 404. Por eso el "+" hay que pasarlo a "%20"
        // a mano después de codificar.
        val nombreCodificado = URLEncoder.encode(nombre, "UTF-8").replace("+", "%20")
        val response = ApiClient.getJson("/api/pelicula/$nombreCodificado")

        response.networkError?.let { return Result.Error(it) }
        val json = response.body ?: return Result.Error("El servidor respondió vacío (código ${response.code}).")

        if (!json.optBoolean("ok", false)) {
            return Result.Error(json.optString("error", "No se pudo cargar la película."))
        }

        val peliculaJson = json.optJSONObject("pelicula")
            ?: return Result.Error("Respuesta incompleta del servidor.")

        return Result.Ok(PeliculaDetalle.fromJson(peliculaJson))
    }
}
