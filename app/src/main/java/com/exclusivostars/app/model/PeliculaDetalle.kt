package com.exclusivostars.app.model

import org.json.JSONObject

/**
 * Actor de la ficha, con la foto que ya cruza el backend contra
 * personas/ (ver _reparto_con_fotos en blueprints/peliculas.py).
 * fotoUrl null si el actor no tiene perfil/foto todavía — la UI cae a
 * un placeholder con la inicial, mismo criterio que pelicula.html.
 */
data class Actor(
    val nombre: String,
    val slug: String?,
    val fotoUrl: String?,
) : java.io.Serializable {
    companion object {
        fun fromJson(json: JSONObject): Actor = Actor(
            nombre = json.optString("nombre", ""),
            slug = json.optString("slug", null),
            fotoUrl = json.optString("foto_url", null),
        )
    }
}

/** Un subtítulo .vtt disponible para la película. */
data class Subtitulo(
    val lang: String?,
    val label: String,
    val url: String,
) : java.io.Serializable {
    companion object {
        fun fromJson(json: JSONObject): Subtitulo = Subtitulo(
            lang = json.optString("lang", null),
            label = json.optString("label", ""),
            url = json.getString("url"),
        )
    }
}

/**
 * Mismos nombres de campo que /api/pelicula/<nombre> ya arma en el
 * backend (api_pelicula en blueprints/peliculas.py) — igual criterio
 * que Pelicula.kt: no inventar nomenclatura nueva del lado de la app.
 *
 * modoDirecto + sourceUrl son lo que necesita PlayerActivity para
 * elegir cómo reproducir (HLS vs archivo directo) sin tener que
 * volver a decidirlo del lado de la app — esa decisión ya la tomó el
 * backend (ver _datos_reproduccion_pelicula).
 */
data class PeliculaDetalle(
    val nombre: String,
    val titulo: String,
    val sinopsis: String?,
    val anio: String?,
    val duracion: String?,
    val genero: List<String>,
    val director: String?,
    val puntuacion: Double?,
    val fondoUrl: String?,
    val tarjetaUrl: String?,
    val posterUrl: String?,
    val reparto: List<Actor>,
    /** Descripción de la pista de audio embebida, solo relevante en modo directo (ver aviso). */
    val audioTexto: String?,
    val modoDirecto: Boolean,
    val sourceUrl: String,
    val subtitulos: List<Subtitulo>,
) : java.io.Serializable {
    companion object {
        fun fromJson(json: JSONObject): PeliculaDetalle {
            val generoArr = json.optJSONArray("genero")
            val genero = buildList {
                if (generoArr != null) for (i in 0 until generoArr.length()) add(generoArr.getString(i))
            }
            val repartoArr = json.optJSONArray("reparto")
            val reparto = buildList {
                if (repartoArr != null) for (i in 0 until repartoArr.length()) add(Actor.fromJson(repartoArr.getJSONObject(i)))
            }
            val subsArr = json.optJSONArray("subtitulos")
            val subtitulos = buildList {
                if (subsArr != null) for (i in 0 until subsArr.length()) add(Subtitulo.fromJson(subsArr.getJSONObject(i)))
            }
            val puntuacionRaw = json.opt("puntuacion")
            return PeliculaDetalle(
                nombre = json.getString("nombre"),
                titulo = json.optString("titulo", json.getString("nombre")),
                sinopsis = json.optString("sinopsis", null),
                anio = json.optString("anio", null),
                duracion = json.optString("duracion", null),
                genero = genero,
                director = json.optString("director", null),
                puntuacion = (puntuacionRaw as? Number)?.toDouble(),
                fondoUrl = json.optString("fondo_url", null),
                tarjetaUrl = json.optString("tarjeta_url", null),
                posterUrl = json.optString("poster_url", null),
                reparto = reparto,
                audioTexto = json.optString("audio_texto", null),
                modoDirecto = json.optBoolean("modo_directo", false),
                sourceUrl = json.getString("source_url"),
                subtitulos = subtitulos,
            )
        }
    }
}
