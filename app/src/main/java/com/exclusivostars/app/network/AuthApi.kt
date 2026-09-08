package com.exclusivostars.app.network

import org.json.JSONObject

/**
 * Ver contrato esperado del backend (endpoints nuevos en auth/routes.py,
 * @csrf.exempt igual que "reindexar") documentado en ApiClient.kt y en
 * la conversación de diseño: POST /api/login, POST /api/register.
 */
object AuthApi {

    data class Result(val ok: Boolean, val error: String?, val userName: String?)

    fun login(email: String, password: String, remember: Boolean): Result =
        toResult(ApiClient.postJson("/api/login", JSONObject().apply {
            put("email", email)
            put("password", password)
            put("remember", remember)
        }))

    fun register(name: String, email: String, password: String, confirm: String): Result =
        toResult(ApiClient.postJson("/api/register", JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
            put("confirm", confirm)
        }))

    private fun toResult(response: ApiClient.JsonResponse): Result {
        if (response.networkError != null) return Result(false, response.networkError, null)
        val json = response.body ?: return Result(false, "El servidor respondió vacío (código ${response.code}).", null)
        return Result(
            ok = json.optBoolean("ok", false),
            error = json.optString("error", null),
            userName = json.optJSONObject("user")?.optString("name"),
        )
    }
}
