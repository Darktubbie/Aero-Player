package com.darktubbie.aeroplayer.ui.album

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/*
 * Vinilo del Album Showcase (Fase 8, 0.6.0).
 *
 * Es un objeto PURAMENTE VISUAL: nada de este archivo conoce a
 * Media3, PlayerRepository ni MainViewModel. Lo único que recibe del
 * exterior es un booleano ("girar o no") que decide quien lo usa.
 *
 * Se separan tres conceptos de estado que no se pisan entre sí:
 *
 *  1. Rotación de reproducción: `spinAngle`/`spinVel` — el giro
 *     continuo del disco mientras suena la música.
 *  2. Transformación manual: `offsetX/Y` y `tiltX/Y` — lo que el
 *     usuario hace al arrastrar el disco (posición e inclinación),
 *     que vuelve a su sitio con un resorte al soltarlo.
 *  3. Estado del líquido: `surfaceTilt`, `level`, `waveAmp` — un
 *     péndulo amortiguado propio, que NO hereda la rotación del
 *     disco: el líquido se dibuja en el marco de la pantalla (la
 *     gravedad es siempre "hacia abajo en pantalla") y solo reacciona
 *     a las aceleraciones que mide del movimiento del disco.
 *
 * Un solo bucle por fotograma (`step`) integra todo, y solo corre
 * mientras haya algo en movimiento: con el disco quieto y sin música
 * no se programa ningún fotograma.
 */

private const val ENTRY_DELAY_S = 0.28f
private const val ENTRY_DURATION_S = 1.0f
private const val ENTRY_ROTATION_DEG = -80f

// Vueltas en reproducción: ~9 s por vuelta, lento y elegante.
private const val SPIN_DEG_PER_S = 40f
private const val SPIN_TAU_S = 0.9f

private const val TILT_MAX_DEG = 14f
private const val BASE_LEVEL = 0.40f

// Fracción del lado del Box que ocupa el disco; el resto deja margen
// para el halo/sombra suave dibujado fuera del borde.
internal const val VINYL_DISC_FRACTION = 0.94f

