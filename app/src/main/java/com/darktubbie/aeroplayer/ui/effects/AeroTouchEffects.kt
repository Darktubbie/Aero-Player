package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * Microinteracciones Aero (Fase 10, 0.6.0).
 *
 * Dos piezas pequeñas, ambas dentro de ui/effects/ y gobernadas por
 * el MISMO sistema de intensidad que ya usan las capas de fondo
 * ([AmbientIntensity] vía [LocalAmbientIntensity]) y por la misma
 * preferencia de "eliminar animaciones" del sistema
 * ([isSystemReduceMotionEnabled]) — no hay un ajuste ni un sistema
 * de animación aparte:
 *
 *  1. Toque con "gota de agua": al TOCAR (no al hacer scroll ni
 *     arrastrar) en cualquier parte de la app aparece una onda doble
 *     con un destello y unas burbujitas que suben. Se detecta en el
 *     Box raíz con `PointerEventPass.Initial` (observa, nunca
 *     consume: ningún botón ni gesto de abajo se entera) y se dibuja
 *     en una sola capa Canvas encima de todo, que no recibe toques.
 *  2. Rebote al presionar ([aeroPressScale]): los controles de
 *     reproducción se hunden un poco al presionarlos y vuelven con
 *     un pequeño rebote, también sin consumir el gesto.
 *
 * Intensidad: OFF y STATIC no muestran nada (mismo criterio que el
 * resto del sistema: "sin animación"); LOW = solo la onda; NORMAL =
 * onda + destello + 3 burbujitas; HIGH = onda + destello + 5.
 *
 * Costo: sin toques recientes no hay ningún fotograma programado ni
 * nada que dibujar; mientras dura un efecto (~1.2 s) corre un solo
 * bucle por fotograma para toda la capa.
 */

private const val EFFECT_TOTAL_SECONDS = 1.25f
private const val MAX_CONCURRENT_EFFECTS = 6
private const val TAP_MAX_MILLIS = 350L

private class TapBubble(
    val dxDp: Float,
    val riseDp: Float,
    val sizeDp: Float,
    val delay: Float,
    val duration: Float,
    val wobble: Float
)

private class TapEffect(
    val position: Offset,
    val startNanos: Long,
    val bubbles: List<TapBubble>,
    val sparkle: Boolean
)

@Stable
class AeroTouchEffectsState {

    private var enabled = false

    private var intensity = AmbientIntensity.NORMAL

    private val effects = ArrayList<TapEffect>()

    // Leídos solo desde el dibujo / el LaunchedEffect de la capa.
    internal var nowNanos by mutableLongStateOf(0L)
        private set

    internal var kick by mutableIntStateOf(0)
        private set

    internal fun configure(
        intensity: AmbientIntensity,
        reduceMotion: Boolean
    ) {

        this.intensity = intensity

        enabled =
            !reduceMotion &&
                intensity != AmbientIntensity.OFF &&
                intensity != AmbientIntensity.STATIC
    }

    internal fun hasEffects(): Boolean =
        effects.isNotEmpty()

    internal fun spawn(
        position: Offset
    ) {

        if (!enabled) {
            return
        }

        val bubbleCount =
            when (intensity) {
                AmbientIntensity.NORMAL -> 3
                AmbientIntensity.HIGH -> 5
                else -> 0
            }

        val bubbles =
            List(bubbleCount) {

                TapBubble(
                    dxDp = Random.nextFloat() * 36f - 18f,
                    riseDp = 38f + Random.nextFloat() * 46f,
                    sizeDp = 7f + Random.nextFloat() * 7f,
                    delay = Random.nextFloat() * 0.12f,
                    duration = 0.80f + Random.nextFloat() * 0.35f,
                    wobble = Random.nextFloat() * 2f * PI.toFloat()
                )
            }

        if (effects.size >= MAX_CONCURRENT_EFFECTS) {
            effects.removeAt(0)
        }

        val now = System.nanoTime()

        effects.add(
            TapEffect(
                position = position,
                startNanos = now,
                bubbles = bubbles,
                sparkle = intensity != AmbientIntensity.LOW
            )
        )

        nowNanos = now

        kick++
    }

    internal fun tick(
        frameNanos: Long
    ) {

        effects.removeAll {
            (frameNanos - it.startNanos) / 1_000_000_000f >
                EFFECT_TOTAL_SECONDS
        }

        nowNanos = frameNanos
    }

