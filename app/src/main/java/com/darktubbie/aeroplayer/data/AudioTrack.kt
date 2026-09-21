package com.darktubbie.aeroplayer.data

/**
 * Modelo de una pista de audio detectada mediante MediaStore.
 *
 * Extraído de MainActivity sin cambiar sus campos (Fase 2 del plan).
 *
 * [dateModifiedMs] se añadió en la Fase 1 (0.4.x) para soportar el
 * ordenamiento "Más recientes primero": es la fecha de modificación
 * del archivo en almacenamiento (MediaStore.DATE_MODIFIED, en
 * milisegundos), no existe una noción de "fecha en que se agregó a
 * la biblioteca de Aero Player" independiente de eso. Con valor
 * por defecto 0L para no romper la deserialización de bibliotecas
 * guardadas por versiones anteriores que no tenían este campo.
 */
data class AudioTrack(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtPath: String?,
    val dateModifiedMs: Long = 0L
)
