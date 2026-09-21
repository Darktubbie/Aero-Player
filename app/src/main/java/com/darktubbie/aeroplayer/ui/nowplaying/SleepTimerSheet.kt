package com.darktubbie.aeroplayer.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.playback.SleepTimerMode
import com.darktubbie.aeroplayer.playback.SleepTimerState
import com.darktubbie.aeroplayer.ui.theme.AeroColors

private val MINUTE_OPTIONS =
    listOf(5, 10, 15, 30, 45, 60)

private val SONG_OPTIONS =
    listOf(1, 2, 3, 5, 10)

/**
 * Overlay para configurar o cancelar el Sleep Timer (Fase 1,
 * 0.4.x). Se abre desde el ícono en el encabezado de NowPlaying
 * (ver [NowPlayingScreen]), igual que ApeEditorScreen/FoldersScreen
 * — un Box a pantalla completa por encima del resto de la app.
 *
 * Muestra dos secciones (por tiempo / por canciones) cuando no hay
 * ningún temporizador activo, o el estado en vivo con botón de
 * cancelar cuando sí lo hay.
 */
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onSelectMinutes: (Int) -> Unit,
    onSelectSongs: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.45f)
                )
                .clickable(onClick = onDismiss),

        contentAlignment = Alignment.BottomCenter
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    // Consume los clics para que tocar dentro de la
                    // tarjeta no cierre el panel (a diferencia de
                    // tocar el fondo oscurecido) — el overload
                    // simple de clickable ya maneja su propio
                    // interactionSource por dentro.
                    .clickable(onClick = {})
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp
                        )
                    )
                    .background(
                        AeroColors.DialogSurface
                    )
                    .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Sleep Timer",
                    color = AeroColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = onDismiss) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = AeroColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isActive) {

                ActiveTimerContent(
                    state = state,
                    onCancel = onCancel
                )

            } else {

                Text(
                    text = "Por tiempo",
                    color = AeroColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(MINUTE_OPTIONS) { minutes ->

                        OptionChip(
                            label = "$minutes min",
                            onClick = { onSelectMinutes(minutes) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Después de X canciones",
                    color = AeroColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(SONG_OPTIONS) { count ->

                        OptionChip(
                            label = "$count",
                            onClick = { onSelectSongs(count) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActiveTimerContent(
    state: SleepTimerState,
    onCancel: () -> Unit
) {

    val description =
        when (val mode = state.mode) {

            is SleepTimerMode.ByTime -> {

                val totalSeconds =
                    state.remainingMs / 1000

                val minutes =
                    totalSeconds / 60

                val seconds =
                    totalSeconds % 60

                "Se pausará en %d:%02d".format(
                    minutes,
                    seconds
                )
            }

            is SleepTimerMode.BySongs -> {

                val label =
                    if (state.remainingSongs == 1) {
                        "1 canción"
                    } else {
                        "${state.remainingSongs} canciones"
                    }

                "Se pausará después de $label"
            }

            null -> ""
        }

    Text(
        text = description,
        color = AeroColors.TextPrimary,
        fontSize = 15.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.6f)
                )
                .clickable(onClick = onCancel)
                .padding(vertical = 14.dp),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Cancelar temporizador",
            color = AeroColors.Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OptionChip(
    label: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    AeroColors.Accent.copy(alpha = 0.14f)
                )
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color = AeroColors.Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
