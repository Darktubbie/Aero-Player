package com.darktubbie.aeroplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Mini-player persistente: portada, título, artista, play/pause,
 * y una zona pulsable para abrir Now Playing.
 *
 * [onOpenNowPlaying] todavía no navega a ningún sitio — Now
 * Playing es la Fase 7 del plan. Se deja ya la zona pulsable y el
 * callback preparados para no tener que volver a tocar este
 * archivo cuando esa pantalla exista.
 *
 * Recibe solo lo que necesita (una canción, no toda la lista, ni
 * el resto del estado de la pantalla), para que Compose pueda
 * saltarse su recomposición cuando cambia algo que no le afecta
 * (por ejemplo, mientras se escanea la biblioteca).
 */
@Composable
fun MiniPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(20.dp)
                )
                .background(
                    Color.White.copy(
                        alpha = 0.48f
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(
                        alpha = 0.72f
                    ),
                    RoundedCornerShape(20.dp)
                )
                .clickable {
                    onOpenNowPlaying()
                }
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AlbumArt(
            path =
                track.path,

            artist =
                track.artist,

            album =
                track.album,

            modifier =
                Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    track.title,

                color =
                    AeroColors.TextPrimary,

                fontSize =
                    14.sp,

                maxLines = 1
            )

            Text(
                text =
                    track.artist,

                color =
                    AeroColors.TextSecondary,

                fontSize =
                    12.sp,

                maxLines = 1
            )
        }

        IconButton(
            onClick =
                onPlayPauseClick
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
                        "Pause"
                    } else {
                        "Play"
                    },

                tint =
                    AeroColors.Accent
            )
        }
    }
}
