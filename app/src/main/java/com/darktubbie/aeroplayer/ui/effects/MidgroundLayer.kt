package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Capa 2 de la jerarquía visual (Fase 5 del plan de evolución
 * visual): elementos ambientales/midground, ahora animados.
 *
 * Las mismas 5 burbujas de las Fases 3-4 (mismos tamaños, colores y
 * posiciones base) ganan un balanceo vertical/horizontal lento y
 * sutil, pensado para bajo consumo:
 *
 * - Una sola animación compartida ([phase], un `Float` que recorre
 *   0..2π en bucle) impulsa el movimiento de las 5 burbujas — no
 *   hay una animación independiente por burbuja.
 * - El desplazamiento se aplica con `Modifier.offset { ... }`
 *   (variante de layout, no de recomposición): leer [phase] ahí
 *   solo dispara una nueva pasada de posicionamiento, no vuelve a
 *   recomponer los Box ni sus modificadores de color/forma.
 * - Si el sistema tiene activada la preferencia de accesibilidad
 *   "eliminar animaciones", [intensity] se ignora y no se anima
 *   nada (ver [isSystemReduceMotionEnabled]).
 * - Fase 7: tampoco se anima nada mientras no haya música sonando
 *   ([LocalAmbientPlayback]) — burbujas quietas si la reproducción
 *   está pausada o no hay nada reproduciéndose.
 * - [AmbientIntensity.OFF] y [AmbientIntensity.STATIC] tampoco
 *   animan nada: las burbujas quedan exactamente como en la Fase 4.
 */
@Composable
fun MidgroundLayer(
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

    val amplitude: Dp =
        when (effectiveIntensity) {
            AmbientIntensity.LOW -> 5.dp
            AmbientIntensity.NORMAL -> 10.dp
            AmbientIntensity.HIGH -> 18.dp
            else -> 0.dp
        }

    val periodMs =
        when (effectiveIntensity) {
            AmbientIntensity.LOW -> 10000
            AmbientIntensity.HIGH -> 5000
            else -> 7000
        }

    val phase: State<Float>? =
        if (isAnimated) {

            val infiniteTransition =
                rememberInfiniteTransition(
                    label = "midground-drift"
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

                label = "midground-phase"
            )

        } else {
            null
        }

    val density = LocalDensity.current

    /**
     * Desplazamiento en px para una burbuja concreta, a partir de
     * la fase compartida más un desfase propio (para que las 5 no
     * se muevan exactamente igual y se vea orgánico en vez de
     * mecánico).
     */
    fun drift(
        phaseOffset: Float,
        horizontal: Boolean
    ): Int {

        val currentPhase =
            phase?.value
                ?: return 0

        val wave =
            sin(
                currentPhase +
                phaseOffset +
                if (horizontal) {
                    (Math.PI / 2).toFloat()
                } else {
                    0f
                }
            )

        val amplitudePx =
            with(density) {
                amplitude.toPx()
            }

        return (wave * amplitudePx).roundToInt()
    }

    fun ambientOffset(
        baseX: Dp,
        baseY: Dp,
        phaseOffset: Float
    ): Modifier =

        Modifier.offset {

            IntOffset(
                x =
                    with(density) {
                        baseX.roundToPx()
                    } +
                    drift(
                        phaseOffset,
                        horizontal = true
                    ),

                y =
                    with(density) {
                        baseY.roundToPx()
                    } +
                    drift(
                        phaseOffset,
                        horizontal = false
                    )
            )
        }

    Box(
        modifier = modifier
    ) {

        Box(
            modifier =
                Modifier
                    .size(320.dp)
                    .then(
                        ambientOffset(
                            baseX = (-80).dp,
                            baseY = 40.dp,
                            phaseOffset = 0f
                        )
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0x99B7FFEF),
                                    Color.Transparent
                                )
                        )
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(360.dp)
                    .then(
                        ambientOffset(
                            baseX = 170.dp,
                            baseY = 120.dp,
                            phaseOffset = 1.2f
                        )
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0x7787E8FF),
                                    Color.Transparent
                                )
                        )
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .then(
                        ambientOffset(
                            baseX = 285.dp,
                            baseY = 80.dp,
                            phaseOffset = 2.4f
                        )
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.18f
                        )
                    )
                    .border(
                        2.dp,
                        Color.White.copy(
                            alpha = 0.42f
                        ),
                        CircleShape
                    )
        ) {

            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .offset(
                            x = 20.dp,
                            y = 16.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            Color.White.copy(
                                alpha = 0.55f
                            )
                        )
            )
        }

        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .then(
                        ambientOffset(
                            baseX = 28.dp,
                            baseY = 150.dp,
                            phaseOffset = 3.6f
                        )
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.22f
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(
                            alpha = 0.45f
                        ),
                        CircleShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(70.dp)
                    .then(
                        ambientOffset(
                            baseX = 250.dp,
                            baseY = 430.dp,
                            phaseOffset = 4.8f
                        )
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.16f
                        )
                    )
                    .border(
                        2.dp,
                        Color.White.copy(
                            alpha = 0.35f
                        ),
                        CircleShape
                    )
        )
    }
}
