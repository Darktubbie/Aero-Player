package com.darktubbie.aeroplayer.ui.layout

import androidx.compose.runtime.compositionLocalOf

/**
 * Modo de layout actual (Fase 1, 0.6.0), expuesto como
 * CompositionLocal por la misma razón que
 * [com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity]: las
 * Fases 2 y 3 van a necesitar leer esto desde pantallas que hoy no
 * reciben el parámetro, sin tener que reenviarlo a mano por cada
 * firma de por medio. Se provee una sola vez en MainActivity.
 *
 * Default [AeroLayoutMode.MOBILE]: si algo llegara a leer esto antes
 * de que MainActivity lo provea (no debería pasar), se comporta como
 * la interfaz actual, nunca como una pantalla Desktop a medio
 * construir.
 */
val LocalAeroLayoutMode =
    compositionLocalOf { AeroLayoutMode.MOBILE }
