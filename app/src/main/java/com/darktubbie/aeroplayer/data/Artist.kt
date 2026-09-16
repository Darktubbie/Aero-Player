package com.darktubbie.aeroplayer.data

/**
 * Agrupación de [AudioTrack] por artista, calculada en memoria.
 * Igual que [Album]: no se persiste, se deriva de `tracks`.
 */
data class Artist(
    val name: String,
    val tracks: List<AudioTrack>,
    val albumCount: Int
)
