package com.darktubbie.aeroplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Fondo decorativo Aero: degradado vertical + burbujas
 * translúcidas, igual que antes vivía inline dentro de
 * AeroPlayer().
 *
 * Extraído como Composable propio (Fase 4 del plan / mejora E4
 * del informe): como no depende de ningún estado (tracks,
 * isScanning...), Compose puede saltárselo en las recomposiciones
 * que sí disparan esos estados, en vez de volver a evaluar las
 * burbujas en cada una.
 */
@Composable
fun AeroBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            AeroColors.BackgroundGradient
                    )
                )
    ) {

        Box(
            modifier =
                Modifier
                    .size(320.dp)
                    .offset(
                        x = (-80).dp,
                        y = 40.dp
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
                    .offset(
                        x = 170.dp,
                        y = 120.dp
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
                    .offset(
                        x = 285.dp,
                        y = 80.dp
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
                    .offset(
                        x = 28.dp,
                        y = 150.dp
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
                    .offset(
                        x = 250.dp,
                        y = 430.dp
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

        content()
    }
}
