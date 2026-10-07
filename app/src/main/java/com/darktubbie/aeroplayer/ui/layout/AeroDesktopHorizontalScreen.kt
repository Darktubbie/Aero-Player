package com.darktubbie.aeroplayer.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.Album
import com.darktubbie.aeroplayer.data.Artist
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Aero Desktop horizontal (Fase 2, 0.6.0 — rediseño post-referencias
 * visuales): ya no es un panel lateral de biblioteca + panel lateral
 * de Now Playing — es una estación multimedia de 2 zonas (sidebar +
 * contenido protagonista con [DesktopContentHeader]) más una barra
 * de reproducción de ancho completo pegada abajo (ver [NowPlayingDock]
 * en DesktopShared.kt), como en las referencias.
 *
 * No introduce ningún estado ni repositorio nuevo: todo lo que
 * recibe ya existe en [com.darktubbie.aeroplayer.MainViewModel].
 */
@Composable
fun AeroDesktopHorizontalScreen(
    tracks: List<AudioTrack>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    favoriteTracks: List<AudioTrack>,
    historyTracks: List<AudioTrack>,
    tracksInPlaylist: (Playlist) -> List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onTick: () -> Unit,
    onExitDesktopMode: () -> Unit,
    // Fase 8 (0.6.0): abre el Album Showcase (overlay a pantalla
    // completa, compartido con Mobile).
    onOpenAlbumShowcase: ((Album) -> Unit)? = null
) {

    var section by remember {
        mutableStateOf(DesktopSection.SONGS)
    }

    var selectedAlbum by remember {
        mutableStateOf<Album?>(null)
    }

    var selectedArtist by remember {
        mutableStateOf<Artist?>(null)
    }

    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }

    val visibleTracks: List<AudioTrack> =
        when (section) {

            DesktopSection.SONGS -> tracks

            DesktopSection.ALBUMS ->
                selectedAlbum?.tracks ?: emptyList()

            DesktopSection.ARTISTS ->
                selectedArtist?.tracks ?: emptyList()

            DesktopSection.PLAYLISTS ->
                selectedPlaylist?.let(tracksInPlaylist) ?: emptyList()

            DesktopSection.FAVORITES -> favoriteTracks

            DesktopSection.HISTORY -> historyTracks
        }

    AeroBackground {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {

                DesktopSidebar(
                    section = section,
                    albums = albums,
                    artists = artists,
                    playlists = playlists,
                    selectedAlbum = selectedAlbum,
                    selectedArtist = selectedArtist,
                    selectedPlaylist = selectedPlaylist,

                    onSectionSelected = { newSection ->
                        section = newSection
                        selectedAlbum = null
                        selectedArtist = null
                        selectedPlaylist = null
                    },

                    onAlbumSelected = { selectedAlbum = it },
                    onArtistSelected = { selectedArtist = it },
                    onPlaylistSelected = { selectedPlaylist = it },
                    onExitDesktopMode = onExitDesktopMode,

                    modifier =
                        Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                )

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                ) {

                    DesktopSectionHeader(
                        section = section,
                        selectedAlbum = selectedAlbum,
                        selectedArtist = selectedArtist,
                        selectedPlaylist = selectedPlaylist,
                        onOpenAlbumShowcase = onOpenAlbumShowcase,

                        onPlayAll = {
                            visibleTracks.firstOrNull()?.let { first ->
                                onTrackClick(first, visibleTracks)
                            }
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                    )

                    DesktopTrackList(
                        tracks = visibleTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,

                        onTrackClick = { track ->
                            onTrackClick(track, visibleTracks)
                        },

                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            NowPlayingDock(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onPlayPauseClick = onPlayPauseClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                onTick = onTick,
                compact = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DesktopSidebar(
    section: DesktopSection,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    selectedAlbum: Album?,
    selectedArtist: Artist?,
    selectedPlaylist: Playlist?,
    onSectionSelected: (DesktopSection) -> Unit,
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (Artist) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onExitDesktopMode: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .aeroGlass(
                    shape = RectangleShape,
                    glassColor = AeroColors.GlassSurfaceBase,
                    baseAlpha = 0.20f,
                    elevation = 0.dp
                )
                .padding(vertical = 16.dp)
    ) {

        // Sin esto no hay forma de salir de Aero Desktop una vez
        // activado AUTO en Ajustes: esa pantalla queda tapada por
        // completo mientras este modo está activo.
        Row(
            verticalAlignment = Alignment.CenterVertically,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExitDesktopMode)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription =
                    stringResource(R.string.cd_exit_desktop),
                tint = AeroColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.desktop_exit_label),
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SidebarEntry(
            icon = Icons.Default.LibraryMusic,
            label = stringResource(R.string.library_tab_songs),
            selected = section == DesktopSection.SONGS,
            onClick = { onSectionSelected(DesktopSection.SONGS) }
        )

        SidebarEntry(
            icon = Icons.Default.AlbumIcon,
            label = stringResource(R.string.library_tab_albums),
            selected = section == DesktopSection.ALBUMS,
            onClick = { onSectionSelected(DesktopSection.ALBUMS) }
        )

        SidebarEntry(
            icon = Icons.Default.Person,
            label = stringResource(R.string.library_tab_artists),
            selected = section == DesktopSection.ARTISTS,
            onClick = { onSectionSelected(DesktopSection.ARTISTS) }
        )

        SidebarEntry(
            icon = Icons.Default.QueueMusic,
            label = stringResource(R.string.playlists_title),
            selected = section == DesktopSection.PLAYLISTS,
            onClick = { onSectionSelected(DesktopSection.PLAYLISTS) }
        )

        SidebarEntry(
            icon = Icons.Default.Favorite,
            label = stringResource(R.string.favorites_title),
            selected = section == DesktopSection.FAVORITES,
            onClick = { onSectionSelected(DesktopSection.FAVORITES) }
        )

        SidebarEntry(
            icon = Icons.Default.History,
            label = stringResource(R.string.history_title),
            selected = section == DesktopSection.HISTORY,
            onClick = { onSectionSelected(DesktopSection.HISTORY) }
        )

        when (section) {

            DesktopSection.ALBUMS -> {

                LazyColumn {

                    items(albums) { album ->

                        SidebarSubEntry(
                            label = album.name,
                            selected = album == selectedAlbum,
                            onClick = { onAlbumSelected(album) }
                        )
                    }
                }
            }

            DesktopSection.ARTISTS -> {

                LazyColumn {

                    items(artists) { artist ->

                        SidebarSubEntry(
                            label = artist.name,
                            selected = artist == selectedArtist,
                            onClick = { onArtistSelected(artist) }
                        )
                    }
                }
            }

            DesktopSection.PLAYLISTS -> {

                LazyColumn {

                    items(playlists) { playlist ->

                        SidebarSubEntry(
                            label = playlist.name,
                            selected = playlist == selectedPlaylist,
                            onClick = { onPlaylistSelected(playlist) }
                        )
                    }
                }
            }

            else -> {
                // Songs/Favoritos/Historial no tienen sub-árbol.
            }
        }
    }
}

@Composable
private fun SidebarEntry(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .then(
                    if (selected) {
                        Modifier.aeroGlass(
                            shape = RoundedCornerShape(12.dp),
                            glassColor = AeroColors.Accent,
                            baseAlpha = 0.35f,
                            elevation = 0.dp
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,

            tint =
                if (selected) {
                    AeroColors.Accent
                } else {
                    AeroColors.TextSecondary
                },

            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,

            color =
                if (selected) {
                    AeroColors.Accent
                } else {
                    AeroColors.TextPrimary
                },

            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SidebarSubEntry(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,

        color =
            if (selected) {
                AeroColors.Accent
            } else {
                AeroColors.TextSecondary
            },

        style = MaterialTheme.typography.bodyMedium,

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 38.dp, vertical = 6.dp)
    )
}
