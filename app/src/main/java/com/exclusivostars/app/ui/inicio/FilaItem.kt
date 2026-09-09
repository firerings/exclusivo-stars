package com.exclusivostars.app.ui.inicio

/**
 * Item genérico para una fila horizontal de Inicio (FilaAdapter). Tanto
 * PeliculasFragment como una futura fila de Reels/Música pueden mapear
 * su propio modelo a esto sin que el adapter de la fila tenga que
 * conocer Pelicula/Serie/Reel puntualmente — mismo espíritu que
 * ApiClient centralizando el HTTP: un solo lugar para "cómo se pinta
 * una tarjeta en una fila", sea cual sea el contenido real.
 */
data class FilaItem(
    val id: String,
    val titulo: String,
    val subtitulo: String?,
    val posterUrl: String?,
    val onClick: () -> Unit,
)
