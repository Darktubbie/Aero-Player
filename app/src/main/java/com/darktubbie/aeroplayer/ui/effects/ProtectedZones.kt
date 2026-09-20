package com.darktubbie.aeroplayer.ui.effects

/**
 * "Protected zones" pedidas en la Fase 6 del plan de evolución
 * visual: franjas verticales (como fracción de la altura de
 * pantalla, 0f a 1f) donde los eventos ambientales ocasionales de
 * [ForegroundLayer] no deben aparecer, para no tapar botones,
 * títulos, controles o carátulas importantes.
 *
 * Se modela de forma simple como una franja reservada arriba
 * (encabezados/títulos de cada pantalla) y otra abajo (MiniPlayer +
 * BottomNavBar, que son globales y están siempre presentes). No es
 * un sistema de "hit testing" contra elementos reales de la UI —
 * eso sería mucho más complejo de lo que esta fase necesita — sino
 * una franja segura conservadora que ya evita las zonas donde
 * siempre hay controles importantes en Aero Player.
 *
 * Los valores por defecto son deliberadamente generosos (mejor
 * dejar más espacio libre de eventos que arriesgarse a tapar un
 * control).
 */
data class ProtectedZones(
    val topFraction: Float = 0.14f,
    val bottomFraction: Float = 0.22f
) {

    /**
     * Franja vertical permitida para eventos ambientales, como
     * fracción de la altura de pantalla (0f = arriba, 1f = abajo).
     */
    val allowedRange: ClosedFloatingPointRange<Float>
        get() = topFraction..(1f - bottomFraction)
}
