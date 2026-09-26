package com.darktubbie.aeroplayer.ui.effects

/**
 * Dirección de movimiento de un evento ambiental de
 * [ForegroundLayer] (Fase 6 del plan de evolución visual).
 *
 * No todos los tipos de evento permiten las 3 direcciones — ver
 * [AmbientEventType] y la lógica de generación en
 * [ForegroundLayer].
 *
 * [TOP_TO_BOTTOM] se agregó en la Fase 6 de ".aero" (0.5.0) junto
 * con [AmbientEventType.LEAF], el primer tipo que cae en vez de
 * subir o cruzar horizontalmente.
 */
enum class AmbientDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    BOTTOM_TO_TOP,
    TOP_TO_BOTTOM
}
