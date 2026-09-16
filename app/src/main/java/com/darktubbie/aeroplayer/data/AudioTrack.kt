package com.darktubbie.aeroplayer.data

/**
 * Modelo de una pista de audio detectada mediante MediaStore.
 *
 * Extraído de MainActivity sin cambiar sus campos (Fase 2 del plan).
 */
data class AudioTrack(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtPath: String?
)
