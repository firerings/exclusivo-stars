package com.exclusivostars.app.model

import org.json.JSONObject

/**
 * Mismos nombres de campo que indexador.py ya arma para cada entrada de
 * indice.json["peliculas"] — no invento nomenclatura nueva del lado de
 * la app, así el backend casi no tiene que "traducir" nada al armar el
 * endpoint, solo envolver lo que ya calcula en JSON.
 *
 * Todo opcional/tolerante a null a propósito: mismo criterio que ya usa
 * el propio backend (info.json puede no existir todavía, una carpeta
 * puede estar "pendiente" sin poster, etc.) — la UI cae a un estado
 * vacío/placeholder en vez de romper si al backend le falta un campo.
 */
data class Pelicula(
    val nombre: String,
    val titulo: String,
    val sinopsis: String?,
    val anio: String?,
    val duracion: String?,
    val posterUrl: String?,
    /** fondo.jpg > tarjeta.jpg > poster — mismo criterio que el hero de la web (_construir_hero). */
    val fondoUrl: String?,
    /** logo.png transparente, o null si la película no tiene — mismo criterio que tiene_logo en la web. */
    val logoUrl: String?,
    /** "procesada" (HLS listo) o "pendiente" (se reproduce directo mientras tanto). */
    val estado: String?,
) {
    companion object {
        fun fromJson(json: JSONObject): Pelicula = Pelicula(
            nombre = json.getString("nombre"),
            titulo = json.optString("titulo", json.getString("nombre")),
            sinopsis = json.optString("sinopsis", null),
            anio = json.optString("anio", null),
            duracion = json.optString("duracion", null),
            posterUrl = json.optString("poster_url", null),
            fondoUrl = json.optString("fondo_url", null),
            logoUrl = json.optString("logo_url", null),
            estado = json.optString("estado", null),
        )
    }
}
