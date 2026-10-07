package com.darktubbie.aeroplayer.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Aero Desktop vertical (Fase 3, 0.6.0 — rediseño post-referencias
 * visuales): la misma familia visual que Desktop horizontal (mismo
 * vidrio, mismo encabezado de sección con carátula protagonista,
 * mismas cápsulas de canciones, misma barra de reproducción de borde
 * a borde abajo) pero reorganizada para un teléfono en vertical:
 * navegación por tabs de vidrio con ícono en vez de sidebar, y
 * drill-down (grupo → canciones) con flecha de volver en vez de
 * árbol lateral. Vertical es el "reproductor portátil"; horizontal
 * es la "estación": dos formas de existir del mismo diseño.
 */
@Composable
fun AeroDesktopVerticalScreen(
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

    val showingGroupList: Boolean =
        when (section) {
            DesktopSection.ALBUMS -> selectedAlbum == null
            DesktopSection.ARTISTS -> selectedArtist == null
            DesktopSection.PLAYLISTS -> selectedPlaylist == null
            else -> false
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

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
            ) {

                // Sin esto no hay forma de volver a Mobile una vez
                // activado AUTO en Ajustes.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = stringResource(R.string.settings_desktop_label),
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onExitDesktopMode) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription =
                                stringResource(R.string.cd_exit_desktop),
                            tint = AeroColors.TextSecondary
                        )
                    }
                }

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

                    modifier = Modifier.fillMaxWidth()
                )

                DesktopSectionTabs(
                    section = section,

                    onSectionSelected = { newSection ->
                        section = newSection
                        selectedAlbum = null
                        selectedArtist = null
                        selectedPlaylist = null
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                )

                if (showingGroupList) {

                    when (section) {

                        DesktopSection.ALBUMS ->
                            DesktopGroupList(
                                items = albums.map { it.name to it.tracks.size },
                                onClick = { index -> selectedAlbum = albums[index] }
                            )

                        DesktopSection.ARTISTS ->
                            DesktopGroupList(
                                items = artists.map { it.name to it.tracks.size },
                                onClick = { index -> selectedArtist = artists[index] }
                            )

                        DesktopSection.PLAYLISTS ->
                            DesktopGroupList(
                                items = playlists.map { it.name to it.trackKeys.size },
                                onClick = { index -> selectedPlaylist = playlists[index] }
                            )

                        else -> {
                            // showingGroupList ya excluye Songs/
                            // Favoritos/Historial.
                        }
                    }

                } else {

                    if (
                        section == DesktopSection.ALBUMS ||
                        section == DesktopSection.ARTISTS ||
                        section == DesktopSection.PLAYLISTS
                    ) {

                        DesktopBackRow(
                            onBack = {
                                selectedAlbum = null
                                selectedArtist = null
                                selectedPlaylist = null
                            }
                        )
                    }

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
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DesktopSectionTabs(
    section: DesktopSection,
    onSectionSelected: (DesktopSection) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier.horizontalScroll(rememberScrollState()),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        DesktopSectionChip(
            icon = Icons.Default.LibraryMusic,
            label = stringResource(R.string.library_tab_songs),
            selected = section == DesktopSection.SONGS,
            onClick = { onSectionSelected(DesktopSection.SONGS) }
        )

        DesktopSectionChip(
            icon = Icons.Default.AlbumIcon,
            label = stringResource(R.string.library_tab_albums),
            selected = section == DesktopSection.ALBUMS,
            onClick = { onSectionSelected(DesktopSection.ALBUMS) }
        )

        DesktopSectionChip(
            icon = Icons.Default.Person,
            label = stringResource(R.string.library_tab_artists),
            selected = section == DesktopSection.ARTISTS,
            onClick = { onSectionSelected(DesktopSection.ARTISTS) }
        )

        DesktopSectionChip(
            icon = Icons.Default.QueueMusic,
            label = stringResource(R.string.playlists_title),
            selected = section == DesktopSection.PLAYLISTS,
            onClick = { onSectionSelected(DesktopSection.PLAYLISTS) }
        )

        DesktopSectionChip(
            icon = Icons.Default.Favorite,
            label = stringResource(R.string.favorites_title),
            selected = section == DesktopSection.FAVORITES,
            onClick = { onSectionSelected(DesktopSection.FAVORITES) }
        )

        DesktopSectionChip(
            icon = Icons.Default.History,
            label = stringResource(R.string.history_title),
            selected = section == DesktopSection.HISTORY,
            onClick = { onSectionSelected(DesktopSection.HISTORY) }
        )
    }
}

@Composable
private fun DesktopSectionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,

        modifier =
            Modifier
                .aeroGlass(
                    shape = RoundedCornerShape(999.dp),
                    glassColor =
                        if (selected) {
                            AeroColors.Accent
                        } else {
                            AeroColors.GlassSurfaceBase
                        },
                    baseAlpha = if (selected) 0.55f else 0.26f,
                    elevation = 0.dp
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AeroColors.TextPrimary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            color = AeroColors.TextPrimary,

            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Árbol de grupos (álbumes/artistas/playlists) del drill-down
 * vertical — cada entrada es (nombre, cantidad de canciones), como
 * cápsula de vidrio igual que las filas de canciones.
 */
@Composable
private fun DesktopGroupList(
    items: List<Pair<String, Int>>,
    onClick: (index: Int) -> Unit
) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        itemsIndexed(items) { index, (name, count) ->

            Row(
                verticalAlignment = Alignment.CenterVertically,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aeroGlass(
                            shape = RoundedCornerShape(14.dp),
                            glassColor = AeroColors.GlassSurfaceBase,
                            baseAlpha = 0.16f,
                            elevation = 2.dp
                        )
                        .clickable { onClick(index) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AeroColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = count.toString(),
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AeroColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DesktopBackRow(
    onBack: () -> Unit
) {

    IconButton(onClick = onBack) {

        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = AeroColors.TextPrimary
        )
    }
}
