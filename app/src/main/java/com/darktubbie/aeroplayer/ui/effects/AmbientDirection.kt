package com.darktubbie.aeroplayer.ui.effects

/**
 * Dirección de movimiento de un evento ambiental de
 * [ForegroundLayer] (Fase 6 del plan de evolución visual).
 *
 * No todos los tipos de evento permiten las 3 direcciones — ver
 * [AmbientEventType] y la lógica de generación en
 * [ForegroundLayer].
 */
enum class AmbientDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    BOTTOM_TO_TOP
}