    internal fun draw(
        scope: DrawScope,
        tint: Color,
        accent: Color
    ) {

        val now = nowNanos

        for (i in effects.indices) {

            val effect = effects[i]

            val seconds =
                ((now - effect.startNanos) / 1_000_000_000f)
                    .coerceAtLeast(0f)

            if (seconds > EFFECT_TOTAL_SECONDS) {
                continue
            }

            scope.drawTapEffect(effect, seconds, tint, accent)
        }
    }
}

/**
 * Estado de las microinteracciones de toque. Hay que llamarlo
 * DENTRO de los `CompositionLocalProvider` de la app (necesita
 * [LocalAmbientIntensity]) y usar el resultado tanto en
 * [aeroTouchEffects] (el detector, en el Box raíz) como en
 * [AeroTouchEffectsLayer] (el dibujo, hijo de ese mismo Box).
 */
@Composable
fun rememberAeroTouchEffectsState(): AeroTouchEffectsState {

    val context = LocalContext.current

    val intensity = LocalAmbientIntensity.current

    val reduceMotion =
        remember {
            isSystemReduceMotionEnabled(context)
        }

    val state =
        remember {
            AeroTouchEffectsState()
        }

    state.configure(intensity, reduceMotion)

    return state
}

/**
 * Detector de toques (no de arrastres): solo reacciona a un toque
 * corto sin desplazamiento, así que hacer scroll por una lista no
 * dispara ondas. Observa en `PointerEventPass.Initial` y nunca
 * consume, igual que el detector de inactividad de Rest Mode.
 */
fun Modifier.aeroTouchEffects(
    state: AeroTouchEffectsState
): Modifier =
    pointerInput(state) {

        val slop = viewConfiguration.touchSlop

        awaitEachGesture {

            val down =
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )

            val start = down.position

            val startedAt = down.uptimeMillis

            var moved = false

            var multiTouch = false

            while (true) {

                val event =
                    awaitPointerEvent(PointerEventPass.Initial)

                if (event.changes.size > 1) {
                    multiTouch = true
                }

                val change =
                    event.changes.firstOrNull {
                        it.id == down.id
                    }
                        ?: break

                if ((change.position - start).getDistance() > slop) {
                    moved = true
                }

                if (!change.pressed) {

                    if (
                        !moved &&
                        !multiTouch &&
                        change.uptimeMillis - startedAt <= TAP_MAX_MILLIS
                    ) {
                        state.spawn(start)
                    }

                    break
                }
            }
        }
    }

/**
 * Capa de dibujo de las ondas. Va como hijo del Box raíz, encima del
 * contenido; no tiene ningún modificador de entrada, así que no
 * intercepta ni un toque.
 */
