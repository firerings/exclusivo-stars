package com.exclusivostars.app.network

import com.exclusivostars.app.model.Pelicula

/**
 * Contrato esperado del backend (ruta nueva en blueprints/peliculas.py,
 * junto a lista()/ver_pelicula()/etc. — no hace falta @csrf.exempt
 * porque es un GET, CSRFProtect de Flask-WTF solo protege POST/PUT/
 * DELETE/PATCH por default):
 *
 * GET /api/peliculas
 *   200: {"ok": true, "peliculas": [
 *          {
 *            "nombre": str,          // carpeta en movies/, identificador único
 *            "titulo": str,
 *            "sinopsis": str | null,
 *            "anio": str | null,
 *            "duracion": str | null,
 *            "poster_url": str | null,   // URL absoluta servida vía peliculas.servir_video,
 *                                         // mismo criterio que poster_url en
 *                                         // _datos_reproduccion_pelicula (poster.jpg o
 *                                         // poster-thumb.jpg, lo que exista; null si no hay)
 *            "estado": "procesada" | "pendiente"
 *          }, ...
 *        ]}
 *   401: si antes de esto no llamaste a /api/login con éxito, antes de
 *        armar la ruta hay que decidir si /api/peliculas devuelve JSON
 *        401 en vez de redirigir a /login como hace hoy
 *        cargar_usuario_y_exigir_login con el resto del sitio — un
 *        redirect HTML rompería el parseo JSON de la app. Agregar
 *        "peliculas.api_peliculas" a una lista de "rutas API" que
 *        devuelvan 401 JSON en vez de redirect, o chequear g.user
 *        manualmente al principio de la vista.
 *
 * Se ordenan por nombre igual que listar_peliculas() ya hace — no hace
 * falta que la app las reordene.
 */
object PeliculasApi {

    sealed class Result {
        data class Ok(val peliculas: List<Pelicula>) : Result()
        data class Error(val mensaje: String) : Result()
    }

    /** Llamar siempre desde background (Thread/executor), nunca desde el hilo principal. */
    fun listar(): Result {
        val response = ApiClient.getJson("/api/peliculas")

        response.networkError?.let { return Result.Error(it) }
        val json = response.body ?: return Result.Error("El servidor respondió vacío (código ${response.code}).")

        if (!json.optBoolean("ok", false)) {
            return Result.Error(json.optString("error", "No se pudieron cargar las películas."))
        }

        val arr = json.optJSONArray("peliculas") ?: return Result.Ok(emptyList())
        val peliculas = buildList {
            for (i in 0 until arr.length()) {
                add(Pelicula.fromJson(arr.getJSONObject(i)))
            }
        }
        return Result.Ok(peliculas)
    }
}