@Stable
internal class VinylState(
    private val reduceMotion: Boolean
) {

    // --- Geometría en px, la asigna el layout antes del primer frame ---
    var radiusPx: Float = 1f
    var entryShiftPx: Float = 0f
    var maxOffsetPx: Float = 1f

    // --- Estado observable, leído SOLO desde capas gráficas / dibujo,
    // así los cambios por fotograma no recomponen nada. ---
    var entry by mutableFloatStateOf(if (reduceMotion) 1f else 0f)
        private set

    var offsetX by mutableFloatStateOf(0f)
        private set

    var offsetY by mutableFloatStateOf(0f)
        private set

    var tiltX by mutableFloatStateOf(0f)
        private set

    var tiltY by mutableFloatStateOf(0f)
        private set

    var spinAngle by mutableFloatStateOf(0f)
        private set

    var surfaceTilt by mutableFloatStateOf(0f)
        private set

    var level by mutableFloatStateOf(BASE_LEVEL)
        private set

    var waveAmp by mutableFloatStateOf(0f)
        private set

    var wavePhase by mutableFloatStateOf(0f)
        private set

    /** Cambia al empezar/soltar un gesto para reactivar el bucle. */
    var kick by mutableIntStateOf(0)
        private set

    // --- Estado interno (no observable) ---
    var dragging = false
        private set

    private var elapsed =
        if (reduceMotion) ENTRY_DELAY_S + ENTRY_DURATION_S else 0f

    private var rawX = 0f
    private var rawY = 0f
    private var velRawX = 0f
    private var velRawY = 0f

    private var spinVel = 0f

    private var alphaVel = 0f
    private var levelOff = 0f
    private var levelVel = 0f

    private var hasPrev = false
    private var prevX = 0f
    private var prevY = 0f
    private var prevVx = 0f
    private var prevVy = 0f
    private var prevW = 0f
    private var prevEntry = entry
    private var smAx = 0f
    private var smAy = 0f
    private var smEps = 0f

    private var lastAngleDeg = 0f

    // Lecturas para las capas gráficas.
    fun translationX(): Float =
        -(1f - entry) * entryShiftPx + offsetX

    fun spinDegrees(): Float =
        spinAngle + ENTRY_ROTATION_DEG * (1f - entry)

    private fun softLimit(raw: Float): Float {

        val limit = maxOffsetPx.coerceAtLeast(1f)

        return limit * tanh(raw / limit)
    }

    /**
     * Un paso de la simulación. [spinning] es solo "dibujar giro de
     * reproducción": no hay ninguna escritura hacia el reproductor.
     */
    fun step(
        dt: Float,
        spinning: Boolean
    ) {

        // 1. Entrada: el disco se desliza desde detrás del sleeve.
        if (elapsed < ENTRY_DELAY_S + ENTRY_DURATION_S) {

            elapsed += dt

            val raw =
                ((elapsed - ENTRY_DELAY_S) / ENTRY_DURATION_S)
                    .coerceIn(0f, 1f)

            entry = FastOutSlowInEasing.transform(raw)
        }

        // 2. Transformación manual: resorte de vuelta al reposo.
        if (!dragging) {

            val k = 70f
            val c = 9f

            velRawX += (-k * rawX - c * velRawX) * dt
            velRawY += (-k * rawY - c * velRawY) * dt

            rawX += velRawX * dt
            rawY += velRawY * dt

            if (abs(rawX) < 0.05f && abs(velRawX) < 0.5f) {
                rawX = 0f
                velRawX = 0f
            }

            if (abs(rawY) < 0.05f && abs(velRawY) < 0.5f) {
                rawY = 0f
                velRawY = 0f
            }
        }

        offsetX = softLimit(rawX)
        offsetY = softLimit(rawY)

        val limit = maxOffsetPx.coerceAtLeast(1f)

        tiltY = offsetX / limit * TILT_MAX_DEG
        tiltX = -offsetY / limit * TILT_MAX_DEG

        // 3. Rotación de reproducción. Durante un gesto manda el dedo
        // (el ángulo lo escribe el gesto); al soltar, la velocidad que
        // dejó el dedo se acerca suavemente a la de reproducción, así
        // el usuario nunca "pelea" contra la animación.
        if (dragging) {

            spinVel *= exp(-dt / 0.12f)

        } else {

            val target =
                if (spinning && !reduceMotion) SPIN_DEG_PER_S else 0f

            spinVel +=
                (target - spinVel) * (1f - exp(-dt / SPIN_TAU_S))

            spinAngle = (spinAngle + spinVel * dt) % 360f

            if (target == 0f && abs(spinVel) < 0.3f) {
                spinVel = 0f
            }
        }

        // 4. Líquido: mide aceleraciones del disco (en px/s²) y las
        // convierte en inclinación de la superficie.
        val r = radiusPx.coerceAtLeast(1f)

        val x = translationX()
        val y = offsetY

        val entryW = 80f * (entry - prevEntry) / dt

        val w = spinVel + entryW

        if (hasPrev) {

            val lim = 60f * r

            val vx = (x - prevX) / dt
            val vy = (y - prevY) / dt

            val ax = ((vx - prevVx) / dt).coerceIn(-lim, lim)
            val ay = ((vy - prevVy) / dt).coerceIn(-lim, lim)
            val eps =
                ((w - prevW) / dt).coerceIn(-20000f, 20000f)

            smAx += (ax - smAx) * 0.3f
            smAy += (ay - smAy) * 0.3f
            smEps += (eps - smEps) * 0.3f

            prevVx = vx
            prevVy = vy
        }

        hasPrev = true
        prevX = x
        prevY = y
        prevW = w
        prevEntry = entry

        // Aceleración horizontal => la superficie se inclina hacia
        // atrás (inercia); aceleración angular => arrastre viscoso.
        val tiltInput =
            (smAx / (20f * r)).coerceIn(-0.6f, 0.6f) +
                (smEps * 0.00015f).coerceIn(-0.5f, 0.5f)

        // Péndulo amortiguado subamortiguado: sigue oscilando un
        // momento tras soltar y luego se asienta.
        val kLiquid = 38f
        val cLiquid = 3.2f

        alphaVel +=
            (-kLiquid * (surfaceTilt - tiltInput) - cLiquid * alphaVel) * dt

        surfaceTilt =
            (surfaceTilt + alphaVel * dt).coerceIn(-0.9f, 0.9f)

        if (
            abs(surfaceTilt) < 0.0015f &&
            abs(alphaVel) < 0.01f &&
            abs(tiltInput) < 0.002f
        ) {
            surfaceTilt = 0f
            alphaVel = 0f
        }

        // Nivel: un pequeño rebote vertical por aceleración vertical.
        val levelInput =
            (smAy / (40f * r)).coerceIn(-0.06f, 0.06f)

        levelVel +=
            (-50f * (levelOff - levelInput) - 5f * levelVel) * dt

        levelOff += levelVel * dt

        if (
            abs(levelOff) < 0.0005f &&
            abs(levelVel) < 0.01f &&
            abs(levelInput) < 0.001f
        ) {
            levelOff = 0f
            levelVel = 0f
        }

        level = BASE_LEVEL + levelOff

        // Oleaje superficial: proporcional a cuánto se está moviendo
        // el líquido; se apaga solo al asentarse.
        val targetAmp =
            (abs(alphaVel) * 0.012f * r).coerceAtMost(0.035f * r)

        waveAmp +=
            (targetAmp - waveAmp) * (1f - exp(-dt / 0.25f))

        if (waveAmp < 0.05f && targetAmp == 0f) {
            waveAmp = 0f
        }

        // La fase solo avanza si hay oleaje: con el líquido quieto no
        // cambia ningún estado y la capa del líquido no se redibuja,
        // aunque el disco esté girando 60 veces por segundo.
        if (waveAmp > 0f) {
            wavePhase = (wavePhase + dt * 6f) % (2f * PI.toFloat() * 4f)
        }
    }

    /** Verdadero cuando ya no hay nada que animar. */
    fun isAtRest(): Boolean =
        !dragging &&
            elapsed >= ENTRY_DELAY_S + ENTRY_DURATION_S &&
            rawX == 0f &&
            rawY == 0f &&
            spinVel == 0f &&
            surfaceTilt == 0f &&
            alphaVel == 0f &&
            levelOff == 0f &&
            levelVel == 0f &&
            waveAmp == 0f &&
            abs(smAx) < 1f &&
            abs(smAy) < 1f &&
            abs(smEps) < 1f

    // --- Gesto manual (solo visual) ---

    fun onDragStart(rel: Offset) {

        dragging = true

        velRawX = 0f
        velRawY = 0f

        lastAngleDeg = angleOf(rel)

        kick++
    }

    fun onDrag(
        delta: Offset,
        rel: Offset,
        dtSec: Float
    ) {

        rawX += delta.x
        rawY += delta.y

        val angle = angleOf(rel)

        // Cerca del centro el ángulo es ruido: no se usa.
        if (rel.getDistance() > 0.25f * radiusPx && dtSec > 0f) {

            var d = angle - lastAngleDeg

            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f

            spinAngle = (spinAngle + d) % 360f

            spinVel =
                (spinVel * 0.6f + (d / dtSec) * 0.4f)
                    .coerceIn(-900f, 900f)
        }

        lastAngleDeg = angle
    }

    fun onDragEnd(
        vx: Float,
        vy: Float
    ) {

        dragging = false

        velRawX = (vx * 0.3f).coerceIn(-1500f, 1500f)
        velRawY = (vy * 0.3f).coerceIn(-1500f, 1500f)

        kick++
    }

    private fun angleOf(rel: Offset): Float =
        (atan2(rel.y, rel.x) * 180f / PI.toFloat())
}

