package com.darktubbie.aeroplayer.ui.library

/**
 * Criterios de ordenamiento de la pestaña Songs.
 *
 * Ampliado en la Fase 1 (0.4.x): antes solo existían TITLE/ARTIST.
 * DATE_NEWEST/DATE_OLDEST usan AudioTrack.dateModifiedMs (fecha de
 * modificación del archivo en almacenamiento — no existe una fecha
 * de "agregado a la biblioteca" independiente de eso, ver el
 * comentario en AudioTrack.kt).
 */
enum class SortOrder {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    DATE_NEWEST,
    DATE_OLDEST
}

/**
 * Etiqueta legible de cada [SortOrder] — compartida entre el menú
 * de Songs ([com.darktubbie.aeroplayer.ui.library.LibraryScreen]) y
 * el selector de "orden predeterminado" en Ajustes (Fase 5, 0.4.x).
 */
fun sortOrderLabel(
    sortOrder: SortOrder
): String {

    return when (sortOrder) {

        SortOrder.TITLE -> "Title"
        SortOrder.ARTIST -> "Artist"
        SortOrder.ALBUM -> "Album"
        SortOrder.DURATION -> "Duration"
        SortOrder.DATE_NEWEST -> "Newest first"
        SortOrder.DATE_OLDEST -> "Oldest first"
    }
}
