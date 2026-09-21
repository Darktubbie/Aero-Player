package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.runtime.compositionLocalOf

/**
 * Intensidad de efectos ambientales elegida en Ajustes (Fase 5,
 * 0.4.x), expuesta como CompositionLocal por la misma razón que
 * [LocalAmbientPlayback]: [MidgroundLayer] se usa desde [AeroBackground][
 * com.darktubbie.aeroplayer.ui.components.AeroBackground], que a su
 * vez se usa en todas las pantallas — pasarle la preferencia como
 * parámetro habría significado tocar la firma de cada pantalla solo
 * para reenviarla. Se provee una sola vez en MainActivity.
 *
 * Default [AmbientIntensity.NORMAL]: mismo valor que ya se usaba
 * como hardcodeado antes de esta fase, para que una preferencia
 * nunca leída (primera instalación) se comporte igual que antes.
 */
val LocalAmbientIntensity =
    compositionLocalOf { AmbientIntensity.NORMAL }
