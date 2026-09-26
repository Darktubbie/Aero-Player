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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Selector de canciones de toda la biblioteca para agregar a una
 * playlist puntual (Fase 3, 0.4.x) — se abre desde el botón "+" de
 * [PlaylistDetailScreen].
 *
 * Tocar una fila alterna su membresía inmediatamente (agregar si no
 * estaba, quitar si ya estaba) en vez de un flujo de selección +
 * botón "Guardar" — mismo gesto directo que ya se usa para
 * favoritos, sin paso extra de confirmación.
 */
@Composable
fun AddTracksToPlaylistScreen(
    playlistName: String,
    allTracks: List<AudioTrack>,
    isTrackInPlaylist: (AudioTrack) -> Boolean,
    onToggleTrack: (AudioTrack) -> Unit,
    onBack: () -> Unit
) {

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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

                Column(
                    modifier = Modifier.padding(start = 4.dp)
                ) {

                    Text(
                        text = stringResource(R.string.playlist_add_songs_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(
                        text = stringResource(R.string.playlist_add_songs_to, playlistName),
                        color = AeroColors.OnBackgroundSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (allTracks.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = stringResource(R.string.library_empty),
                        color = AeroColors.OnBackgroundSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    items(
                        allTracks,
                        key = { it.uri }
                    ) { track ->

                        val inPlaylist =
                            isTrackInPlaylist(track)

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(
                                            alpha =
                                                if (inPlaylist) {
                                                    0.55f
                                                } else {
                                                    0.30f
                                                }
                                        )
                                    )
                                    .clickable {
                                        onToggleTrack(track)
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
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

                            if (inPlaylist) {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(AeroColors.Accent),

                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.playlist_already_in_it_2),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
