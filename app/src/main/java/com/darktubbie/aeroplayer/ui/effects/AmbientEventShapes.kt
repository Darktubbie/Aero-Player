package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Silueta simple de una burbuja en primer plano, para
 * [AmbientEventType.BUBBLE_FRONT]. Reutiliza el mismo lenguaje
 * visual (círculo translúcido con reflejo) que las burbujas de
 * [MidgroundLayer], sin ningún asset externo — todo dibujado con
 * Canvas.
 */
@Composable
fun BubbleFrontShape(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier =
            modifier.size(64.dp)
    ) {

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color(0x8887E8FF)
                        )
                )
        )

        drawCircle(
            color =
                Color.White.copy(
                    alpha = 0.75f
                ),

            radius = size.minDimension * 0.14f,

            center =
                Offset(
                    x = size.width * 0.34f,
                    y = size.height * 0.32f
                )
        )
    }
}

/**
 * Estilo de texto compartido para los emojis de eventos ambientales
 * (Fase 6 extendida): una sombra suave para que se distingan sobre
 * cualquier fondo/imagen, ya que el fondo estático (Fase 4) puede
 * tener cualquier color de base.
 */
private val ambientEmojiStyle =
    TextStyle(
        fontSize = 34.sp,

        shadow =
            Shadow(
                color = Color.Black.copy(alpha = 0.35f),
                offset = Offset(0f, 2f),
                blurRadius = 6f
            )
    )

/**
 * Un pez del cardumen, para [AmbientEventType.FISH]. Usa el emoji
 * 🐠 en vez de una silueta dibujada — más simple y sin necesidad de
 * ningún asset propio.
 *
 * @param facingRight si es false, se espeja horizontalmente (el
 * glyph del emoji suele mirar a la izquierda por defecto en la
 * mayoría de fuentes, pero esto varía según el dispositivo/fuente
 * de emojis instalada).
 */
@Composable
fun FishEmoji(
    facingRight: Boolean,
    modifier: Modifier = Modifier
) {

    Text(
        text = "🐠",
        style = ambientEmojiStyle,

        modifier =
            modifier.scale(
                scaleX = if (facingRight) -1f else 1f,
                scaleY = 1f
            )
    )
}

/**
 * Una medusa, para [AmbientEventType.JELLYFISH]. Usa el emoji 🪼.
 * Siempre sube, así que no necesita espejado.
 */
@Composable
fun JellyfishEmoji(
    modifier: Modifier = Modifier
) {

    Text(
        text = "🪼",
        style = ambientEmojiStyle,
        modifier = modifier
    )
}

/**
 * Una nube, para [AmbientEventType.CLOUD]. Usa el emoji ☁️.
 */
@Composable
fun CloudEmoji(
    modifier: Modifier = Modifier
) {

    Text(
        text = "☁\uFE0F",

        style =
            ambientEmojiStyle.copy(
                fontSize = 40.sp
            ),

        modifier = modifier
    )
}
