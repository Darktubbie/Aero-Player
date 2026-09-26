package com.darktubbie.aeroplayer.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
import androidx.media3.common.Player
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.playback.SleepTimerState
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlinx.coroutines.delay

/**
 * Pantalla Now Playing: portada grande, progreso, transporte,
 * shuffle y repeat.
 *
 * Fase 7 del plan. Reutiliza AeroBackground/AlbumArt/AeroColors
 * tal como se planteó en la Sección D del informe, para que se
 * sienta igual de "Aero" que la biblioteca sin duplicar estilos.
 */
@Composable
fun NowPlayingScreen(
    track: AudioTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onTick: () -> Unit,
    onOpenApeEditor: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    sleepTimerState: SleepTimerState,
    onOpenSleepTimer: () -> Unit
) {

    /*
     * Media3 no empuja actualizaciones continuas de posición, así
     * que esta pantalla sondea mientras está visible. Se detiene
     * sola en cuanto el usuario vuelve a la biblioteca (el
     * LaunchedEffect se cancela al salir de composición).
     */
    LaunchedEffect(Unit) {

        while (true) {

            onTick()

            delay(500)
        }
    }

    var isDragging by remember {
        mutableStateOf(false)
    }

    var dragValue by remember {
        mutableStateOf(0f)
    }

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription =
                            stringResource(R.string.cd_back),

                        tint = Color.White
                    )
                }

                Text(
                    text = stringResource(R.string.now_playing_title),

                    color = Color.White,

                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                IconButton(
                    onClick = onToggleFavorite
                ) {

                    Icon(
                        imageVector =
                            if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },

                        contentDescription =
                            if (isFavorite) {
                                stringResource(R.string.favorites_remove)
                            } else {
                                stringResource(R.string.cd_favorite_add)
                            },

                        tint =
                            if (isFavorite) {
                                AeroColors.Accent
                            } else {
                                Color.White
                            }
                    )
                }

                IconButton(
                    onClick = onAddToPlaylist
                ) {

                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = stringResource(R.string.playlist_add_to),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onOpenSleepTimer
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Timer,

                        contentDescription =
                            if (sleepTimerState.isActive) {
                                stringResource(R.string.cd_sleep_timer_active)
                            } else {
                                stringResource(R.string.sleep_timer_title)
                            },

                        tint =
                            if (sleepTimerState.isActive) {
                                AeroColors.Accent
                            } else {
                                Color.White
                            }
                    )
                }

                IconButton(
                    onClick = onOpenApeEditor
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,

                        contentDescription =
                            stringResource(R.string.cd_edit_effects),

                        tint = Color.White
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            Box(
                modifier =
                    Modifier.fillMaxWidth(),

                contentAlignment =
                    Alignment.Center
            ) {

                AlbumArt(
                    path = track.path,
                    artist = track.artist,
                    album = track.album,
                    highRes = true,
                    animateChanges = true,

                    modifier =
                        Modifier
                            .size(260.dp)
                            .clip(
                                RoundedCornerShape(28.dp)
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Text(
                text = track.title,

                color = AeroColors.TextPrimary,

                style = MaterialTheme.typography.headlineMedium,

                maxLines = 1,

                modifier =
                    Modifier
                        .fillMaxWidth(),

                textAlign =
                    TextAlign.Center
            )

            Text(
                text = track.artist,

                color = AeroColors.TextSecondary,

                style = MaterialTheme.typography.bodyLarge,

                maxLines = 1,

                modifier =
                    Modifier.fillMaxWidth(),

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            val sliderRange =
                0f..(
                    durationMs
                        .coerceAtLeast(1L)
                        .toFloat()
                )

            Slider(
                value =
                    if (isDragging) {
                        dragValue
                    } else {
                        positionMs.toFloat()
                    },

                onValueChange = { value ->

                    isDragging = true

                    dragValue = value
                },

                onValueChangeFinished = {

                    onSeek(dragValue.toLong())

                    isDragging = false
                },

                valueRange = sliderRange,

                colors =
                    SliderDefaults.colors(
                        thumbColor =
                            AeroColors.Accent,

                        activeTrackColor =
                            AeroColors.Accent,

                        inactiveTrackColor =
                            AeroColors.GlassSurfaceBase.copy(
                                alpha = 0.35f
                            )
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        formatTime(
                            if (isDragging) {
                                dragValue.toLong()
                            } else {
                                positionMs
                            }
                        ),

                    color = AeroColors.TextSecondary,

                    style = MaterialTheme.typography.labelMedium,
                )

                Text(
                    text = formatTime(durationMs),

                    color = AeroColors.TextSecondary,

                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceEvenly,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onToggleShuffle
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Shuffle,

                        contentDescription =
                            stringResource(R.string.cd_shuffle),

                        tint =
                            if (shuffleEnabled) {
                                AeroColors.Accent
                            } else {
                                AeroColors.TextSecondary
                            }
                    )
                }

                IconButton(
                    onClick = onPrevious
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipPrevious,

                        contentDescription =
                            stringResource(R.string.cd_previous),

                        tint = AeroColors.TextPrimary,

                        modifier =
                            Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                AeroColors.GlassSurfaceBase.copy(
                                    alpha = 0.42f
                                )
                            )
                            .border(
                                1.dp,
                                AeroColors.GlassSurfaceBase.copy(
                                    alpha = 0.7f
                                ),
                                CircleShape
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    IconButton(
                        onClick = onPlayPauseClick
                    ) {

                        Icon(
                            imageVector =
                                if (isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },

                            contentDescription =
                                if (isPlaying) {
                                    stringResource(R.string.cd_pause_action)
                                } else {
                                    stringResource(R.string.cd_play_action)
                                },

                            tint = AeroColors.Accent,

                            modifier =
                                Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipNext,

                        contentDescription =
                            stringResource(R.string.cd_next),

                        tint = AeroColors.TextPrimary,

                        modifier =
                            Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat
                ) {

                    Icon(
                        imageVector =
                            if (
                                repeatMode ==
                                Player.REPEAT_MODE_ONE
                            ) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },

                        contentDescription =
                            stringResource(R.string.cd_repeat),

                        tint =
                            if (
                                repeatMode !=
                                Player.REPEAT_MODE_OFF
                            ) {
                                AeroColors.Accent
                            } else {
                                AeroColors.TextSecondary
                            }
                    )
                }
            }
        }
    }
}

private fun formatTime(
    ms: Long
): String {

    val totalSeconds =
        (ms / 1000)
            .coerceAtLeast(0L)

    val minutes = totalSeconds / 60

    val seconds = totalSeconds % 60

    return "%d:%02d".format(
        minutes,
        seconds
    )
}

/**
 * Contenido de la sección "Reproductor" (Fase 1) cuando no hay
 * ninguna canción activa todavía. Convertir el reproductor en una
 * sección principal a todos los efectos (estado persistente entre
 * navegaciones, etc.) es alcance de la Fase 2 — aquí solo se cubre
 * el caso de "destino de navegación sin contenido que mostrar".
 */
@Composable
fun EmptyNowPlayingPlaceholder() {

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = stringResource(R.string.now_playing_empty_title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = stringResource(R.string.now_playing_empty_hint),
                color = AeroColors.OnBackgroundSubtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
