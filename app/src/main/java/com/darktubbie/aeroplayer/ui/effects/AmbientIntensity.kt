package com.darktubbie.aeroplayer.ui.effects

/**
 * Niveles de intensidad para las animaciones ambientales de fondo
 * (Fase 5 del plan de evolución visual).
 *
 * Todavía no existe una pantalla de Ajustes real donde el usuario
 * pueda elegir uno de estos valores (eso vive en la sección "Más",
 * marcada como "Próximamente" desde la Fase 1) — por ahora
 * [MidgroundLayer] usa [NORMAL] como valor por defecto, pero ya
 * queda preparado el parámetro para que una futura fase de Ajustes
 * solo tenga que pasar el valor elegido, sin tocar la lógica de
 * animación.
 *
 * [OFF] y [STATIC] son equivalentes en este momento (ninguno anima
 * nada): se mantienen como dos valores separados porque así los
 * pidió el brief original, pensando en una futura UI de Ajustes
 * donde puedan presentarse como dos opciones distintas.
 */
enum class AmbientIntensity {
    OFF,
    STATIC,
    LOW,
    NORMAL,
    HIGH
}
