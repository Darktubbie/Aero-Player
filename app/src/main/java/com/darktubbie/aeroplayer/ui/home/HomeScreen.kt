package com.darktubbie.aeroplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Sección "Inicio" (Fase 3 del roadmap 0.5.0 — Aero Home).
 *
 * Antes era solo un placeholder de bienvenida (Fase 1 del plan de
 * evolución visual); ahora es el panel principal real del
 * reproductor. No introduce ningún sistema nuevo de datos: todo lo
 * que muestra viene de repositorios que ya existían (reproducción
 * actual, Historial, Favoritos, Playlists, Biblioteca) resuelto por
 * [com.darktubbie.aeroplayer.MainViewModel] — esta pantalla solo
 * arma la interfaz con esa información, sin tocar cómo se guarda ni
 * cómo se reproduce nada.
 *
 * "Continue Listening" no agrega persistencia nueva: muestra la
 * canción actualmente cargada en el reproductor (sonando o en
 * pausa), tal como ya la expone [com.darktubbie.aeroplayer.playback.PlayerRepository].
 * Si no hay ninguna canción cargada en esta sesión, la sección
 * simplemente no aparece.
 */
@Composable
fun HomeScreen(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    recentlyPlayed: List<AudioTrack>,
    favoriteTracks: List<AudioTrack>,
    playlists: List<Playlist>,
    playlistTrackCount: (Playlist) -> Int,
    aeroPicks: List<AudioTrack>,

    onContinueListeningClick: () -> Unit,
    onContinueListeningPlayPause: () -> Unit,
    onRecentlyPlayedClick: (AudioTrack) -> Unit,
    onViewAllRecentlyPlayed: () -> Unit,
    onFavoriteClick: (AudioTrack) -> Unit,
    onViewAllFavorites: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onViewAllPlaylists: () -> Unit,
    onAeroPickClick: (AudioTrack) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSettings: () -> Unit
) {

    AeroBackground {

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),

            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(26.dp)
        ) {

            item {

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AeroColors.OnBackgroundSubtitle
                )
            }

            if (currentTrack != null) {

                item {

                    Column {

                        SectionHeader(
                            title = stringResource(R.string.home_continue_listening)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ContinueListeningCard(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onClick = onContinueListeningClick,
                            onPlayPauseClick = onContinueListeningPlayPause
                        )
                    }
                }
            }

            if (recentlyPlayed.isNotEmpty()) {

                item {

                    Column {

                        SectionHeader(
                            title = stringResource(R.string.home_recently_played),
                            onSeeAll = onViewAllRecentlyPlayed
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TrackCardRow(
                            tracks = recentlyPlayed.take(10),
                            onTrackClick = onRecentlyPlayedClick
                        )
                    }
                }
            }

            if (favoriteTracks.isNotEmpty()) {

                item {

                    Column {

                        SectionHeader(
                            title = stringResource(R.string.favorites_title),
                            onSeeAll = onViewAllFavorites
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TrackCardRow(
                            tracks = favoriteTracks.take(10),
                            onTrackClick = onFavoriteClick
                        )
                    }
                }
            }

            if (playlists.isNotEmpty()) {

                item {

                    Column {

                        SectionHeader(
                            title = stringResource(R.string.playlists_title),
                            onSeeAll = onViewAllPlaylists
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            items(
                                playlists,
                                key = { it.id }
                            ) { playlist ->

                                PlaylistCard(
                                    playlist = playlist,
                                    trackCount = playlistTrackCount(playlist),
                                    onClick = {
                                        onPlaylistClick(playlist)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {

                Column {

                    SectionHeader(
                        title = stringResource(R.string.home_aero_picks)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (aeroPicks.isEmpty()) {

                        Text(
                            text = stringResource(R.string.home_aero_picks_empty),
                            color = AeroColors.OnBackgroundSubtitle,
                            style = MaterialTheme.typography.bodySmall
                        )

                    } else {

                        TrackCardRow(
                            tracks = aeroPicks,
                            onTrackClick = onAeroPickClick
                        )
                    }
                }
            }

            item {

                Column {

                    SectionHeader(
                        title = stringResource(R.string.home_quick_actions)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        QuickActionButton(
                            icon = Icons.Default.LibraryMusic,
                            label = stringResource(R.string.library_title),
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionButton(
                            icon = Icons.Default.Star,
                            label = stringResource(R.string.favorites_title),
                            onClick = onOpenFavorites,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionButton(
                            icon = Icons.Default.PlaylistPlay,
                            label = stringResource(R.string.playlists_title),
                            onClick = onOpenPlaylists,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        QuickActionButton(
                            icon = Icons.Default.Folder,
                            label = stringResource(R.string.folders_title),
                            onClick = onOpenFolders,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionButton(
                            icon = Icons.Default.Settings,
                            label = stringResource(R.string.settings_title),
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        if (onSeeAll != null) {

            Text(
                text = stringResource(R.string.home_see_all),
                color = AeroColors.Accent,
                style = MaterialTheme.typography.labelMedium,
                modifier =
                    Modifier.clickable(onClick = onSeeAll)
            )
        }
    }
}

/**
 * Tarjeta de "Continue Listening": mismo patrón visual/estructural
 * que [com.darktubbie.aeroplayer.ui.components.MiniPlayer] (portada
 * + título/artista + play/pause, zona pulsable para abrir Now
 * Playing), con una barra de progreso fina agregada debajo.
 */
@Composable
private fun ContinueListeningCard(
    track: AudioTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.42f)
                )
                .border(
                    1.dp,
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.7f),
                    RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onClick)
                .padding(12.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            AlbumArt(
                path = track.path,
                artist = track.artist,
                album = track.album,

                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = track.title,
                    color = AeroColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )

                Text(
                    text = track.artist,
                    color = AeroColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }

            IconButton(onClick = onPlayPauseClick) {

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

                    tint = AeroColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val progress =
            if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.5f)
                    )
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AeroColors.Accent)
            )
        }
    }
}

/**
 * Fila horizontal de tarjetas de canción — usada por Recently
 * Played, Favorites y Aero Picks en Home. Cada sección le pasa su
 * propia lista ya resuelta y su propio callback de click.
 */
@Composable
private fun TrackCardRow(
    tracks: List<AudioTrack>,
    onTrackClick: (AudioTrack) -> Unit
) {

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // itemsIndexed + clave "índice-uri": Recently Played viene del
        // Historial, que NO deduplica a propósito (escuchar la misma
        // canción varias veces son eventos de historial legítimamente
        // distintos), así que el mismo track.uri puede repetirse en
        // esta lista. Una LazyRow exige claves únicas por item, y
        // usar solo track.uri ahí crasheaba la app al abrir Inicio
        // si el historial tenía una canción repetida.
        itemsIndexed(
            tracks,
            key = { index, track -> "$index-${track.uri}" }
        ) { _, track ->

            TrackCard(
                track = track,
                onClick = {
                    onTrackClick(track)
                }
            )
        }
    }
}

@Composable
private fun TrackCard(
    track: AudioTrack,
    onClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .width(112.dp)
                .clickable(onClick = onClick)
    ) {

        AlbumArt(
            path = track.path,
            artist = track.artist,
            album = track.album,

            // Fase 5 de la Experiencia de Artwork (0.5.0): a 112dp
            // esta tarjeta necesita hasta 448px en pantallas xxxhdpi
            // (4x) — el caché chico (192px) se veía notoriamente
            // borroso acá, mismo motivo por el que Now Playing ya
            // usaba highRes desde la Fase 7 de 0.4.x.
            highRes = true,

            modifier =
                Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )

        Text(
            text = track.artist,
            color = AeroColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    trackCount: Int,
    onClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .width(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                )
                .clickable(onClick = onClick)
                .padding(14.dp)
    ) {

        Icon(
            imageVector = Icons.Default.PlaylistPlay,
            contentDescription = null,
            tint = AeroColors.Accent
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.name,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )

        Text(
            text =
                pluralStringResource(
                    R.plurals.song_count,
                    trackCount,
                    trackCount
                ),
            color = AeroColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.32f)
                )
                .border(
                    1.dp,
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.55f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp, horizontal = 8.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AeroColors.Accent
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