/**
 * Vinilo de cristal con líquido azul independiente.
 *
 * Capas (de atrás hacia adelante):
 *  1. Cuerpo translúcido (estático).
 *  2. Líquido: se dibuja en el marco de la pantalla y recortado a la
 *     cavidad anular del disco; NO rota con él.
 *  3. Detalles grabados que SÍ rotan (marcas, sectores, cubo): son lo
 *     que hace visible el giro de un disco transparente.
 *  4. Reflejos de cristal fijos respecto a la luz (no rotan).
 *
 * El `Box` exterior no se transforma y es quien recibe el gesto: así
 * las coordenadas del dedo no se mueven mientras el disco se mueve.
 * El gesto solo se acepta sobre la mitad visible del disco; en
 * cualquier otro punto no se consume y el scroll de la lista sigue
 * funcionando.
 */
@Composable
internal fun VinylDisc(
    state: VinylState,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val accent =
        AeroColors.Accent

    Box(
        modifier =
            modifier
                .size(size)
                .pointerInput(state) {

                    awaitEachGesture {

                        val down =
                            awaitFirstDown(requireUnconsumed = true)

                        val half = this.size.width / 2f

                        val center =
                            Offset(half, this.size.height / 2f)

                        val rel0 = down.position - center

                        val discRadius =
                            half * VINYL_DISC_FRACTION

                        // Fuera del círculo, o en la mitad que queda
                        // escondida bajo el sleeve: no es del disco.
                        // Tampoco hay disco que agarrar mientras todavía
                        // se está deslizando desde detrás del sleeve.
                        if (
                            state.entry < 0.85f ||
                            rel0.getDistance() > discRadius ||
                            rel0.x < -0.02f * discRadius
                        ) {
                            return@awaitEachGesture
                        }

                        down.consume()

                        val tracker =
                            VelocityTracker()

                        tracker.addPosition(
                            down.uptimeMillis,
                            down.position
                        )

                        state.onDragStart(rel0)

                        var lastT = down.uptimeMillis

                        while (true) {

                            val event =
                                awaitPointerEvent()

                            val change =
                                event.changes.firstOrNull {
                                    it.id == down.id
                                }
                                    ?: break

                            if (!change.pressed) {

                                val v =
                                    tracker.calculateVelocity()

                                state.onDragEnd(v.x, v.y)

                                change.consume()

                                return@awaitEachGesture
                            }

                            tracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )

                            val dt =
                                (change.uptimeMillis - lastT) / 1000f

                            lastT = change.uptimeMillis

                            state.onDrag(
                                change.position - change.previousPosition,
                                change.position - center,
                                dt
                            )

                            change.consume()
                        }

                        // El puntero desapareció sin levantarse.
                        state.onDragEnd(0f, 0f)
                    }
                }
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {

                        translationX = state.translationX()
                        translationY = state.offsetY

                        rotationX = state.tiltX
                        rotationY = state.tiltY

                        cameraDistance = 14f * density
                    }
        ) {

            // 1. Cuerpo translúcido.
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawDiscBody()
            }

            // 2. Líquido independiente de la rotación del disco.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .liquidLayer(state)
            )

            // 3. Detalles grabados (rotan con el disco). Se dibujan
            // una sola vez; girar es solo una matriz de la GPU.
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = state.spinDegrees()
                        }
            ) {
                drawDiscDetails(accent)
            }

            // 4. Reflejos de cristal.
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawDiscGloss()
            }
        }
    }
}

