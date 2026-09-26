package com.darktubbie.aeroplayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

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

/**
 * Esquema de color de Material 3 propiamente dicho (Fase 7, 0.5.0 —
 * Aero Dark UI Refinement).
 *
 * Hasta esta fase, el `MaterialTheme(...)` raíz de `MainActivity`
 * solo recibía `typography` — nunca un `colorScheme`, así que
 * quedaba en el `lightColorScheme()` por defecto de Material 3 SIN
 * IMPORTAR qué tema Aero estuviera activo. Casi toda la app pinta
 * sus propios colores explícitamente vía [AeroColors] y nunca lo
 * notó, pero cualquier componente de Material 3 sin estilizar a
 * mano (un `DropdownMenu`, los colores no sobreescritos de
 * `SwitchDefaults`/`SliderDefaults`, etc.) heredaba ese esquema
 * claro fijo — el causante concreto de que, por ejemplo, el menú de
 * orden de Biblioteca apareciera como un recuadro blanco genérico
 * en Aero Dark.
 *
 * Esta función traduce cada [AeroColorScheme] activo a un
 * [ColorScheme] de Material 3 real, para que ese tipo de componente
 * sin estilizar a mano herede algo coherente con Aero Dark/Aero
 * Claro en vez del esquema claro fijo de Material. No reemplaza
 * ningún color que la app ya pinta a mano con [AeroColors] — sigue
 * siendo el sistema de colores real de la app; esto solo le da a
 * Material 3 un fallback correcto para lo que [AeroColors] no cubre
 * explícitamente.
 */
fun AppTheme.materialColorScheme(): ColorScheme {

    val aero = colorScheme()

    val base =
        when (this) {
            AppTheme.LIGHT_AERO -> lightColorScheme()
            AppTheme.DARK_AERO -> darkColorScheme()
        }

    return base.copy(
        primary = aero.accent,
        onPrimary = aero.onAccent,
        secondary = aero.accent,
        onSecondary = aero.onAccent,
        background = aero.backgroundGradient.last(),
        onBackground = aero.textPrimary,
        surface = aero.glassSurfaceBase,
        onSurface = aero.textPrimary,
        surfaceVariant = aero.glassSurfaceBase,
        onSurfaceVariant = aero.textSecondary,
        outline = aero.textMuted,
        outlineVariant = aero.glassSurfaceBase.copy(alpha = 0.4f)
    )
}
