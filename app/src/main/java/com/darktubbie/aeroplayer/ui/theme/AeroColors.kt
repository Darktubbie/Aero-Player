package com.darktubbie.aeroplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Un esquema de color completo de Aero Player (Fase de Dark Aero,
 * post-0.4.0): agrupa todo lo que antes eran `val` sueltos y
 * hardcodeados en [AeroColors], para poder tener más de una
 * variante (Aero claro / Dark Aero) sin duplicar cada pantalla.
 */
data class AeroColorScheme(
    val backgroundGradient: List<Color>,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val onBackgroundSubtitle: Color,
    val accent: Color,
    val albumArtPlaceholderGradient: List<Color>,

    /**
     * Color base de los paneles "glass" translúcidos (tarjetas,
     * filas, bordes) que hay por toda la app — cada sitio le aplica
     * su propio `.copy(alpha = ...)`, así que cambiar este único
     * valor por tema alcanza para que todo el glassmorphism de la
     * app pase de vidrio claro a vidrio oscuro sin tocar cada
     * pantalla una por una.
     */
    val glassSurfaceBase: Color,

    /**
     * Fondo prácticamente opaco de diálogos y bottom sheets (crear
     * playlist, confirmar borrado, Sleep Timer, etc.) — a diferencia
     * de [glassSurfaceBase], este no lleva alpha variable porque
     * necesita texto legible encima sin depender de qué haya detrás.
     */
    val dialogSurface: Color
)

/**
 * Aero claro: paleta original de la app (Frutiger Aero / Windows 7
 * Aero / glassmorphism), sin cambios de valores respecto a antes de
 * esta fase.
 */
val AeroLightColorScheme =
    AeroColorScheme(
        backgroundGradient =
            listOf(
                Color(0xFF35BCEB),
                Color(0xFF73DDF3),
                Color(0xFFB8F1E2),
                Color(0xFFEFFFF0)
            ),
        textPrimary = Color(0xFF175B70),
        textSecondary = Color(0xFF467D88),
        textTertiary = Color(0xFF5D8C94),
        textMuted = Color(0xFF397B82),
        onBackgroundSubtitle = Color(0xFFE7FFFF),
        accent = Color(0xFF08779B),
        albumArtPlaceholderGradient =
            listOf(
                Color(0xFFB8F4FF),
                Color(0xFF70D8E9),
                Color(0xFF8BE6B0)
            ),
        glassSurfaceBase = Color.White,
        dialogSurface = Color(0xFFEFFFF0)
    )

/**
 * Dark Aero: variante oscura pensada para verse como una versión
 * nocturna/tecnológica del mismo vidrio Aero, no como sus colores
 * invertidos ni como un tema cyberpunk.
 *
 * Decisiones de paleta:
 * - El degradado de fondo mantiene la misma idea del claro (más
 *   luminoso arriba, más denso abajo) pero en azules profundos casi
 *   negros en vez de celeste a crema — sensación de profundidad
 *   nocturna, no una pantalla plana en negro puro.
 * - Los acentos son cian/azul eléctrico (nunca magenta, verde neón
 *   ni violeta) para que se sienta futurista sin cruzar a
 *   cyberpunk.
 * - El texto pasa a tonos claros cian/hielo para mantener contraste
 *   sobre fondos y vidrios oscuros.
 * - `glassSurfaceBase` es azul-pizarra oscuro, no negro puro ni
 *   blanco con alpha bajo: sigue leyéndose como vidrio (superficie
 *   distinta del fondo que tiene detrás), pero un vidrio oscuro con
 *   reflejo azulado en vez de vidrio esmerilado claro.
 */
val AeroDarkColorScheme =
    AeroColorScheme(
        backgroundGradient =
            listOf(
                Color(0xFF0C2B42),
                Color(0xFF081B2E),
                Color(0xFF05101E),
                Color(0xFF01050B)
            ),
        textPrimary = Color(0xFFCFF3FF),
        textSecondary = Color(0xFF8FD0E8),
        textTertiary = Color(0xFF6AA9C2),
        textMuted = Color(0xFF5A93AC),
        onBackgroundSubtitle = Color(0xFFAEE7FB),
        accent = Color(0xFF2DD4F5),
        albumArtPlaceholderGradient =
            listOf(
                Color(0xFF0F2A43),
                Color(0xFF13415E),
                Color(0xFF1C6E8C)
            ),
        glassSurfaceBase = Color(0xFF16283F),
        dialogSurface = Color(0xFF0E1B2C)
    )

/**
 * Esquema activo, provisto una sola vez en MainActivity a partir de
 * la preferencia de tema guardada — mismo patrón que
 * [com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity].
 * Default [AeroLightColorScheme]: el tema que ya existía, para que
 * una preferencia nunca leída (primera instalación, o una versión
 * vieja de la app sin esta preferencia) se comporte igual que antes.
 */
val LocalAeroColorScheme =
    compositionLocalOf { AeroLightColorScheme }

/**
 * Paleta centralizada del estilo visual Aero (Frutiger Aero /
 * Windows 7 Aero / glassmorphism).
 *
 * Cada propiedad lee del esquema activo ([LocalAeroColorScheme]) en
 * vez de tener un valor fijo — así, agregar Dark Aero no requirió
 * tocar ninguna de las decenas de pantallas que ya escriben
 * `AeroColors.Accent`, `AeroColors.TextPrimary`, etc.: siguen
 * compilando y funcionando igual, solo que ahora el valor devuelto
 * depende del tema elegido en Ajustes.
 */
object AeroColors {

    val BackgroundGradient: List<Color>
        @Composable get() = LocalAeroColorScheme.current.backgroundGradient

    val TextPrimary: Color
        @Composable get() = LocalAeroColorScheme.current.textPrimary

    val TextSecondary: Color
        @Composable get() = LocalAeroColorScheme.current.textSecondary

    val TextTertiary: Color
        @Composable get() = LocalAeroColorScheme.current.textTertiary

    val TextMuted: Color
        @Composable get() = LocalAeroColorScheme.current.textMuted

    val OnBackgroundSubtitle: Color
        @Composable get() = LocalAeroColorScheme.current.onBackgroundSubtitle

    val Accent: Color
        @Composable get() = LocalAeroColorScheme.current.accent

    val AlbumArtPlaceholderGradient: List<Color>
        @Composable get() =
            LocalAeroColorScheme.current.albumArtPlaceholderGradient

    val GlassSurfaceBase: Color
        @Composable get() = LocalAeroColorScheme.current.glassSurfaceBase

    val DialogSurface: Color
        @Composable get() = LocalAeroColorScheme.current.dialogSurface
}
