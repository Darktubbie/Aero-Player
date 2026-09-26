package com.darktubbie.aeroplayer.ui.more

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historial de reproducción (Fase 4, 0.4.x), colgado de la fila
 * "Historial" en Más — mismo patrón de overlay que las demás
 * pantallas de esta sección.
 *
 * [entries] llega ya en orden cronológico (más reciente primero,
 * ver [com.darktubbie.aeroplayer.MainViewModel.historyTracks]) y
 * resuelto contra la biblioteca actual — una canción borrada del
 * almacenamiento simplemente ya no aparece acá.
 */
@Composable
fun HistoryScreen(
    entries: List<Pair<AudioTrack, Long>>,
    onTrackClick: (AudioTrack) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {

    var showClearConfirm by remember {
        mutableStateOf(false)
    }

    val dateFormat =
        remember {
            SimpleDateFormat(
                "d MMM, HH:mm",
                Locale.getDefault()
            )
        }

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = onBack) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.history_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (entries.isNotEmpty()) {

                    IconButton(
                        onClick = { showClearConfirm = true }
                    ) {

                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.history_clear_cd),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (entries.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = stringResource(R.string.history_empty),
                        color = AeroColors.OnBackgroundSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        entries,
                        key = { (track, playedAtMs) ->
                            "${track.uri}_$playedAtMs"
                        }

                    ) { (track, playedAtMs) ->

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                                    )
                                    .clickable {
                                        onTrackClick(track)
                                    }
                                    .padding(10.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            AlbumArt(
                                path = track.path,
                                artist = track.artist,
                                album = track.album,

                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = track.title,
                                    color = AeroColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )

                                Text(
                                    text = track.artist,
                                    color = AeroColors.TextSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text =
                                    dateFormat.format(
                                        Date(playedAtMs)
                                    ),

                                color = AeroColors.TextTertiary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {

        ConfirmClearHistoryDialog(
            onConfirm = {
                showClearConfirm = false
                onClearHistory()
            },

            onDismiss = {
                showClearConfirm = false
            }
        )
    }
}

@Composable
private fun ConfirmClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),

        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(onClick = {})
                    .clip(RoundedCornerShape(20.dp))
                    .background(AeroColors.DialogSurface)
                    .padding(20.dp)
        ) {

            Text(
                text = stringResource(R.string.history_clear_confirm_title),
                color = AeroColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.action_undo_warning),
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.6f))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE05B5B).copy(alpha = 0.85f))
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = stringResource(R.string.history_clear),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
