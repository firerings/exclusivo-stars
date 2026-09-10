package com.exclusivostars.app.network

/**
 * Contrato esperado del backend (api_me() en auth/routes.py, mismo
 * patrón que api_login/api_register pero GET y sin body):
 *
 * GET /api/me
 *   200: {"ok": true, "user": {"name": str, "email": str}}
 *   401: si no hay sesión (before_app_request de auth/routes.py ya lo
 *        cubre para todo /api/, igual que en PeliculasApi/SeriesApi).
 *
 * Para qué sirve: la sesión vive en la cookie (PersistentCookieStore),
 * no en un objeto de usuario guardado del lado de la app — así que
 * HomeActivity necesita pedir este endpoint para saber a quién
 * pintarle el avatar/nombre en el header, sin repetir el login.
 */
object SessionApi {

    sealed class Result {
        data class Ok(val nombre: String, val email: String) : Result()
        data class Error(val mensaje: String) : Result()
    }

    /** Llamar siempre desde background (Thread/executor), nunca desde el hilo principal. */
    fun me(): Result {
        val response = ApiClient.getJson("/api/me")

        response.networkError?.let { return Result.Error(it) }
        val json = response.body ?: return Result.Error("El servidor respondió vacío (código ${response.code}).")

        if (!json.optBoolean("ok", false)) {
            return Result.Error(json.optString("error", "No se pudo cargar la cuenta."))
        }

        val user = json.optJSONObject("user") ?: return Result.Error("Respuesta incompleta del servidor.")
        return Result.Ok(
            nombre = user.optString("name", ""),
            email = user.optString("email", ""),
        )
    }
}
