package com.exclusivostars.app.ui.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.exclusivostars.app.model.PeliculaDetalle

/**
 * Reproductor compartido entre el inline de PeliculaDetalleActivity y
 * el fullscreen de PlayerActivity.
 *
 * Antes cada pantalla creaba su propio ExoPlayer: "expandir" liberaba
 * el de la ficha y preparaba uno nuevo en PlayerActivity (prepare +
 * buffer + seek, siempre con demora), y como PlayerActivity nunca le
 * devolvía la posición a la ficha, volver del fullscreen perdía el
 * progreso.
 *
 * Con una sola instancia real -- viva mientras el usuario esté en la
 * ficha de esa película, sin importar qué pantalla la esté mostrando
 * en cada momento -- "expandir" y "volver" pasan a ser solo mover la
 * misma instancia de un PlayerView a otro (adjuntar/desadjuntar), sin
 * prepare/seek/pérdida de posición: es el mismo player, nunca se
 * detuvo.
 *
 * No hace falta un Service para esto: las dos Activities viven en el
 * mismo proceso, así que un singleton in-process alcanza. Un Service
 * solo haría falta si quisiéramos que la reproducción sobreviva a que
 * la app entera pase a background (audio de fondo estilo Spotify), que
 * no es el caso acá -- de hecho justamente NO queremos eso (ver
 * liberar() desde onStop cuando se sale de verdad).
 */
object PlayerManager {

    var player: ExoPlayer? = null
        private set

    /** Nombre de la película del player actual, para no heredar el reproductor de una ficha distinta. */
    private var peliculaActual: String? = null

    /**
     * Devuelve el reproductor para [pelicula]: reutiliza el existente
     * si ya está armado para esta misma película, o crea uno nuevo
     * (liberando antes cualquier otro que hubiera quedado).
     */
    fun obtener(context: Context, pelicula: PeliculaDetalle): ExoPlayer {
        val existente = player
        if (existente != null && peliculaActual == pelicula.nombre) {
            return existente
        }
        liberar()
        val nuevo = ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(MediaItemFactory.build(pelicula))
            playWhenReady = true
            prepare()
        }
        player = nuevo
        peliculaActual = pelicula.nombre
        return nuevo
    }

    /** true si ya hay un reproductor listo para [pelicula] (para no re-preparar al volver del fullscreen). */
    fun tieneReproductorPara(pelicula: PeliculaDetalle): Boolean =
        player != null && peliculaActual == pelicula.nombre

    /** Libera el reproductor de verdad -- llamar solo cuando el usuario sale de la ficha, no al pasar entre inline/fullscreen. */
    fun liberar() {
        player?.release()
        player = null
        peliculaActual = null
    }
}
