package com.darktubbie.aeroplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.darktubbie.aeroplayer.R

/**
 * Aero Rest Mode (Fase 7, 0.6.0): un overlay opcional (Ajustes,
 * apagado por defecto) para cuando Aero Player queda abierto sin
 * tocarse — oscurece todo y deja visible, muy discreto, solo el
 * ícono de la app, con una respiración lenta para que no se sienta
 * una pantalla "congelada"/apagada de verdad.
 *
 * No toca reproducción, orientación ni ciclo de vida: es puramente
 * visual, se dibuja arriba de todo lo demás (último hijo del Box
 * raíz en [com.darktubbie.aeroplayer.MainActivity]) y la música
 * sigue sonando exactamente igual debajo. Cualquier toque, en
 * cualquier parte, sale — ese toque se consume acá (no debe
 * "pasar" y además activar el botón que haya debajo, igual que al
 * despertar la pantalla de bloqueo del sistema).
 */
@Composable
fun AeroRestModeOverlay(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {

    val transition = rememberInfiniteTransition(
        label = "aeroRestModeBreathing"
    )

    val iconAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),

        label = "aeroRestModeIconAlpha"
    )

    val exitDescription = stringResource(R.string.rest_mode_cd_exit)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .semantics {
                    contentDescription = exitDescription
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onExit() }
                    )
                },

        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(R.drawable.ic_rest_mode_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                Color.White.copy(alpha = iconAlpha)
            )
        )
    }
}
