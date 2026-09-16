package com.darktubbie.aeroplayer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta centralizada del estilo visual Aero (Frutiger Aero /
 * Windows 7 Aero / glassmorphism).
 *
 * Extraída de MainActivity sin cambiar ningún valor (Fase 4 del
 * plan / mejora E9 del informe): mismos colores exactos, ahora
 * reutilizables desde cualquier pantalla nueva (mini-player, Now
 * Playing, biblioteca por álbumes/artistas...) en vez de repetir
 * hexadecimales sueltos por toda la UI.
 */
object AeroColors {

    // Fondo de la app.
    val BackgroundGradient =
        listOf(
            Color(0xFF35BCEB),
            Color(0xFF73DDF3),
            Color(0xFFB8F1E2),
            Color(0xFFEFFFF0)
        )

    // Texto sobre superficies claras / paneles glass.
    val TextPrimary = Color(0xFF175B70)
    val TextSecondary = Color(0xFF467D88)
    val TextTertiary = Color(0xFF5D8C94)
    val TextMuted = Color(0xFF397B82)

    // Texto/subtítulo sobre el fondo degradado.
    val OnBackgroundSubtitle = Color(0xFFE7FFFF)

    // Acentos: iconos activos, refresh, add, etc.
    val Accent = Color(0xFF08779B)

    // Placeholder de portada cuando no hay artwork.
    val AlbumArtPlaceholderGradient =
        listOf(
            Color(0xFFB8F4FF),
            Color(0xFF70D8E9),
            Color(0xFF8BE6B0)
        )
}
