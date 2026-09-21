package com.darktubbie.aeroplayer.ui.theme

/**
 * Tema visual de la app, elegible desde Ajustes (post-0.4.0).
 * [colorScheme] es el único punto que traduce esta preferencia al
 * [AeroColorScheme] real que consume [AeroColors] a través de
 * [LocalAeroColorScheme].
 */
enum class AppTheme {
    LIGHT_AERO,
    DARK_AERO
}

fun AppTheme.colorScheme(): AeroColorScheme =
    when (this) {
        AppTheme.LIGHT_AERO -> AeroLightColorScheme
        AppTheme.DARK_AERO -> AeroDarkColorScheme
    }
