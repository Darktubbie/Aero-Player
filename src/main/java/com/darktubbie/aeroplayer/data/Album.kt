package com.darktubbie.aeroplayer.data

/**
 * Agrupación de [AudioTrack] por álbum, calculada en memoria a
 * partir de la biblioteca ya escaneada.
 *
 * No se persiste — se recalcula (barato: un groupBy sobre una
 * lista que ya está en RAM) cada vez que cambia la biblioteca,
 * mediante `derivedStateOf` en MainViewModel, así que nunca se
 * desincroniza de `tracks`.
 */
data class Album(
    val name: String,
    val artist: String,
    val tracks: List<AudioTrack>
)