// ---------------------------------------------------------------
// Dibujo
// ---------------------------------------------------------------

private fun androidx.compose.ui.graphics.drawscope.DrawScope.discRadius(): Float =
    size.minDimension / 2f * VINYL_DISC_FRACTION

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiscBody() {

    val r = discRadius()

    val c = Offset(size.width / 2f, size.height / 2f)

    // Halo suave exterior: separa el disco del fondo sin una sombra
    // dura (el disco es translúcido).
    drawCircle(
        brush =
            Brush.radialGradient(
                0.96f to Color(0x30001A44),
                1.0f to Color.Transparent,
                center = c,
                radius = size.minDimension / 2f
            ),
        radius = size.minDimension / 2f,
        center = c
    )

    // Plástico cristalino.
    drawCircle(
        brush =
            Brush.radialGradient(
                0f to Color(0x5CCFEFFF),
                0.55f to Color(0x2E8CCBF5),
                0.93f to Color(0x4A9AD8FF),
                1f to Color(0x80FFFFFF),
                center = c,
                radius = r
            ),
        radius = r,
        center = c
    )

    // Cavidad interior un poco más profunda.
    drawCircle(
        color = Color(0x220A2A5A),
        radius = r * 0.93f,
        center = c
    )

    // Bordes: canto exterior, pared de la cavidad y pared del cubo.
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = r - 1.dp.toPx(),
        center = c,
        style = Stroke(2.dp.toPx())
    )

    drawCircle(
        color = Color(0xFF9FE3FF).copy(alpha = 0.40f),
        radius = r * 0.93f,
        center = c,
        style = Stroke(1.dp.toPx())
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = r * 0.31f,
        center = c,
        style = Stroke(1.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiscDetails(
    accent: Color
) {

    val r = discRadius()

    val c = Offset(size.width / 2f, size.height / 2f)

    // Surcos finos (también dan lectura de "disco").
    for (f in floatArrayOf(0.46f, 0.54f, 0.70f, 0.78f)) {

        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = r * f,
            center = c,
            style = Stroke(0.8.dp.toPx())
        )
    }

    // Tres sectores grabados: sin esto un disco transparente no
    // muestra que está girando.
    for (k in 0 until 3) {

        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = k * 120f + 10f,
            sweepAngle = 46f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
            size = Size(r * 1.24f, r * 1.24f),
            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Anillo de marcas técnicas en el canto.
    for (i in 0 until 72) {

        val a = i * 5.0 * PI / 180.0

        val long = i % 6 == 0

        val r1 = r * (if (long) 0.945f else 0.958f)
        val r2 = r * 0.98f

        drawLine(
            color =
                Color.White.copy(alpha = if (long) 0.55f else 0.30f),
            start =
                Offset(
                    c.x + (cos(a) * r1).toFloat(),
                    c.y + (sin(a) * r1).toFloat()
                ),
            end =
                Offset(
                    c.x + (cos(a) * r2).toFloat(),
                    c.y + (sin(a) * r2).toFloat()
                ),
            strokeWidth = if (long) 1.4.dp.toPx() else 0.9.dp.toPx()
        )
    }

    // Cubo de plástico transparente.
    drawCircle(
        brush =
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.40f),
                1f to Color.White.copy(alpha = 0.14f),
                center = c,
                radius = r * 0.30f
            ),
        radius = r * 0.30f,
        center = c
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = r * 0.20f,
        center = c,
        style = Stroke(1.dp.toPx())
    )

    // Marca de acento: permite ver la orientación del disco.
    drawCircle(
        color = accent,
        radius = 3.5.dp.toPx(),
        center = Offset(c.x + r * 0.255f, c.y)
    )

    // Agujero central.
    drawCircle(
        color = Color(0xE6071A38),
        radius = r * 0.075f,
        center = c
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = r * 0.075f,
        center = c,
        style = Stroke(1.dp.toPx())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiscGloss() {

    val r = discRadius()

    val c = Offset(size.width / 2f, size.height / 2f)

    val circle =
        Path().apply {
            addOval(Rect(c, r))
        }

    // Brillo diagonal fijo (la luz no gira con el disco).
    clipPath(circle) {

        drawRect(
            brush =
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.30f),
                    0.38f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.10f),
                    start = Offset(c.x - r, c.y - r),
                    end = Offset(c.x + r, c.y + r)
                )
        )
    }

    // Reflejos en arco: uno fuerte arriba-izquierda y uno tenue
    // abajo-derecha, como luz refractada por el borde.
    drawArc(
        color = Color.White.copy(alpha = 0.65f),
        startAngle = 195f,
        sweepAngle = 62f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.9f, c.y - r * 0.9f),
        size = Size(r * 1.8f, r * 1.8f),
        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
    )

    drawArc(
        color = Color(0xFFBFEFFF).copy(alpha = 0.35f),
        startAngle = 15f,
        sweepAngle = 40f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.9f, c.y - r * 0.9f),
        size = Size(r * 1.8f, r * 1.8f),
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Capa del líquido. Todo lo caro (la cavidad, el degradado, los
 * Path) se construye una vez por tamaño; en cada fotograma solo se
 * recalcula la línea de superficie (19 puntos) y se reutilizan los
 * mismos Path.
 */
