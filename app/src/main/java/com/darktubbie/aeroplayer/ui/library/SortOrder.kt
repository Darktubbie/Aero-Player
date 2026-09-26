package com.darktubbie.aeroplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.darktubbie.aeroplayer.R

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
 *
 * Fase 2 (0.5.0): antes devolvía los nombres hardcodeados en
 * inglés ("Title", "Artist"...) aunque el resto de la app estaba en
 * español — ahora sale de `strings.xml` como cualquier otro texto.
 */
@Composable
fun sortOrderLabel(
    sortOrder: SortOrder
): String {

    return when (sortOrder) {

        SortOrder.TITLE -> stringResource(R.string.sort_title)
        SortOrder.ARTIST -> stringResource(R.string.sort_artist)
        SortOrder.ALBUM -> stringResource(R.string.sort_album)
        SortOrder.DURATION -> stringResource(R.string.sort_duration)
        SortOrder.DATE_NEWEST -> stringResource(R.string.sort_newest_first)
        SortOrder.DATE_OLDEST -> stringResource(R.string.sort_oldest_first)
    }
}
