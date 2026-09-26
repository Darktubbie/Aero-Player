package com.darktubbie.aeroplayer.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.darktubbie.aeroplayer.R

/**
 * Un esquema de color completo de Aero Player (Fase de Dark Aero,
 * post-0.4.0): agrupa todo lo que antes eran `val` sueltos y
 * hardcodeados en [AeroColors], para poder tener más de una
 * variante (Aero claro / Dark Aero) sin duplicar cada pantalla.
 */
data class AeroColorScheme(
    val backgroundGradient: List<Color>,

    /**
     * Fondo real de pantalla completa de [com.darktubbie.aeroplayer.ui.effects.BackgroundLayer]
     * (Fase 1, 0.5.0): cada tema apunta a su propio recurso
     * `res/drawable`, así que basta con reemplazar ese archivo de
     * imagen para darle un fondo distinto a Light Aero o a Aero
     * Dark sin tocar ninguna lógica de la interfaz. [backgroundGradient]
     * queda sin usar por esta capa (se conserva por si algún efecto
     * ambiental lo necesita más adelante).
     */
    @DrawableRes val backgroundRes: Int,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val onBackgroundSubtitle: Color,
    val accent: Color,

    /**
     * Color de texto/ícono legible ENCIMA de un fondo sólido pintado
     * con [accent] (Fase 7, 0.5.0 — Aero Dark UI refinement):
     * ejemplos reales son el chip de tema/idioma seleccionado y el
     * de orden de canciones en Ajustes. Antes esos dos sitios
     * usaban blanco fijo, que tenía muy poco contraste sobre el
     * verde/cian claro de [accent] — más notorio todavía con el
     * verde de Aero Dark, que es más luminoso que un acento oscuro.
     */
    val onAccent: Color,

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
    val dialogSurface: Color,

    /**
     * Adaptación de los efectos ambientales al tema (Fase 4, 0.5.0
     * — Dynamic Aero): antes [com.darktubbie.aeroplayer.ui.effects.MidgroundLayer]
     * y [com.darktubbie.aeroplayer.ui.effects.AmbientEventShapes]
     * tenían sus colores fijos en blanco/celeste sin importar el
     * tema activo. [ambientBubbleTint] es el color base de cuerpo y
     * borde de las burbujas (no de los reflejos/highlights, que se
     * mantienen blancos puros a propósito en ambos temas por ser un
     * brillo especular, no el color del cuerpo). [ambientGlowPrimary]
     * y [ambientGlowSecondary] son los dos degradados radiales de
     * las burbujas grandes de [MidgroundLayer].
     */
    val ambientBubbleTint: Color,
    val ambientGlowPrimary: Color,
    val ambientGlowSecondary: Color
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
        backgroundRes = R.drawable.aero_background_light,
        textPrimary = Color(0xFF175B70),
        textSecondary = Color(0xFF467D88),
        textTertiary = Color(0xFF5D8C94),
        textMuted = Color(0xFF397B82),
        onBackgroundSubtitle = Color(0xFFE7FFFF),
        accent = Color(0xFF08779B),
        onAccent = Color.White,
        albumArtPlaceholderGradient =
            listOf(
                Color(0xFFB8F4FF),
                Color(0xFF70D8E9),
                Color(0xFF8BE6B0)
            ),
        glassSurfaceBase = Color.White,
        dialogSurface = Color(0xFFEFFFF0),
        // Mismos valores que estaban hardcodeados antes de esta
        // fase: Light Aero se ve exactamente igual que antes.
        ambientBubbleTint = Color.White,
        ambientGlowPrimary = Color(0x99B7FFEF),
        ambientGlowSecondary = Color(0x7787E8FF)
    )