private fun Modifier.liquidLayer(
    state: VinylState
): Modifier =
    drawWithCache {

        val r = size.minDimension / 2f * VINYL_DISC_FRACTION

        val c = Offset(size.width / 2f, size.height / 2f)

        // Cavidad anular: entre el cubo y la pared exterior.
        val cavity =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addOval(Rect(c, r * 0.93f))
                addOval(Rect(c, r * 0.31f))
            }

        val liquid =
            Brush.verticalGradient(
                0f to Color(0xE63AA9F2),
                0.45f to Color(0xF01B6BC4),
                1f to Color(0xFA0A2C70),
                startY = c.y - r,
                endY = c.y + r
            )

        val body = Path()
        val edge = Path()

        val steps = 18
        val x0 = c.x - r * 1.15f
        val span = r * 2.3f

        val glowWidth = 9.dp.toPx()
        val edgeWidth = 1.6.dp.toPx()

        onDrawBehind {

            val amp = state.waveAmp
            val phase = state.wavePhase

            // Superficie a "level" de la altura, medida desde abajo.
            val surface = c.y + r * (1f - 2f * state.level)

            body.reset()
            edge.reset()

            for (i in 0..steps) {

                val x = x0 + span * i / steps

                val u = (x - c.x) / r

                val y =
                    surface +
                        amp * (
                            sin(u * 3.2f + phase) +
                                0.5f * sin(u * 6.1f - phase * 1.3f)
                            )

                if (i == 0) {
                    body.moveTo(x, y)
                    edge.moveTo(x, y)
                } else {
                    body.lineTo(x, y)
                    edge.lineTo(x, y)
                }
            }

            body.lineTo(x0 + span, c.y + r * 1.2f)
            body.lineTo(x0, c.y + r * 1.2f)
            body.close()

            clipPath(cavity) {

                // La inclinación se aplica al marco del líquido, no
                // al disco: con el disco girando, la superficie sigue
                // horizontal respecto a la pantalla.
                rotate(
                    degrees =
                        state.surfaceTilt * 180f / PI.toFloat(),
                    pivot = c
                ) {

                    drawPath(body, liquid)

                    drawPath(
                        edge,
                        Color(0x40BFF0FF),
                        style = Stroke(glowWidth)
                    )

                    drawPath(
                        edge,
                        Color.White.copy(alpha = 0.75f),
                        style = Stroke(edgeWidth)
                    )
                }
            }
        }
    }
