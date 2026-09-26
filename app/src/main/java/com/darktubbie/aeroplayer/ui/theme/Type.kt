package com.darktubbie.aeroplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.R

/**
 * Fase 1 (0.5.0) — Identidad visual Aero: fuente Frutiger Aero.
 *
 * Se eligió Nunito (Google Fonts, licencia SIL Open Font License
 * 1.1 — ver `app/src/main/assets/licenses/Nunito-OFL.txt`) por ser
 * redondeada, suave y amigable sin caer en una estética "técnica"
 * o futurista, que es justo lo que pide el lenguaje visual
 * Frutiger Aero. Tiene soporte completo de Latin Extended (incluye
 * ñ, á, é, í, ó, ú, ü y el resto de acentos españoles) y mantiene
 * buena legibilidad incluso en los tamaños chicos que usa la app
 * (11–13sp) gracias a sus formas abiertas.
 *
 * [R.font.nunito] es una única fuente variable (eje de peso
 * ExtraLight–Black) en vez de varios archivos estáticos: Android
 * soporta variar el peso de una fuente variable por instancia
 * mediante [FontVariation.Settings] desde API 26 (coincide con el
 * minSdk del proyecto), así que un solo .ttf cubre toda la
 * jerarquía sin sumar peso extra al APK por cada variante.
 */
private val Nunito =
    FontFamily(
        Font(
            resId = R.font.nunito,
            weight = FontWeight.Normal,

            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(400)
                )
        ),

        Font(
            resId = R.font.nunito,
            weight = FontWeight.Medium,

            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(500)
                )
        ),

        Font(
            resId = R.font.nunito,
            weight = FontWeight.SemiBold,

            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(600)
                )
        ),

        Font(
            resId = R.font.nunito,
            weight = FontWeight.Bold,

            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(700)
                )
        )
    )

/**
 * Jerarquía tipográfica de Aero Player.
 *
 * No son los tamaños por defecto de Material 3: son los tamaños
 * que la app ya venía usando (relevados desde el código de
 * 0.4.0), consolidados en las 13 categorías de [Typography] para
 * eliminar valores sueltos y arbitrarios (había tamaños como 10sp,
 * 15sp o 24sp conviviendo sin ningún criterio, y hasta tres tamaños
 * distintos usados como "botón"). El resultado visual es
 * prácticamente idéntico al de 0.4.0 — esto es una organización de
 * lo que ya existía, no un rediseño.
 *
 * Guía de uso por categoría:
 * - `headlineLarge` (30sp) — título de pantalla principal
 *   ("Aero Player", "Más").
 * - `headlineMedium` (22sp) — títulos de sección ("Library") y el
 *   título de la canción en Now Playing.
 * - `headlineSmall` (20sp, Medium) — encabezado de pantallas
 *   secundarias (Favoritos, Carpetas, Historial, Playlists,
 *   Ajustes, Editor APE, nombre de playlist).
 * - `titleLarge` (18sp, Medium) — subtítulos y encabezados de
 *   diálogo/hoja inferior (Sleep Timer, Agregar canciones, estado
 *   vacío de Now Playing).
 * - `titleMedium` (16sp, Medium) — título de diálogos de
 *   confirmación ("¿Eliminar...?").
 * - `titleSmall` (16sp) — texto de cuerpo con énfasis puntual.
 * - `bodyLarge` (15sp) — texto de fila/cuerpo prominente.
 * - `bodyMedium` (14sp) — texto secundario estándar (el más común
 *   de la app).
 * - `bodySmall` (13sp) — texto secundario chico (subtítulos de
 *   lista, hints, placeholders).
 * - `labelLarge` (14sp, Medium) — etiquetas de botón.
 * - `labelMedium` (12sp) — leyendas y metadatos chicos.
 * - `labelSmall` (11sp) — el texto más chico de la interfaz
 *   (navegación inferior, timestamps, chips).
 *
 * `displayLarge/Medium/Small` no los usa ninguna pantalla todavía
 * (la app no tiene ningún texto más grande que el título de
 * 30sp), pero quedan definidos siguiendo la misma progresión de
 * escala para que una futura fase (por ejemplo un número grande en
 * Aero Home) tenga de dónde tomarlos en lugar de inventar un
 * tamaño suelto nuevo.
 */
val AeroTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            ),

        displayMedium =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),

        displaySmall =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp
            ),

        headlineLarge =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp
            ),

        headlineMedium =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp
            ),

        headlineSmall =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            ),

        titleLarge =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            ),

        titleMedium =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),

        titleSmall =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),

        bodyLarge =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            ),

        bodyMedium =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            ),

        bodySmall =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp
            ),

        labelLarge =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),

        labelMedium =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp
            ),

        labelSmall =
            TextStyle(
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp
            )
    )
