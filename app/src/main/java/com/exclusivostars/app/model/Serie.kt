package com.exclusivostars.app.model

import org.json.JSONObject

/**
 * Mismo criterio que Pelicula.kt: nombres de campo calcados de lo que
 * arma indexador.py / api_series() — sin traducir nomenclatura del
 * lado de la app.
 *
 * Todavía no hay SerieDetalleActivity (la pestaña Series sigue en
 * "Próximamente" en HomeActivity), así que por ahora esto solo se usa
 * para pintar la fila de Series en Inicio — el click no navega a
 * ningún lado todavía.
 */
data class Serie(
    val slug: String,
    val titulo: String,
    val sinopsis: String?,
    val anio: String?,
    val posterUrl: String?,
    val nEpisodios: Int,
    /** País en ISO 3166-1 alpha-3, mismo criterio que Pelicula.pais: el backend todavía no lo manda en /api/series. */
    val pais: String? = null,
) {
    companion object {
        fun fromJson(json: JSONObject): Serie = Serie(
            slug = json.getString("slug"),
            titulo = json.optString("titulo", json.getString("slug")),
            sinopsis = json.optString("sinopsis", null),
            anio = json.optString("anio", null),
            posterUrl = json.optString("poster_url", null),
            nEpisodios = json.optInt("n_episodios", 0),
            pais = json.optString("pais", null),
        )
    }
}
