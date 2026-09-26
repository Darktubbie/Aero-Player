package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlin.math.sin

/**
 * Capa 1 de la jerarquía visual: fondo estático de pantalla
 * completa (recortado con [ContentScale.Crop] para llenar
 * cualquier proporción de pantalla sin deformarse).
 *
 * Fase 1 (0.5.0) — fondos independientes por tema: el recurso a
 * dibujar ya no es fijo, sino que sale de [AeroColors.BackgroundRes],
 * que resuelve al `drawable` correspondiente según el tema activo
 * ([com.darktubbie.aeroplayer.ui.theme.AeroLightColorScheme] o
 * [com.darktubbie.aeroplayer.ui.theme.AeroDarkColorScheme]). Cambiar
 * el tema en Ajustes cambia automáticamente esta imagen.
 *
 * Los archivos incluidos ahora mismo
 * (`res/drawable/aero_background_light.png` y
 * `res/drawable/aero_background_dark.png`) son marcadores de
 * posición claramente identificados como tales — el fondo final de
 * cada tema lo da el desarrollador reemplazando directamente esos
 * mismos archivos, sin tocar este código.
 *
 * Fase 4 de Dynamic Aero (0.5.0) — profundidad/parallax entre capas:
 * antes esta capa era 100% estática. Ahora tiene una deriva MUY
 * lenta y sutil (desplazamiento + zoom leve) para reforzar que es
 * la capa MÁS LEJANA de las tres ([BackgroundLayer] < [MidgroundLayer]
 * < [ForegroundLayer]):
 *
 * - Mismo mecanismo que [MidgroundLayer] (una sola animación
 *   compartida, aplicada en `Modifier.graphicsLayer { }`, que es
 *   fase de layout/dibujo, no de recomposición) y mismo criterio de
 *   cuándo animar: nada si el sistema tiene "eliminar animaciones"
 *   activado, nada si no hay música sonando, nada si la intensidad
 *   es [AmbientIntensity.OFF] o [AmbientIntensity.STATIC].
 * - Período mucho más largo (2.5x el de [MidgroundLayer] en cada
 *   nivel de intensidad) y amplitud mucho menor (un tercio de la de
 *   [MidgroundLayer]): la capa más lejana se percibe moviéndose más
 *   lento, como corresponde a la profundidad.
 * - El zoom leve (1.06x en vez de 1.0x cuando está animado) evita
 *   que el desplazamiento deje ver un borde sin imagen — el margen
 *   de sobra de [ContentScale.Crop] normalmente no alcanza para
 *   correr la imagen sin revelar el borde.
 * - Cuando no está animado (OFF/STATIC, sin música, o reduce
 *   motion), el zoom vuelve a 1.0x y el offset a 0: el resultado es
 *   idéntico en píxeles al de antes de esta fase.
 */
@Composable
fun BackgroundLayer(
    modifier: Modifier = Modifier,
    intensity: AmbientIntensity = LocalAmbientIntensity.current
) {

    val context = LocalContext.current

    val reduceMotion =
        remember {
            isSystemReduceMotionEnabled(context)
        }

    val isMusicPlaying =
        LocalAmbientPlayback.current.isPlaying

    val effectiveIntensity =
        if (reduceMotion || !isMusicPlaying) {
            AmbientIntensity.OFF
        } else {
            intensity
        }

    val isAnimated =
        effectiveIntensity != AmbientIntensity.OFF &&
        effectiveIntensity != AmbientIntensity.STATIC

    // Un tercio de la amplitud y 2.5x el período de MidgroundLayer
    // en cada nivel — la capa de más atrás se mueve menos y más
    // despacio, que es lo que hace que se lea como "más lejos".
    val amplitudeFraction: Float =
        when (effectiveIntensity) {
            AmbientIntensity.LOW -> 0.012f
            AmbientIntensity.NORMAL -> 0.022f
            AmbientIntensity.HIGH -> 0.035f
            else -> 0f
        }

    val periodMs =
        when (effectiveIntensity) {
            AmbientIntensity.LOW -> 25000
            AmbientIntensity.HIGH -> 12500
            else -> 17500
        }

    val phase: State<Float>? =
        if (isAnimated) {

            val infiniteTransition =
                rememberInfiniteTransition(
                    label = "background-parallax"
                )

            infiniteTransition.animateFloat(
                initialValue = 0f,

                targetValue =
                    (2 * Math.PI).toFloat(),

                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = periodMs,
                                easing = LinearEasing
                            )
                    ),

                label = "background-phase"
            )

        } else {
            null
        }

    Image(
        painter =
            painterResource(
                id = AeroColors.BackgroundRes
            ),

        contentDescription = null,

        contentScale =
            ContentScale.Crop,

        modifier =
            modifier.graphicsLayer {

                val currentPhase =
                    phase?.value

                if (currentPhase == null) {
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                    return@graphicsLayer
                }

                scaleX = 1.06f
                scaleY = 1.06f

                val amplitudePx =
                    size.minDimension * amplitudeFraction

                translationX =
                    sin(currentPhase) * amplitudePx

                translationY =
                    sin(currentPhase * 0.7f) * amplitudePx
            }
    )
}
