package com.exclusivostars.app

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente de autenticación contra endpoints JSON dedicados para la app
 * (NO los mismos /login y /register que renderizan HTML para el navegador).
 *
 * Por qué JSON y no form-urlencoded contra las rutas web:
 * - Nada de leer el HTML para sacar el csrf_token ni de parsear el
 *   error-box con regex — eso se rompe apenas cambie una clase CSS o
 *   el texto de un mensaje en el template.
 * - El backend puede cambiar de diseño (auth.html) sin romper la app,
 *   porque la app no depende de esa página en absoluto.
 * - La sesión sigue siendo la misma cookie de Flask de siempre —
 *   CookieManager (ver MainActivity) ya la guarda y reenvía sola.
 *
 * Contrato esperado del backend (agregar en auth/routes.py, junto a
 * login/register, ambas @csrf.exempt igual que "reindexar" — no son
 * formularios de navegador, así que no aplica la protección CSRF
 * pensada para ese caso):
 *
 * POST /api/login
 *   body:  {"email": str, "password": str, "remember": bool}
 *   200:   {"ok": true, "user": {"id": int, "name": str, "email": str}}
 *   401:   {"ok": false, "error": str}   -> credenciales inválidas
 *   429:   {"ok": false, "error": str}   -> rate limit (5/min, igual que hoy)
 *
 * POST /api/register
 *   body:  {"name": str, "email": str, "password": str, "confirm": str}
 *   200:   {"ok": true, "user": {"id": int, "name": str, "email": str}}
 *   400:   {"ok": false, "error": str}   -> validación (nombre, email,
 *          longitud de contraseña, contraseñas no coinciden, email
 *          duplicado)
 *   429:   {"ok": false, "error": str}
 *
 * Ambas, si "ok": true, deben dejar la sesión iniciada (mismo
 * session["user_id"] = user.id que ya hacen login()/register() hoy)
 * para que el resto de la app (endpoints protegidos por
 * cargar_usuario_y_exigir_login) funcione con la cookie que devuelva
 * esta respuesta.
 */
object AuthApi {

    data class Result(val ok: Boolean, val error: String?, val userName: String?)

    fun login(email: String, password: String, remember: Boolean): Result =
        post("/api/login", JSONObject().apply {
            put("email", email)
            put("password", password)
            put("remember", remember)
        })

    fun register(name: String, email: String, password: String, confirm: String): Result =
        post("/api/register", JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
            put("confirm", confirm)
        })

    /** Corre en background (I/O de red). */
    private fun post(path: String, payload: JSONObject): Result {
        return try {
            val conn = URL(Config.BASE_URL + path).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
            conn.disconnect()

            if (body.isEmpty()) {
                return Result(false, "El servidor respondió vacío (código $code).", null)
            }

            val json = JSONObject(body)
            val ok = json.optBoolean("ok", false)
            val error = json.optString("error", null)
            val userName = json.optJSONObject("user")?.optString("name")
            Result(ok, error, userName)
        } catch (e: Exception) {
            Result(false, "No se pudo conectar con ${Config.BASE_URL}. ¿Está corriendo el servidor?", null)
        }
    }
}
