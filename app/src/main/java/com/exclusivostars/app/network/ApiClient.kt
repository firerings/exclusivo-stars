package com.exclusivostars.app.network

import com.exclusivostars.app.Config
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP mínimo, sin librerías externas (HttpURLConnection +
 * org.json, ambas del SDK). Cualquier *Api nueva (PeliculasApi,
 * SeriesApi, ReelsApi, MusicaApi...) debería apoyarse en esto en vez
 * de abrir su propia conexión a mano — así el manejo de timeouts,
 * errores de red y parseo de la respuesta vive en un solo lugar.
 *
 * La cookie de sesión (CookieManager, ver MainActivity) se adjunta
 * sola vía CookieHandler.setDefault, no hace falta nada acá para eso.
 */
object ApiClient {

    data class JsonResponse(val code: Int, val body: JSONObject?, val networkError: String?)

    /** Corre en el hilo que la llame — siempre desde background (Thread/executor), nunca desde el hilo principal. */
    fun postJson(path: String, payload: JSONObject): JsonResponse = request("POST", path, payload)

    fun getJson(path: String): JsonResponse = request("GET", path, null)

    private fun request(method: String, path: String, payload: JSONObject?): JsonResponse {
        return try {
            val conn = URL(Config.BASE_URL + path).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")

            if (payload != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                    it.write(payload.toString())
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
            conn.disconnect()

            JsonResponse(code, if (body.isEmpty()) null else JSONObject(body), null)
        } catch (e: Exception) {
            JsonResponse(-1, null, "No se pudo conectar con ${Config.BASE_URL}. ¿Está corriendo el servidor?")
        }
    }
}
