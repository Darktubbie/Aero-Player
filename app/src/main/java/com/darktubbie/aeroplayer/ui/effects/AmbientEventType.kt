package com.darktubbie.aeroplayer.ui.effects

/**
 * Tipos de eventos ambientales ocasionales de foreground (Fase 6
 * del plan de evolución visual).
 *
 * [BUBBLE_FRONT]: una burbuja como las de [MidgroundLayer], pero
 * que esta vez pasa por delante de la interfaz en vez de por
 * detrás. Puede moverse en cualquiera de las 3 [AmbientDirection].
 *
 * [FISH]: aparece en cardumen (varios a la vez, muy juntos) y
 * serpentea en vez de seguir una línea recta — solo se mueve en
 * horizontal ([AmbientDirection.LEFT_TO_RIGHT] o
 * [AmbientDirection.RIGHT_TO_LEFT]).
 *
 * [JELLYFISH]: siempre sube ([AmbientDirection.BOTTOM_TO_TOP]).
 *
 * [CLOUD]: siempre sube ([AmbientDirection.BOTTOM_TO_TOP]).
 */
enum class AmbientEventType {
    BUBBLE_FRONT,
    FISH,
    JELLYFISH,
    CLOUD
}
