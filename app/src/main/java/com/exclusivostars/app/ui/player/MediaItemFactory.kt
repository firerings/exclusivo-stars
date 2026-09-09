package com.exclusivostars.app.ui.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.exclusivostars.app.model.PeliculaDetalle

/**
 * Arma el MediaItem (source + subtítulos) a partir de la ficha ya
 * decidida por el backend. Extraído de PlayerActivity para que el
 * reproductor inline de PeliculaDetalleActivity use exactamente la
 * misma lógica sin duplicarla.
 */
object MediaItemFactory {
    fun build(pelicula: PeliculaDetalle): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(pelicula.sourceUrl))
        if (pelicula.subtitulos.isNotEmpty()) {
            val subs = pelicula.subtitulos.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.lang)
                    .setLabel(sub.label)
                    .build()
            }
            builder.setSubtitleConfigurations(subs)
        }
        return builder.build()
    }
}