/**
 * Dark Aero: variante oscura pensada para verse como una versión
 * nocturna/tecnológica del mismo vidrio Aero, no como sus colores
 * invertidos ni como un tema cyberpunk.
 *
 * Decisiones de paleta (Fase 7, 0.5.0 — segunda pasada, Aero Dark UI
 * Refinement):
 * - El degradado de fondo mantiene la misma idea del claro (más
 *   luminoso arriba, más denso abajo) pero en azules profundos casi
 *   negros en vez de celeste a crema — sensación de profundidad
 *   nocturna, no una pantalla plana en negro puro.
 * - El acento principal pasó de cian a un verde tecnológico
 *   ligeramente luminoso (nostalgia Y2K/consolas de principios de
 *   los 2000, no verde neón/cyberpunk): se usa en controles
 *   principales, selección, progreso y estados activos — exactamente
 *   los mismos usos que ya tenía el acento, ningún llamado nuevo.
 *   El cian/teal no desaparece: sigue vivo como color COMPLEMENTARIO
 *   en los degradados ambientales ([ambientGlowPrimary]/
 *   [ambientGlowSecondary], sin cambios) y en el tinte de las
 *   burbujas ([ambientBubbleTint]), logrando el "verde primario +
 *   cyan/teal de acompañamiento" que pide el brief sin agregar
 *   ningún campo nuevo al esquema.
 * - El texto se mantiene en tonos cian/hielo (no se pasó a verde):
 *   un verde saturado como color de texto de lectura prolongada
 *   cansa más la vista que un cian suave, y ya cumplía con el
 *   contraste necesario.
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
        backgroundRes = R.drawable.aero_background_dark,
        textPrimary = Color(0xFFCFF3FF),
        textSecondary = Color(0xFF8FD0E8),
        textTertiary = Color(0xFF6AA9C2),
        textMuted = Color(0xFF5A93AC),
        onBackgroundSubtitle = Color(0xFFAEE7FB),
        accent = Color(0xFF3ED98E),
        // Verde bastante claro/luminoso: blanco encima quedaba con
        // poco contraste. Un verde-negro bien oscuro (no negro puro,
        // para que no se sienta desconectado del resto de la
        // paleta) da la relación de contraste que pide accesibilidad
        // sin salir de la familia de color de este acento.
        onAccent = Color(0xFF04241A),
        albumArtPlaceholderGradient =
            listOf(
                Color(0xFF0F2A43),
                Color(0xFF13415E),
                Color(0xFF1C6E8C)
            ),
        glassSurfaceBase = Color(0xFF16283F),
        dialogSurface = Color(0xFF0E1B2C),
        // Burbujas tiñen a celeste-hielo (mismo tono que textPrimary)
        // en vez de blanco puro, para que se lean como parte del
        // vidrio oscuro y no como manchas blancas fuera de lugar; los
        // degradados pasan a glow cian/azul profundo (acento + el
        // azul más oscuro de albumArtPlaceholderGradient) en vez del
        // celeste-verdoso pensado para el fondo claro.
        ambientBubbleTint = Color(0xFFCFF3FF),
        ambientGlowPrimary = Color(0x552DD4F5),
        ambientGlowSecondary = Color(0x401C6E8C)
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

    val BackgroundRes: Int
        @Composable get() = LocalAeroColorScheme.current.backgroundRes

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

    val OnAccent: Color
        @Composable get() = LocalAeroColorScheme.current.onAccent

    val AlbumArtPlaceholderGradient: List<Color>
        @Composable get() =
            LocalAeroColorScheme.current.albumArtPlaceholderGradient

    val GlassSurfaceBase: Color
        @Composable get() = LocalAeroColorScheme.current.glassSurfaceBase

    val DialogSurface: Color
        @Composable get() = LocalAeroColorScheme.current.dialogSurface

    val AmbientBubbleTint: Color
        @Composable get() = LocalAeroColorScheme.current.ambientBubbleTint

    val AmbientGlowPrimary: Color
        @Composable get() = LocalAeroColorScheme.current.ambientGlowPrimary

    val AmbientGlowSecondary: Color
        @Composable get() = LocalAeroColorScheme.current.ambientGlowSecondary
}
