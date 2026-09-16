package com.exclusivostars.app.util

/**
 * Arma el texto del chip integrado al poster (país · año · tipo) y
 * abrevia los países de nombre largo para que entre en un chip chico
 * sin romper el layout. Lista corta a propósito: se amplía a medida
 * que el backend empiece a mandar "pais" con valores reales (todavía
 * no lo hace — ver Pelicula.kt/Serie.kt, el campo es null hasta que
 * se agregue del lado del servidor).
 */
object Etiqueta {

    private val abreviaturas = mapOf(
        "Estados Unidos" to "USA",
        "Reino Unido" to "UK",
        "Corea del Sur" to "Corea",
    )

    private fun paisAbreviado(pais: String?): String? {
        val limpio = pais?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return abreviaturas[limpio] ?: limpio
    }

    /** [tipo] siempre presente ("Película"/"Serie"); país y año solo si hay dato. */
    fun formatear(pais: String?, anio: String?, tipo: String): String =
        listOfNotNull(paisAbreviado(pais), anio?.trim()?.takeIf { it.isNotEmpty() }, tipo)
            .joinToString(" · ")
}
