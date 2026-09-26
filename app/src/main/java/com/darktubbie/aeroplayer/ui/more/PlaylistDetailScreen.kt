package com.darktubbie.aeroplayer.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Detalle de una playlist (Fase 3, 0.4.x): reproducir completa o
 * desde una canción, reordenar (con flechas arriba/abajo, ver
 * [com.darktubbie.aeroplayer.MainViewModel.movePlaylistTrack]),
 * quitar canciones, renombrar, eliminar la playlist, y entrar al
 * selector para agregar más canciones.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    tracks: List<AudioTrack>,
    currentTrackUri: String?,
    isPlaying: Boolean,
    onTrackClick: (AudioTrack) -> Unit,
    onPlayAll: () -> Unit,
    onMoveTrack: (AudioTrack, Int) -> Unit,
    onRemoveTrack: (AudioTrack) -> Unit,
    onRename: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onOpenAddTracks: () -> Unit,
    onBack: () -> Unit
) {

    var showRenameDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteConfirm by remember {
        mutableStateOf(false)
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
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(onClick = onBack) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                ) {

                    Text(
                        text = playlist.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1
                    )

                    Text(
                        text = pluralStringResource(R.plurals.song_count, tracks.size, tracks.size),

                        color = AeroColors.OnBackgroundSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                IconButton(
                    onClick = { showRenameDialog = true }
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_rename),
                        tint = Color.White
                    )
                }

                IconButton(onClick = onOpenAddTracks) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.playlist_add_songs_title),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true }
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_playlist),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (tracks.isNotEmpty()) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                AeroColors.Accent.copy(alpha = 0.18f)
                            )
                            .clickable(onClick = onPlayAll)
                            .padding(vertical = 10.dp),

                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AeroColors.Accent
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = stringResource(R.string.playlist_play_all),
                        color = AeroColors.Accent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (tracks.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = stringResource(R.string.playlist_empty_detail),

                        color = AeroColors.OnBackgroundSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    itemsIndexed(
                        tracks,

                        // Fase 6 (0.5.0): clave "índice-uri" en vez de
                        // solo track.uri — una playlist puede tener la
                        // misma canción agregada más de una vez (no
                        // hay nada que lo impida al agregar canciones),
                        // y con la clave repetida esta LazyColumn
                        // crasheaba igual que la de Home en la Fase 3
                        // (mismo IllegalArgumentException de Compose
                        // por clave duplicada).
                        key = { index, track -> "$index-${track.uri}" }

                    ) { index, track ->

                        val isCurrentTrack =
                            track.uri == currentTrackUri

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(
                                            alpha =
                                                if (isCurrentTrack) {
                                                    0.60f
                                                } else {
                                                    0.38f
                                                }
                                        )
                                    )
                                    .let { rowModifier ->

                                        if (isCurrentTrack) {

                                            rowModifier.border(
                                                1.dp,
                                                AeroColors.Accent.copy(
                                                    alpha = 0.6f
                                                ),
                                                RoundedCornerShape(16.dp)
                                            )

                                        } else {

                                            rowModifier
                                        }
                                    }
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
                                        .size(44.dp)
                                        .clip(
                                            RoundedCornerShape(10.dp)
                                        )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = track.title,
                                    color = AeroColors.TextPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )

                                Text(
                                    text = track.artist,
                                    color = AeroColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }

                            if (isCurrentTrack) {

                                Icon(
                                    imageVector =
                                        if (isPlaying) {
                                            Icons.Default.Pause
                                        } else {
                                            Icons.Default.PlayArrow
                                        },

                                    contentDescription = null,
                                    tint = AeroColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = {
                                    onMoveTrack(track, -1)
                                },

                                enabled = index > 0,
                                modifier = Modifier.size(30.dp)
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.KeyboardArrowUp,

                                    contentDescription = stringResource(R.string.cd_move_up),

                                    tint =
                                        if (index > 0) {
                                            AeroColors.TextSecondary
                                        } else {
                                            AeroColors.TextTertiary.copy(
                                                alpha = 0.4f
                                            )
                                        },

                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onMoveTrack(track, 1)
                                },

                                enabled = index < tracks.lastIndex,
                                modifier = Modifier.size(30.dp)
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.KeyboardArrowDown,

                                    contentDescription = stringResource(R.string.cd_move_down),

                                    tint =
                                        if (index < tracks.lastIndex) {
                                            AeroColors.TextSecondary
                                        } else {
                                            AeroColors.TextTertiary.copy(
                                                alpha = 0.4f
                                            )
                                        },

                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onRemoveTrack(track)
                                },

                                modifier = Modifier.size(30.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_remove_from_playlist),
                                    tint = AeroColors.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {

        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename_title),
            initialValue = playlist.name,

            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },

            onDismiss = {
                showRenameDialog = false
            }
        )
    }

    if (showDeleteConfirm) {

        ConfirmDeletePlaylistDialog(
            playlistName = playlist.name,

            onConfirm = {
                showDeleteConfirm = false
                onDeletePlaylist()
            },

            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
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
                text = stringResource(R.string.playlist_delete_confirm, playlistName),
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
                        text = stringResource(R.string.action_delete),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