@Composable
fun AeroTouchEffectsLayer(
    state: AeroTouchEffectsState,
    modifier: Modifier = Modifier
) {

    val tint =
        AeroColors.AmbientBubbleTint

    val accent =
        AeroColors.Accent

    // Un solo bucle por fotograma para toda la capa; termina solo
    // cuando no queda ningún efecto vivo.
    LaunchedEffect(state.kick) {

        while (state.hasEffects()) {

            androidx.compose.runtime.withFrameNanos {
                state.tick(it)
            }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {

        // Leer nowNanos dentro del dibujo es lo que lo redibuja
        // por fotograma, sin recomponer nada.
        state.draw(this, tint, accent)
    }
}

// ---------------------------------------------------------------
// Dibujo de un efecto
// ---------------------------------------------------------------

private fun DrawScope.drawTapEffect(
    effect: TapEffect,
    t: Float,
    tint: Color,
    accent: Color
) {

    val c = effect.position

    // Onda doble: una grande y otra más chica y retrasada, como una
    // gota sobre agua.
    drawRipple(c, t, 0f, 0.65f, 6.dp.toPx(), 52.dp.toPx(), 0.55f, accent)

    drawRipple(c, t, 0.12f, 0.60f, 4.dp.toPx(), 32.dp.toPx(), 0.35f, accent)

    // Destello: estrella de 4 puntas que aparece y se apaga rápido.
    if (effect.sparkle) {

        val p = t / 0.28f

        if (p in 0f..1f) {

            val size = (0.6f + 0.7f * p) * 14.dp.toPx()

            val a = sin(p * PI.toFloat())

            val white = Color.White.copy(alpha = 0.9f * a)

            val stroke = 1.5.dp.toPx()

            drawLine(
                white,
                Offset(c.x - size, c.y),
                Offset(c.x + size, c.y),
                stroke,
                StrokeCap.Round
            )

            drawLine(
                white,
                Offset(c.x, c.y - size),
                Offset(c.x, c.y + size),
                stroke,
                StrokeCap.Round
            )

            val d = size * 0.45f

            val faint = Color.White.copy(alpha = 0.55f * a)

            drawLine(
                faint,
                Offset(c.x - d, c.y - d),
                Offset(c.x + d, c.y + d),
                stroke * 0.8f,
                StrokeCap.Round
            )

            drawLine(
                faint,
                Offset(c.x - d, c.y + d),
                Offset(c.x + d, c.y - d),
                stroke * 0.8f,
                StrokeCap.Round
            )

            drawCircle(
                Color.White.copy(alpha = a),
                2.5.dp.toPx(),
                c
            )
        }
    }

    // Burbujitas que suben con un leve balanceo, con el mismo
    // lenguaje visual que las burbujas ambientales.
    for (bubble in effect.bubbles) {

        val q = (t - bubble.delay) / bubble.duration

        if (q < 0f || q > 1f) {
            continue
        }

        val ease = 1f - (1f - q) * (1f - q)

        val x =
            c.x +
                bubble.dxDp.dp.toPx() +
                sin(q * 2f * PI.toFloat() + bubble.wobble) * 4.dp.toPx()

        val y =
            c.y - 6.dp.toPx() - bubble.riseDp.dp.toPx() * ease

        val r = bubble.sizeDp.dp.toPx() / 2f

        val a =
            min(q / 0.10f, 1f) * min((1f - q) / 0.35f, 1f)

        val center = Offset(x, y)

        drawCircle(tint.copy(alpha = 0.22f * a), r, center)

        drawCircle(
            tint.copy(alpha = 0.55f * a),
            r,
            center,
            style = Stroke(1.dp.toPx())
        )

        drawCircle(
            Color.White.copy(alpha = 0.8f * a),
            r * 0.22f,
            Offset(x - r * 0.3f, y - r * 0.3f)
        )
    }
}

private fun DrawScope.drawRipple(
    center: Offset,
    t: Float,
    delay: Float,
    duration: Float,
    startRadius: Float,
    maxRadius: Float,
    maxAlpha: Float,
    accent: Color
) {

    val p = (t - delay) / duration

    if (p < 0f || p > 1f) {
        return
    }

    val ease = 1f - (1f - p) * (1f - p) * (1f - p)

    val radius = startRadius + (maxRadius - startRadius) * ease

    val a = (1f - p) * maxAlpha

    // Relleno muy tenue (el "agua" que se mueve), borde blanco de
    // reflejo y un contorno de acento apenas más afuera que da
    // lectura sobre fondos claros.
    drawCircle(
        Color.White.copy(alpha = a * 0.22f),
        radius,
        center
    )

    drawCircle(
        accent.copy(alpha = a * 0.55f),
        radius + 1.5.dp.toPx(),
        center,
        style = Stroke(1.2.dp.toPx())
    )

    drawCircle(
        Color.White.copy(alpha = a),
        radius,
        center,
        style = Stroke(2.dp.toPx())
    )
}

// ---------------------------------------------------------------
// Rebote al presionar
// ---------------------------------------------------------------

/**
 * Hunde levemente el elemento mientras se lo presiona y lo devuelve
 * con un pequeño rebote al soltar. Observa en
 * `PointerEventPass.Initial` y no consume nada, así que se puede
 * agregar a cualquier control sin cambiar cómo responde. Respeta
 * "eliminar animaciones" del sistema.
 *
 * Al ser un modificador de transformación gráfica, no recompone ni
 * relayoutea: solo cambia la matriz de la capa.
 */
@Composable
fun Modifier.aeroPressScale(
    pressedScale: Float = 0.93f
): Modifier {

    val context = LocalContext.current

    val reduceMotion =
        remember {
            isSystemReduceMotionEnabled(context)
        }

    val scale =
        remember {
            Animatable(1f)
        }

    val scope =
        rememberCoroutineScope()

    if (reduceMotion) {
        return this
    }

    return this
        .pointerInput(Unit) {

            awaitEachGesture {

                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )

                scope.launch {
                    scale.animateTo(pressedScale, tween(90))
                }

                waitForUpOrCancellation(PointerEventPass.Initial)

                scope.launch {
                    scale.animateTo(
                        1f,
                        spring(
                            dampingRatio = 0.45f,
                            stiffness = 500f
                        )
                    )
                }
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}
