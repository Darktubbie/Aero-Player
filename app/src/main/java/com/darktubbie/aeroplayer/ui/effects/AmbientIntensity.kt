package com.darktubbie.aeroplayer.ui.effects

/**
 * Niveles de intensidad para las animaciones ambientales de fondo.
 *
 * Elegible desde Ajustes (Fase 5, 0.4.x) — ver
 * [com.darktubbie.aeroplayer.ui.more.SettingsScreen] y
 * [com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity], que
 * es cómo el valor elegido llega hasta [MidgroundLayer] sin pasar
 * por parámetro en cada pantalla.
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
