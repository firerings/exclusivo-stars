package com.exclusivostars.app.network

import com.exclusivostars.app.model.Serie

/**
 * Contrato esperado del backend (api_series() en blueprints/series.py,
 * mismo patrón que api_peliculas()):
 *
 * GET /api/series
 *   200: {"ok": true, "series": [
 *          {
 *            "slug": str,
 *            "titulo": str,
 *            "sinopsis": str | null,
 *            "anio": str | null,
 *            "poster_url": str | null,   // URL absoluta vía series.servir_media
 *            "n_episodios": int
 *          }, ...
 *        ]}
 *
 * Se ordenan por título igual que series.lista() ya hace — no hace
 * falta que la app las reordene.
 */
object SeriesApi {

    sealed class Result {
        data class Ok(val series: List<Serie>) : Result()
        data class Error(val mensaje: String) : Result()
    }

    /** Llamar siempre desde background (Thread/executor), nunca desde el hilo principal. */
    fun listar(): Result {
        val response = ApiClient.getJson("/api/series")

        response.networkError?.let { return Result.Error(it) }
        val json = response.body ?: return Result.Error("El servidor respondió vacío (código ${response.code}).")

        if (!json.optBoolean("ok", false)) {
            return Result.Error(json.optString("error", "No se pudieron cargar las series."))
        }

        val arr = json.optJSONArray("series") ?: return Result.Ok(emptyList())
        val series = buildList {
            for (i in 0 until arr.length()) {
                add(Serie.fromJson(arr.getJSONObject(i)))
            }
        }
        return Result.Ok(series)
    }
}
