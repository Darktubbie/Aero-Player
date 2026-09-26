package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.darktubbie.aeroplayer.ape.ApeEvent
import com.darktubbie.aeroplayer.ape.ApeFile
import com.darktubbie.aeroplayer.ape.ApeRepository
import com.darktubbie.aeroplayer.data.EffectAssetRepository
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Un evento ambiental individual en curso. Un grupo (burbujas,
 * medusas, nubes) o cardumen (peces) genera varias instancias de
 * esta clase a la vez, cada una con su propia animación
 * independiente.
 *
 * @param lane posición fija en el eje que el evento NO recorre:
 * fracción vertical de pantalla (0f arriba, 1f abajo) para eventos
 * horizontales ([AmbientDirection.LEFT_TO_RIGHT] /
 * [AmbientDirection.RIGHT_TO_LEFT]), o fracción horizontal para
 * eventos verticales ([AmbientDirection.BOTTOM_TO_TOP]).
 * @param wanderAmplitudePx qué tan pronunciado es el balanceo
 * perpendicular a su avance (0f = sin balanceo, línea recta). Los
 * peces usan un valor notorio; las burbujas uno muy sutil.
 * @param wanderFrequency cuántos ciclos de balanceo hace durante su
 * recorrido.
 * @param wanderSeed desfase de fase del balanceo, para que varios
 * miembros del mismo grupo no se muevan todos exactamente igual.
 */
private data class ActiveSprite(
    val id: Long,
    val type: AmbientEventType,
    val direction: AmbientDirection,
    val lane: Float,
    val progress: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    val wanderAmplitudePx: Float = 0f,
    val wanderFrequency: Float = 0f,
    val wanderSeed: Float = 0f
)

private fun newSpriteId(): Long =
    Random.nextLong()

/**
 * Capa 4 de la jerarquía visual (Fase 6 del plan de evolución
 * visual, versión extendida): eventos ambientales ocasionales.
 *
 * Pueden coexistir varios eventos independientes, con variedad de
 * tipo, dirección y temporización, hasta un máximo de
 * [MAX_CONCURRENT_SPRITES] simultáneos para no saturar la pantalla
 * ni el rendimiento en gama media/baja:
 *
 * - Burbujas ([AmbientEventType.BUBBLE_FRONT]): aparecen en grupos
 *   de 3 a 5, repartidas por la pantalla (no muy juntas), todas con
 *   la MISMA dirección dentro de un mismo grupo (las 3
 *   [AmbientDirection] posibles, pero nunca mezcladas entre sí) y
 *   un balanceo perpendicular a su avance MUY sutil (serpenteo
 *   suave, no tan marcado como el de los peces).
 * - Peces ([AmbientEventType.FISH]): cardumen de 3 a 5, muy juntos,
 *   serpenteando de forma notoria sobre su línea de avance. Solo se
 *   mueven en horizontal. Usan el emoji 🐠.
 * - Medusas ([AmbientEventType.JELLYFISH], emoji 🪼): grupos de 3 a
 *   5, repartidas (no muy juntas), siempre suben de abajo hacia
 *   arriba.
 * - Nubes ([AmbientEventType.CLOUD], emoji ☁️): grupos de 3 a 5,
 *   repartidas (no muy juntas), se mueven en horizontal
 *   (izquierda-derecha o viceversa) igual que las burbujas, pero
 *   más lento.
 * - Hojas ([AmbientEventType.LEAF], emoji 🍃, Fase 6 de ".aero"
 *   0.5.0): grupos de 2 a 4, repartidas, cayendo de arriba hacia
 *   abajo con un balanceo lateral moderado.
 *
 * Cada [ActiveSprite] es una animación independiente
 * (`Animatable` + coroutine propia); la posición se lee en fase de
 * layout vía `Modifier.offset { }`, igual que en [MidgroundLayer],
 * para no forzar recomposición en cada frame.
 *
 * Fase 7 (Aero Player Effects): si la canción actual tiene un
 * archivo `.ape` con el mismo nombre base (ver [ApeRepository]),
 * sus eventos programados REEMPLAZAN al generador aleatorio de más
 * arriba para esa canción — tiempos, efecto y duración exactos en
 * vez de aleatorios. Sin `.ape`, se usa el generador aleatorio de
 * siempre. En ambos casos, esta capa solo OBSERVA la posición de
 * reproducción (nunca la modifica) y solo hay efectos mientras
 * [AmbientPlaybackInfo.isPlaying] es true — si la música se pausa o
 * no hay nada reproduciéndose, no hay ningún efecto en pantalla.
 */
