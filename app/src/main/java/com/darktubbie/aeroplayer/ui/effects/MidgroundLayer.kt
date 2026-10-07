package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.ui.theme.AeroColors
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
 *
 * Fase 4 de Dynamic Aero (0.5.0): los colores de cuerpo/borde y de
 * los degradados radiales ya no son fijos — salen de [AeroColors]
 * ([AeroColors.AmbientBubbleTint], [AeroColors.AmbientGlowPrimary],
 * [AeroColors.AmbientGlowSecondary]), así que Aero Dark tiñe las
 * burbujas a celeste-hielo y cian/azul en vez de blanco. Los
 * reflejos/highlights internos se mantienen en blanco puro a
 * propósito en ambos temas (son un brillo especular, no el color
 * del cuerpo de la burbuja).
 *
 * Fase 5 de la Experiencia de Artwork (0.5.0) — integración con
 * artwork: los dos degradados radiales grandes se matizan (35%) con
 * el color dominante de la portada de la canción actual (ver
 * [AmbientArtworkColor]), independientemente del nivel de
 * intensidad — es un color, no movimiento. Sin canción/sin artwork,
 * quedan exactamente en el color fijo del tema.
 *
 * Fase 9 (0.6.0) — Album Art as Environment: la integración pasa de
 * un color promedio a una paleta de dos colores ([ArtworkPalette]),
 * con fundido entre canciones, un lavado vertical de color y las
 * burbujas tintadas. Ver el bloque comentado más abajo.
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

    val ambientPlayback =
        LocalAmbientPlayback.current

    val isMusicPlaying =
        ambientPlayback.isPlaying

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

    /*
     * Album Art as Environment (Fase 9, 0.6.0). Evolución de la
     * integración de la Fase 5 (que solo matizaba 2 glows con UN
     * color promedio): ahora la portada actual aporta una paleta de
     * dos colores ([ArtworkPalette]) que ilumina tres cosas del
     * ambiente — los dos glows grandes (uno con cada color), un
     * lavado vertical de color sobre todo el fondo, y el tinte de
     * las burbujas.
     *
     * Sigue siendo COLOR, no movimiento: aplica igual con cualquier
     * [AmbientIntensity] (incluido OFF/STATIC) y con reduce-motion.
     * La intensidad solo gobierna lo que ya gobernaba (el balanceo
     * de las burbujas) y, nuevo, cuánto "respira" el lavado — con la
     * misma fase compartida, sin una animación aparte.
     *
     * La paleta se resuelve de forma perezosa al cambiar de canción
     * (no en cada frame) y NO se borra mientras resuelve la
     * siguiente: así no hay un parpadeo al tema fijo entre canciones.
     * Sin canción/sin artwork, vuelve (con fundido) al color fijo del
     * tema, igual que antes de esta fase.
     */
    var palette by
        remember {
            mutableStateOf<ArtworkPalette?>(null)
        }

    var lastPalette by
        remember {
            mutableStateOf<ArtworkPalette?>(null)
        }

    LaunchedEffect(
        ambientPlayback.trackPath,
        ambientPlayback.trackArtist,
        ambientPlayback.trackAlbum
    ) {

        val path =
            ambientPlayback.trackPath

        val resolved =
            if (path.isNullOrBlank()) {
                null
            } else {
                AmbientArtworkColor.getPalette(
                    context = context,
                    path = path,
                    artist = ambientPlayback.trackArtist,
                    album = ambientPlayback.trackAlbum
                )
            }

        palette = resolved

        if (resolved != null) {
            lastPalette = resolved
        }
    }

    // Fundido entre canciones: los colores y la fuerza se animan en
    // vez de saltar. Al quedarse sin portada se conserva el último
    // color mientras la fuerza baja a 0 (así se apaga en su color).
    val shownPalette =
        palette ?: lastPalette

    val envStrength by
        animateFloatAsState(
            targetValue = if (palette != null) 1f else 0f,
            animationSpec = tween(900),
            label = "environment-strength"
        )

    val envPrimary by
        animateColorAsState(
            targetValue =
                shownPalette?.primary
                    ?: AeroColors.AmbientGlowPrimary.copy(alpha = 1f),
            animationSpec = tween(900),
            label = "environment-primary"
        )

    val envSecondary by
        animateColorAsState(
            targetValue =
                shownPalette?.secondary
                    ?: AeroColors.AmbientGlowSecondary.copy(alpha = 1f),
            animationSpec = tween(900),
            label = "environment-secondary"
        )

    /*
     * Mezcla sobre el color fijo del tema — la identidad Aero sigue
     * siendo la base, el artwork la matiza. Se preserva el alpha
     * original: el artwork nunca vuelve nada más opaco ni más
     * transparente. Fase 9: 55% en los glows (era 35%).
     */
    fun tinted(
        base: Color,
        artwork: Color,
        amount: Float
    ): Color =
        lerp(
            base.copy(alpha = 1f),
            artwork.copy(alpha = 1f),
            amount * envStrength
        ).copy(alpha = base.alpha)

    val glowPrimary =
        tinted(AeroColors.AmbientGlowPrimary, envPrimary, 0.55f)

    val glowSecondary =
        tinted(AeroColors.AmbientGlowSecondary, envSecondary, 0.55f)

    val bubbleTint =
        tinted(AeroColors.AmbientBubbleTint, envPrimary, 0.30f)

    // "Respiración" del lavado: solo si hay animación activa, con
    // la misma fase que mueve las burbujas.
    val breathAmount: Float =
        when (effectiveIntensity) {
            AmbientIntensity.LOW -> 0.06f
            AmbientIntensity.NORMAL -> 0.10f
            AmbientIntensity.HIGH -> 0.16f
            else -> 0f
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

        // Lavado de color de la portada: arriba un color, abajo el
        // otro, transparente en el medio para no velar la interfaz.
        // Alpha y respiración se leen dentro de graphicsLayer, así
        // que no recomponen nada por fotograma.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {

                        val wave =
                            phase?.value?.let {
                                (sin(it) + 1f) / 2f
                            } ?: 1f

                        alpha =
                            (1f - breathAmount) +
                                breathAmount * wave
                    }
                    .background(
                        Brush.verticalGradient(
                            0f to
                                envPrimary.copy(
                                    alpha = 0.20f * envStrength
                                ),
                            0.55f to Color.Transparent,
                            1f to
                                envSecondary.copy(
                                    alpha = 0.16f * envStrength
                                )
                        )
                    )
        )

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
                                    glowPrimary,
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
                                    glowSecondary,
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
                        bubbleTint.copy(
                            alpha = 0.18f
                        )
                    )
                    .border(
                        2.dp,
                        bubbleTint.copy(
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
                        bubbleTint.copy(
                            alpha = 0.22f
                        )
                    )
                    .border(
                        1.dp,
                        bubbleTint.copy(
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
                        bubbleTint.copy(
                            alpha = 0.16f
                        )
                    )
                    .border(
                        2.dp,
                        bubbleTint.copy(
                            alpha = 0.35f
                        ),
                        CircleShape
                    )
        )
    }
}
