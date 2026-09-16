package com.exclusivostars.app.util

/**
 * Arma el texto de la franja de datos del poster (país · año · tipo).
 *
 * El país se muestra tal cual llega del backend, en formato ISO 3166-1
 * alpha-3 ("USA", "DEU", "KOR"...) — se decidió alpha-3 en vez de
 * alpha-2 a propósito (más legible que "US"/"DE"/"KR", y sigue siendo
 * corto y fijo en 3 caracteres, así que nunca fuerza un wrap ni un
 * ellipsize en la franja, sea cual sea el país). No hay diccionario de
 * abreviaturas de este lado: el backend manda el código ya armado
 * (ver Pelicula.kt/Serie.kt — el campo "pais" sigue null hasta que se
 * agregue del lado del servidor).
 */
object Etiqueta {

    private fun paisCodigo(pais: String?): String? =
        pais?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()

    /** [tipo] siempre presente ("Película"/"Serie"); país y año solo si hay dato. */
    fun formatear(pais: String?, anio: String?, tipo: String): String =
        listOfNotNull(paisCodigo(pais), anio?.trim()?.takeIf { it.isNotEmpty() }, tipo)
            .joinToString(" · ")
}
