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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Lista de canciones favoritas (Fase 2, 0.4.x), colgada de la fila
 * "Favoritos" en Más — mismo patrón de overlay a pantalla completa
 * que [FoldersScreen]/[SettingsScreen].
 *
 * [tracks] llega ya ordenado (con el mismo criterio elegido para
 * Songs) desde [com.darktubbie.aeroplayer.MainViewModel.favoriteTracks].
 */
@Composable
fun FavoritesScreen(
    tracks: List<AudioTrack>,
    currentTrackUri: String?,
    isPlaying: Boolean,
    onTrackClick: (AudioTrack) -> Unit,
    onPlayAll: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
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
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Favoritos",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (tracks.isNotEmpty()) {

                    IconButton(onClick = onPlayAll) {

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir todos",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tracks.isEmpty()) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize(),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text =
                            "Todavía no marcaste ninguna canción " +
                            "como favorita",

                        color = AeroColors.OnBackgroundSubtitle,
                        fontSize = 13.sp
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        tracks,

                        key = { it.uri }

                    ) { track ->

                        val isCurrentTrack =
                            track.uri == currentTrackUri

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(16.dp)
                                    )
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
                                        .size(48.dp)
                                        .clip(
                                            RoundedCornerShape(12.dp)
                                        )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = track.title,
                                    color = AeroColors.TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )

                                Text(
                                    text = track.artist,
                                    color = AeroColors.TextSecondary,
                                    fontSize = 12.sp,
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

                                    contentDescription =
                                        if (isPlaying) {
                                            "Playing"
                                        } else {
                                            "Paused"
                                        },

                                    tint = AeroColors.Accent
                                )
                            }

                            IconButton(
                                onClick = {
                                    onToggleFavorite(track)
                                },

                                modifier = Modifier.size(36.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Quitar de favoritos",
                                    tint = AeroColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
