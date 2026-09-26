package com.darktubbie.aeroplayer.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistPlay
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
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.ui.more.PlaylistNameDialog
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Panel "Agregar a playlist" para una única canción (Fase 3,
 * 0.4.x) — se abre desde el ícono correspondiente en la fila de
 * Songs (LibraryScreen) o en el header de Now Playing.
 *
 * Mismo patrón de overlay inferior que
 * [com.darktubbie.aeroplayer.ui.nowplaying.SleepTimerSheet]: fondo
 * oscurecido + tarjeta que sube desde abajo. Cada playlist se
 * muestra con un check si [track] ya está en ella; tocarla alterna
 * su membresía. También permite crear una playlist nueva ya con
 * esta canción adentro.
 */
@Composable
fun AddToPlaylistSheet(
    track: AudioTrack,
    playlists: List<Playlist>,
    isTrackInPlaylist: (Playlist) -> Boolean,
    onTogglePlaylist: (Playlist) -> Unit,
    onCreatePlaylistWithTrack: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),

        contentAlignment = Alignment.BottomCenter
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clickable(onClick = {})
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp
                        )
                    )
                    .background(AeroColors.DialogSurface)
                    .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = stringResource(R.string.playlist_add_to),
                    color = AeroColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )

                IconButton(onClick = onDismiss) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = AeroColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AeroColors.Accent.copy(alpha = 0.14f))
                        .clickable {
                            showCreateDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AeroColors.Accent
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.playlist_new),
                    color = AeroColors.Accent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (playlists.isEmpty()) {

                Text(
                    text = stringResource(R.string.playlist_none_yet),
                    color = AeroColors.TextTertiary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    items(
                        playlists,
                        key = { it.id }
                    ) { playlist ->

                        val inPlaylist =
                            isTrackInPlaylist(playlist)

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        onTogglePlaylist(playlist)
                                    }
                                    .padding(12.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = AeroColors.Accent,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = playlist.name,
                                color = AeroColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            if (inPlaylist) {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AeroColors.Accent),

                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.playlist_already_in_it),
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {

        PlaylistNameDialog(
            title = stringResource(R.string.playlist_new),
            initialValue = "",

            onConfirm = { name ->
                onCreatePlaylistWithTrack(name)
                showCreateDialog = false
            },

            onDismiss = {
                showCreateDialog = false
            }
        )
    }
}