@Composable
fun ForegroundLayer(
    modifier: Modifier = Modifier,
    protectedZones: ProtectedZones = ProtectedZones()
) {

    val context = LocalContext.current

    val reduceMotion =
        remember {
            isSystemReduceMotionEnabled(context)
        }

    if (reduceMotion) {

        // Misma preferencia de accesibilidad que la Fase 5: sin
        // eventos ambientales ocasionales tampoco.
        return
    }

    val playback = LocalAmbientPlayback.current

    if (!playback.isPlaying) {

        // Fase 7: sin música sonando, no hay ningún efecto — ni los
        // aleatorios de siempre ni los de un .ape.
        return
    }

    val sprites =
        remember {
            mutableStateListOf<ActiveSprite>()
        }

    // Se resuelve una vez por canción, y también cada vez que
    // apeVersion cambia (el editor acaba de guardar un .ape nuevo o
    // modificado para la canción que sigue sonando — sin esta
    // segunda clave, esta relectura solo pasaría al cambiar de
    // canción). Nunca controla Media3: solo lee el archivo .ape, si
    // existe.
    var apeFile by
        remember(playback.trackPath, playback.apeVersion) {
            mutableStateOf<ApeFile?>(null)
        }

    var apeResolved by
        remember(playback.trackPath, playback.apeVersion) {
            mutableStateOf(false)
        }

    LaunchedEffect(playback.trackPath, playback.apeVersion) {

        apeResolved = false

        apeFile =
            playback.trackPath?.let {
                ApeRepository.get(it)
            }

        apeResolved = true
    }

    if (!apeResolved) {

        // Evita arrancar el generador aleatorio durante el
        // instante en que todavía no sabemos si esta canción tiene
        // un .ape propio.
        return
    }

    val scheduledEffects = apeFile?.effects

    if (scheduledEffects != null && scheduledEffects.isNotEmpty()) {

        // Esta canción tiene su propio .ape: se reproducen sus
        // eventos programados en vez del generador aleatorio.
        LaunchedEffect(
            playback.trackPath,
            playback.apeVersion,
            scheduledEffects
        ) {

            runApeEvents(
                events = scheduledEffects,
                getPositionMs = playback.getPositionMs,
                protectedZones = protectedZones,
                sprites = sprites
            )
        }

    } else {

        // Sin .ape para esta canción: generador ambiental aleatorio
        // de siempre.
        LaunchedEffect(playback.trackPath, playback.apeVersion) {

            while (isActive) {

                delay(
                    Random.nextLong(25_000L, 70_000L)
                )

                if (sprites.size >= MAX_CONCURRENT_SPRITES) {
                    continue
                }

                when (Random.nextFloat()) {

                    in 0f..0.24f ->
                        spawnBubbleGroup(protectedZones, sprites)

                    in 0.24f..0.44f ->
                        spawnFishSchool(protectedZones, sprites)

                    in 0.44f..0.62f ->
                        spawnJellyfishGroup(sprites)

                    in 0.62f..0.82f ->
                        spawnCloudGroup(protectedZones, sprites)

                    else ->
                        spawnLeafGroup(sprites)
                }
            }
        }
    }

    if (sprites.isEmpty()) {
        return
    }

    val density = LocalDensity.current

    val effectAssetRepository =
        remember(context) {
            EffectAssetRepository(context)
        }

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize()
    ) {

        val widthPx =
            with(density) {
                maxWidth.toPx()
            }

        val heightPx =
            with(density) {
                maxHeight.toPx()
            }

        val margin = 140f

        for (sprite in sprites) {

            val offsetModifier =
                spriteOffsetModifier(
                    sprite = sprite,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    margin = margin
                )

            when (sprite.type) {

                AmbientEventType.BUBBLE_FRONT -> {

                    CustomOrBuiltInEffect(
                        assetRepository = effectAssetRepository,
                        type = sprite.type,
                        modifier = offsetModifier
                    ) {

                        BubbleFrontShape(
                            modifier = offsetModifier
                        )
                    }
                }

                AmbientEventType.FISH -> {

                    CustomOrBuiltInEffect(
                        assetRepository = effectAssetRepository,
                        type = sprite.type,
                        modifier = offsetModifier
                    ) {

                        FishEmoji(
                            facingRight =
                                sprite.direction ==
                                AmbientDirection.LEFT_TO_RIGHT,

                            modifier = offsetModifier
                        )
                    }
                }

                AmbientEventType.JELLYFISH -> {

                    CustomOrBuiltInEffect(
                        assetRepository = effectAssetRepository,
                        type = sprite.type,
                        modifier = offsetModifier
                    ) {

                        JellyfishEmoji(
                            modifier = offsetModifier
                        )
                    }
                }

                AmbientEventType.CLOUD -> {

                    CustomOrBuiltInEffect(
                        assetRepository = effectAssetRepository,
                        type = sprite.type,
                        modifier = offsetModifier
                    ) {

                        CloudEmoji(
                            modifier = offsetModifier
                        )
                    }
                }

                AmbientEventType.LEAF -> {

                    CustomOrBuiltInEffect(
                        assetRepository = effectAssetRepository,
                        type = sprite.type,
                        modifier = offsetModifier
                    ) {

                        LeafEmoji(
                            modifier = offsetModifier
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_CONCURRENT_SPRITES = 16

/**
 * Construye el `Modifier.offset { }` de un sprite según su
 * dirección, sumando el balanceo perpendicular a su avance
 * ([ActiveSprite.wanderAmplitudePx]) — notorio en los peces, muy
 * sutil en las burbujas, inexistente en medusas/nubes.
 */
private fun spriteOffsetModifier(
    sprite: ActiveSprite,
    widthPx: Float,
    heightPx: Float,
    margin: Float
): Modifier =

    Modifier.offset {

        val p = sprite.progress.value

        val wander =
            if (sprite.wanderAmplitudePx == 0f) {
                0f
            } else {
                sin(
                    p *
                    sprite.wanderFrequency *
                    2f *
                    PI.toFloat() +
                    sprite.wanderSeed
                ) * sprite.wanderAmplitudePx
            }

        when (sprite.direction) {

            AmbientDirection.LEFT_TO_RIGHT,
            AmbientDirection.RIGHT_TO_LEFT -> {

                val leftToRight =
                    sprite.direction ==
                    AmbientDirection.LEFT_TO_RIGHT

                val startX =
                    if (leftToRight) -margin else widthPx + margin

                val endX =
                    if (leftToRight) widthPx + margin else -margin

                val x = startX + (endX - startX) * p

                // El balanceo es perpendicular al avance: en
                // movimiento horizontal, se aplica en Y.
                val y = heightPx * sprite.lane + wander

                IntOffset(
                    x = x.roundToInt(),
                    y = y.roundToInt()
                )
            }

            AmbientDirection.BOTTOM_TO_TOP -> {

                val startY = heightPx + margin
                val endY = -margin

                val y = startY + (endY - startY) * p

                // En movimiento vertical, el balanceo se aplica en
                // X (por ahora ningún tipo lo usa en esta
                // dirección, pero queda listo si hiciera falta).
                val x = widthPx * sprite.lane + wander

                IntOffset(
                    x = x.roundToInt(),
                    y = y.roundToInt()
                )
            }

            AmbientDirection.TOP_TO_BOTTOM -> {

                // Fase 6 de ".aero" (0.5.0), agregada para
                // AmbientEventType.LEAF: la inversa exacta de
                // BOTTOM_TO_TOP — de arriba hacia abajo en vez de
                // abajo hacia arriba, mismo balanceo lateral en X.
                val startY = -margin
                val endY = heightPx + margin

                val y = startY + (endY - startY) * p

                val x = widthPx * sprite.lane + wander

                IntOffset(
                    x = x.roundToInt(),
                    y = y.roundToInt()
                )
            }
        }
    }

/**
 * Sondea la posición de reproducción y dispara los eventos de un
 * `.ape` en el momento exacto que indican, en vez de generarlos al
 * azar. Regla no negociable del brief: APE nunca controla Media3 —
 * [getPositionMs] es una lectura, nunca se llama a nada que cambie
 * la reproducción.
 *
 * Si la posición retrocede de forma notoria (el usuario retrocedió
 * manualmente, o la canción volvió a empezar por repeat), los
 * eventos ya disparados se rearman — así un repeat o un seek hacia
 * atrás no deja el resto de la reproducción sin efectos.
 */
private suspend fun CoroutineScope.runApeEvents(
    events: List<ApeEvent>,
    getPositionMs: () -> Long,
    protectedZones: ProtectedZones,
    sprites: MutableList<ActiveSprite>
) {

    val sortedEvents =
        events.sortedBy { it.time }

    val fired =
        BooleanArray(sortedEvents.size)

    var lastPosition = -1L

    while (isActive) {

        val position = getPositionMs()

        if (position < lastPosition - 1000L) {
            fired.fill(false)
        }

        lastPosition = position

        for (i in sortedEvents.indices) {

            val event = sortedEvents[i]

            if (!fired[i] && position >= event.time) {

                fired[i] = true

                if (sprites.size < MAX_CONCURRENT_SPRITES) {

                    spawnApeEffect(
                        event = event,
                        protectedZones = protectedZones,
                        sprites = sprites
                    )
                }
            }
        }

        delay(200L)
    }
}

/**
 * Dispara los eventos programados de un `.ape` reutilizando los
 * mismos generadores de grupo que el modo aleatorio (burbujas,
 * medusas y hojas en grupo, peces en cardumen), en vez de una
 * única instancia solitaria — la única diferencia es que la
 * duración de cada miembro es la indicada en el `.ape` en vez de
 * aleatoria.
 *
 * Fase 6 de ".aero" (0.5.0) — mejor organización: el parseo del
 * nombre de texto ([ApeEvent.effect]) a [AmbientEventType] ya no es
 * un `when` de Strings acá adentro, sale de
 * [ambientEventTypeFromKey] (un único lugar, compartido con el
 * editor). Un nombre no reconocido sigue ignorándose sin romper el
 * resto del `.ape`, igual que antes.
 */
private fun CoroutineScope.spawnApeEffect(
    event: ApeEvent,
    protectedZones: ProtectedZones,
    sprites: MutableList<ActiveSprite>
) {

    val fixedDurationMs =
        event.duration
            .takeIf { it in 1..60_000L }
            ?.toInt()

    when (ambientEventTypeFromKey(event.effect)) {

        AmbientEventType.BUBBLE_FRONT ->
            spawnBubbleGroup(
                protectedZones,
                sprites,
                fixedDurationMs
            )

        AmbientEventType.FISH ->
            spawnFishSchool(
                protectedZones,
                sprites,
                fixedDurationMs
            )

        AmbientEventType.JELLYFISH ->
            spawnJellyfishGroup(
                sprites,
                fixedDurationMs
            )

        AmbientEventType.CLOUD ->
            spawnCloudGroup(
                protectedZones,
                sprites,
                fixedDurationMs
            )

        AmbientEventType.LEAF ->
            spawnLeafGroup(
                sprites,
                fixedDurationMs
            )

        // Nombre de efecto no reconocido: se ignora este evento y
        // se sigue con el resto del .ape.
        null -> Unit
    }
}

/**
 * Burbujas: grupo de 3 a 5 que comparten la misma dirección (todas
 * izquierda-derecha, todas derecha-izquierda, o todas abajo-arriba
 * — nunca mezcladas dentro del mismo grupo), pero cada una con su
 * propio carril/posición independiente (para que no se vean
 * agrupadas/pegadas) y un balanceo perpendicular muy sutil.
 */
private fun CoroutineScope.spawnBubbleGroup(
    protectedZones: ProtectedZones,
    sprites: MutableList<ActiveSprite>,
    fixedDurationMs: Int? = null
) {

    // Fase 6 de ".aero" (0.5.0): AmbientDirection.entries ya no
    // alcanza para esto — ahora también incluye TOP_TO_BOTTOM
    // (agregada para AmbientEventType.LEAF), y una burbuja cayendo
    // no tiene sentido físico ni la lane de más abajo la contempla.
    // Se listan a mano las 3 direcciones que las burbujas siempre
    // soportaron, exactamente el mismo conjunto (y las mismas
    // probabilidades) que daba AmbientDirection.entries.random()
    // antes de esta fase.
    val direction =
        listOf(
            AmbientDirection.LEFT_TO_RIGHT,
            AmbientDirection.RIGHT_TO_LEFT,
            AmbientDirection.BOTTOM_TO_TOP
        ).random()

    val count = Random.nextInt(3, 6)

    repeat(count) { index ->

        launch {

            delay(
                index * Random.nextLong(500L, 1100L)
            )

            val lane =
                if (direction == AmbientDirection.BOTTOM_TO_TOP) {
                    Random.nextFloat() * 0.8f + 0.1f
                } else {
                    Random.nextFloat() *
                    (
                        protectedZones.allowedRange.endInclusive -
                        protectedZones.allowedRange.start
                    ) +
                    protectedZones.allowedRange.start
                }

            val progress = Animatable(0f)

            val sprite =
                ActiveSprite(
                    id = newSpriteId(),
                    type = AmbientEventType.BUBBLE_FRONT,
                    direction = direction,
                    lane = lane,
                    progress = progress,

                    // Serpenteo MUY sutil: mucho menos amplitud y
                    // menos ciclos que el de los peces.
                    wanderAmplitudePx =
                        Random.nextFloat() * 6f + 6f,

                    wanderFrequency =
                        Random.nextFloat() * 0.6f + 0.5f,

                    wanderSeed =
                        Random.nextFloat() * (2f * PI.toFloat())
                )

            sprites.add(sprite)

            progress.animateTo(
                targetValue = 1f,

                animationSpec =
                    tween(
                        durationMillis =
                            fixedDurationMs
                                ?: Random.nextInt(4500, 7500),
                        easing = LinearEasing
                    )
            )

            sprites.remove(sprite)
        }
    }
}

/**
 * Peces: cardumen de 3 a 5, muy juntos (misma dirección, carriles
 * muy próximos entre sí) y con un serpenteo notorio.
 */
private fun CoroutineScope.spawnFishSchool(
    protectedZones: ProtectedZones,
    sprites: MutableList<ActiveSprite>,
    fixedDurationMs: Int? = null
) {

    val direction =
        if (Random.nextBoolean()) {
            AmbientDirection.LEFT_TO_RIGHT
        } else {
            AmbientDirection.RIGHT_TO_LEFT
        }

    val baseLane =
        Random.nextFloat() *
        (
            protectedZones.allowedRange.endInclusive -
            protectedZones.allowedRange.start
        ) +
        protectedZones.allowedRange.start

    val count = Random.nextInt(3, 6)

    val wanderFrequency =
        Random.nextInt(2, 4).toFloat()

    repeat(count) { index ->

        launch {

            delay(
                index * Random.nextLong(120L, 260L)
            )

            val laneJitter =
                (index - (count - 1) / 2f) * 0.018f

            val lane =
                (baseLane + laneJitter).coerceIn(
                    protectedZones.allowedRange
                )

            val progress = Animatable(0f)

            val sprite =
                ActiveSprite(
                    id = newSpriteId(),
                    type = AmbientEventType.FISH,
                    direction = direction,
                    lane = lane,
                    progress = progress,
                    wanderAmplitudePx = 46f,
                    wanderFrequency = wanderFrequency,

                    wanderSeed =
                        Random.nextFloat() * (2f * PI.toFloat())
                )

            sprites.add(sprite)

            progress.animateTo(
                targetValue = 1f,

                animationSpec =
                    tween(
                        durationMillis =
                            fixedDurationMs
                                ?: Random.nextInt(9000, 13000),
                        easing = LinearEasing
                    )
            )

            sprites.remove(sprite)
        }
    }
}

/**
 * Medusas: grupo de 3 a 5, cada una con su propio carril horizontal
 * independiente (no muy juntas), siempre subiendo.
 */
private fun CoroutineScope.spawnJellyfishGroup(
    sprites: MutableList<ActiveSprite>,
    fixedDurationMs: Int? = null
) {

    val count = Random.nextInt(3, 6)

    repeat(count) { index ->

        launch {

            delay(
                index * Random.nextLong(600L, 1300L)
            )

            val progress = Animatable(0f)

            val sprite =
                ActiveSprite(
                    id = newSpriteId(),
                    type = AmbientEventType.JELLYFISH,
                    direction = AmbientDirection.BOTTOM_TO_TOP,
                    lane = Random.nextFloat() * 0.8f + 0.1f,
                    progress = progress
                )

            sprites.add(sprite)

            progress.animateTo(
                targetValue = 1f,

                animationSpec =
                    tween(
                        durationMillis =
                            fixedDurationMs
                                ?: Random.nextInt(9000, 13000),
                        easing = LinearEasing
                    )
            )

            sprites.remove(sprite)
        }
    }
}

/**
 * Nubes: grupo de 3 a 5, cada una con su propio carril vertical
 * independiente (no muy juntas). Se mueven en horizontal como las
 * burbujas, pero más lento.
 */
private fun CoroutineScope.spawnCloudGroup(
    protectedZones: ProtectedZones,
    sprites: MutableList<ActiveSprite>,
    fixedDurationMs: Int? = null
) {

    val count = Random.nextInt(3, 6)

    repeat(count) { index ->

        launch {

            delay(
                index * Random.nextLong(700L, 1500L)
            )

            val direction =
                if (Random.nextBoolean()) {
                    AmbientDirection.LEFT_TO_RIGHT
                } else {
                    AmbientDirection.RIGHT_TO_LEFT
                }

            val allowedSpan =
                protectedZones.allowedRange.endInclusive -
                protectedZones.allowedRange.start

            // Franja alta dentro de lo permitido: las nubes deben
            // sentirse arriba, no mezcladas con burbujas/peces en
            // el medio, pero sin invadir la zona protegida del
            // encabezado.
            val lane =
                protectedZones.allowedRange.start +
                Random.nextFloat() * allowedSpan * 0.28f

            val progress = Animatable(0f)

            val sprite =
                ActiveSprite(
                    id = newSpriteId(),
                    type = AmbientEventType.CLOUD,
                    direction = direction,
                    lane = lane,
                    progress = progress
                )

            sprites.add(sprite)

            progress.animateTo(
                targetValue = 1f,

                animationSpec =
                    tween(
                        // Más lentas que las burbujas (4500-7500ms).
                        durationMillis =
                            fixedDurationMs
                                ?: Random.nextInt(13000, 19000),
                        easing = LinearEasing
                    )
            )

            sprites.remove(sprite)
        }
    }
}

/**
 * Hojas ([AmbientEventType.LEAF], Fase 6 de ".aero" 0.5.0): grupo de
 * 2 a 4 (más disperso que el resto — una lluvia de hojas constante
 * se sentiría demasiado cargada), cada una con su propio carril
 * horizontal independiente, cayendo de arriba hacia abajo
 * ([AmbientDirection.TOP_TO_BOTTOM]) con un balanceo lateral
 * moderado: más marcado que el de las burbujas (que es casi
 * imperceptible a propósito), pero bastante menos que el serpenteo
 * notorio de los peces — la idea es que se sienta flotando en el
 * aire, no cayendo en línea recta ni serpenteando como un pez.
 */
private fun CoroutineScope.spawnLeafGroup(
    sprites: MutableList<ActiveSprite>,
    fixedDurationMs: Int? = null
) {

    val count = Random.nextInt(2, 5)

    repeat(count) { index ->

        launch {

            delay(
                index * Random.nextLong(700L, 1600L)
            )

            val progress = Animatable(0f)

            val sprite =
                ActiveSprite(
                    id = newSpriteId(),
                    type = AmbientEventType.LEAF,
                    direction = AmbientDirection.TOP_TO_BOTTOM,
                    lane = Random.nextFloat() * 0.8f + 0.1f,
                    progress = progress,

                    wanderAmplitudePx =
                        Random.nextFloat() * 18f + 18f,

                    wanderFrequency =
                        Random.nextFloat() * 1.2f + 1.2f,

                    wanderSeed =
                        Random.nextFloat() * (2f * PI.toFloat())
                )

            sprites.add(sprite)

            progress.animateTo(
                targetValue = 1f,

                animationSpec =
                    tween(
                        durationMillis =
                            fixedDurationMs
                                ?: Random.nextInt(10000, 15000),
                        easing = LinearEasing
                    )
            )

            sprites.remove(sprite)
        }
    }
}
