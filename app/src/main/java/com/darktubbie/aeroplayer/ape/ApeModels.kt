package com.darktubbie.aeroplayer.ape

/**
 * Un evento dentro de un `.ape` (Fase 7 del plan de evolución
 * visual): qué efecto mostrar, en qué momento de la canción
 * (milisegundos) y durante cuánto tiempo.
 *
 * [effect] es un nombre de texto libre en el archivo (para que el
 * formato no dependa de los nombres internos de
 * `AmbientEventType`); la conversión de texto a tipo de efecto vive
 * en `ForegroundLayer`.
 */
data class ApeEvent(
    val time: Long,
    val effect: String,
    val duration: Long
)

/**
 * Un archivo `.ape` ya parseado.
 *
 * [version] se guarda tal cual del JSON aunque hoy no se use para
 * nada: es lo que permitirá en el futuro cambiar el formato sin
 * romper archivos `.ape` ya creados por el usuario (ver el
 * comentario de arquitectura en [ApeRepository] sobre
 * extensibilidad futura).
 */
data class ApeFile(
    val version: Int,
    val effects: List<ApeEvent>
)
