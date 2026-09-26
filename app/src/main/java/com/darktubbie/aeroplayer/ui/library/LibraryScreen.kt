package com.darktubbie.aeroplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.Album
import com.darktubbie.aeroplayer.data.Artist
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/*
 * Pantalla de biblioteca — lo que antes era la función AeroPlayer
 * completa dentro de MainActivity.kt.
 *
 * Extraída sin rediseñar nada (Fase 4 del plan): mismo layout,
 * mismos textos, mismos tamaños. El fondo con degradado y las
 * burbujas decorativas ahora viven en AeroBackground(), y los
 * colores repetidos en AeroColors, en vez de estar hardcodeados
 * aquí.
 */
@Composable
fun LibraryScreen(
    selectedFolders: List<String>,
    tracks: List<AudioTrack>,
    displayedTracks: List<AudioTrack>,
    albums: List<Album>,
    artists: List<Artist>,
    isScanning: Boolean,
    currentTrackUri: String?,
    isPlaying: Boolean,
    libraryTab: LibraryTab,
    searchQuery: String,
    sortOrder: SortOrder,
    artistFilter: String?,
    favoritePaths: Set<String>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onScan: () -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistSelected: (String) -> Unit,
    onClearArtistFilter: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit
) {

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

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text =
                            stringResource(R.string.app_name),

                        style = MaterialTheme.typography.headlineLarge,

                        color =
                            Color.White
                    )

                    Text(
                        text =
                            stringResource(R.string.home_tagline),

                        style = MaterialTheme.typography.bodyMedium,

                        color =
                            AeroColors.OnBackgroundSubtitle
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(
                                CircleShape
                            )
                            .background(
                                AeroColors.GlassSurfaceBase.copy(
                                    alpha = 0.22f
                                )
                            )
                            .border(
                                1.dp,
                                AeroColors.GlassSurfaceBase.copy(
                                    alpha = 0.55f
                                ),
                                CircleShape
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                stringResource(R.string.settings_title),

                            tint =
                                Color.White
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(
                            RoundedCornerShape(
                                28.dp
                            )
                        )
                        .background(
                            AeroColors.GlassSurfaceBase.copy(
                                alpha = 0.38f
                            )
                        )
                        .border(
                            1.dp,
                            AeroColors.GlassSurfaceBase.copy(
                                alpha = 0.7f
                            ),
                            RoundedCornerShape(
                                28.dp
                            )
                        )
                        .padding(14.dp)
            ) {

                Column {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    stringResource(R.string.library_title),

                                style = MaterialTheme.typography.headlineMedium,

                                color =
                                    AeroColors.TextPrimary
                            )

                            Text(
                                text =
                                    if (
                                        isScanning
                                    ) {

                                        stringResource(R.string.library_scanning_short)

                                    } else if (
                                        tracks.isEmpty()
                                    ) {

                                        stringResource(R.string.library_no_songs_scanned)

                                    } else {

                                        pluralStringResource(
                                            R.plurals.library_songs_found,
                                            tracks.size,
                                            tracks.size
                                        )
                                    },

                                style = MaterialTheme.typography.bodySmall,

                                color =
                                    AeroColors.TextSecondary
                            )
                        }

                        IconButton(
                            onClick =
                                onScan,

                            enabled =
                                selectedFolders.isNotEmpty() &&
                                !isScanning
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Refresh,

                                contentDescription =
                                    stringResource(R.string.cd_refresh_library),

                                tint =
                                    AeroColors.Accent
                            )
                        }
                    }

                    if (
                        isScanning
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    30.dp
                                )
                        )

                        Column(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.LibraryMusic,

                                contentDescription =
                                    null,

                                tint =
                                    AeroColors.Accent,

                                modifier =
                                    Modifier.size(
                                        55.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )

                            Text(
                                text =
                                    stringResource(R.string.library_scanning),

                                color =
                                    AeroColors.TextPrimary,

                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Text(
                                text =
                                    stringResource(R.string.library_please_wait),

                                color =
                                    AeroColors.TextSecondary,

                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                    } else if (
                        tracks.isEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    35.dp
                                )
                        )

                        Column(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.LibraryMusic,

                                contentDescription =
                                    null,

                                tint =
                                    AeroColors.Accent,

                                modifier =
                                    Modifier.size(
                                        55.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            Text(
                                text =
                                    if (
                                        selectedFolders.isEmpty()
                                    ) {

                                        stringResource(R.string.library_select_folder_first)

                                    } else {

                                        stringResource(R.string.library_music_will_appear)
                                    },

                                color =
                                    AeroColors.TextPrimary,

                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            IconButton(
                                onClick =
                                    onScan,

                                enabled =
                                    selectedFolders.isNotEmpty()
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Refresh,

                                    contentDescription =
                                        stringResource(R.string.cd_scan_music),

                                    tint =
                                        AeroColors.Accent
                                )
                            }
                        }

                    } else {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            16.dp
                                        )
                                    )
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(
                                            alpha = 0.55f
                                        )
                                    )
                                    .padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    null,

                                tint =
                                    AeroColors.TextSecondary,

                                modifier =
                                    Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Box(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                if (searchQuery.isEmpty()) {

                                    Text(
                                        text =
                                            stringResource(R.string.library_search_hint),

                                        color =
                                            AeroColors.TextSecondary,

                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                BasicTextField(
                                    value =
                                        searchQuery,

                                    onValueChange =
                                        onSearchQueryChange,

                                    singleLine = true,

                                    textStyle =
                                        MaterialTheme.typography.bodySmall.copy(
                                            color =
                                                AeroColors.TextPrimary
                                        ),

                                    cursorBrush =
                                        SolidColor(
                                            AeroColors.Accent
                                        ),

                                    modifier =
                                        Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {

                                IconButton(
                                    onClick = {
                                        onSearchQueryChange("")
                                    },

                                    modifier =
                                        Modifier.size(28.dp)
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Close,

                                        contentDescription =
                                            stringResource(R.string.cd_clear_search),

                                        tint =
                                            AeroColors.TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            LibraryTab.entries.forEach { tab ->

                                val selected =
                                    tab == libraryTab

                                Box(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .clip(
                                                RoundedCornerShape(14.dp)
                                            )
                                            .background(
                                                if (selected) {
                                                    AeroColors.Accent.copy(
                                                        alpha = 0.18f
                                                    )
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                onTabSelected(tab)
                                            }
                                            .padding(vertical = 8.dp),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text =
                                            when (tab) {
                                                LibraryTab.SONGS -> stringResource(R.string.library_tab_songs)
                                                LibraryTab.ALBUMS -> stringResource(R.string.library_tab_albums)
                                                LibraryTab.ARTISTS -> stringResource(R.string.library_tab_artists)
                                            },

                                        color =
                                            if (selected) {
                                                AeroColors.Accent
                                            } else {
                                                AeroColors.TextSecondary
                                            },

                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        if (
                            libraryTab == LibraryTab.SONGS &&
                            artistFilter != null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(
                                            RoundedCornerShape(14.dp)
                                        )
                                        .background(
                                            AeroColors.Accent.copy(
                                                alpha = 0.15f
                                            )
                                        )
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text(
                                    text =
                                        stringResource(R.string.library_artist_filter, artistFilter ?: ""),

                                    color =
                                        AeroColors.Accent,

                                    style = MaterialTheme.typography.labelMedium,
                                )

                                Icon(
                                    imageVector =
                                        Icons.Default.Close,

                                    contentDescription =
                                        stringResource(R.string.cd_clear_artist_filter),

                                    tint =
                                        AeroColors.Accent,

                                    modifier =
                                        Modifier
                                            .size(16.dp)
                                            .clickable {
                                                onClearArtistFilter()
                                            }
                                )
                            }
                        }

                        if (libraryTab == LibraryTab.SONGS) {

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.End
                            ) {

                                SortOrderSelector(
                                    sortOrder = sortOrder,
                                    onSortOrderChange = onSortOrderChange
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        when (libraryTab) {

                            LibraryTab.SONGS -> {

                        LazyColumn(
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            items(
                                displayedTracks,

                                key = {
                                    it.uri
                                }
                            ) { track ->

                                val isCurrentTrack =
                                    track.uri ==
                                    currentTrackUri

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(
                                                RoundedCornerShape(
                                                    16.dp
                                                )
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
                                                        RoundedCornerShape(
                                                            16.dp
                                                        )
                                                    )

                                                } else {

                                                    rowModifier
                                                }
                                            }
                                            .clickable {
                                                onTrackClick(track)
                                            }
                                            .padding(
                                                10.dp
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

                                                .size(
                                                    48.dp
                                                )
                                                .clip(
                                                    RoundedCornerShape(
                                                        12.dp
                                                    )
                                                )
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                10.dp
                                            )
                                    )

                                    Column(
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    ) {

                                        Text(
                                            text =
                                                track.title,

                                            color =
                                                AeroColors.TextPrimary,

                                            style = MaterialTheme.typography.bodyMedium,

                                            maxLines =
                                                1
                                        )

                                        Text(
                                            text =
                                                track.artist,

                                            color =
                                                AeroColors.TextSecondary,

                                            style = MaterialTheme.typography.labelMedium,

                                            maxLines =
                                                1
                                        )

                                        Text(
                                            text =
                                                track.album,

                                            color =
                                                AeroColors.TextTertiary,

                                            style = MaterialTheme.typography.labelSmall,

                                            maxLines =
                                                1
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
                                                    stringResource(R.string.cd_playing)
                                                } else {
                                                    stringResource(R.string.cd_paused)
                                                },

                                            tint =
                                                AeroColors.Accent
                                        )
                                    }

                                    val isFavorite =
                                        favoritePaths.contains(
                                            track.path
                                        )

                                    IconButton(
                                        onClick = {
                                            onToggleFavorite(track)
                                        },

                                        modifier =
                                            Modifier.size(36.dp)
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
                                                    AeroColors.TextTertiary
                                                },

                                            modifier =
                                                Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onAddToPlaylist(track)
                                        },

                                        modifier =
                                            Modifier.size(36.dp)
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.PlaylistAdd,

                                            contentDescription =
                                                stringResource(R.string.playlist_add_to),

                                            tint =
                                                AeroColors.TextTertiary,

                                            modifier =
                                                Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                            }

                            LibraryTab.ALBUMS -> {

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    items(
                                        albums,

                                        key = {
                                            it.name + "|" + it.artist
                                        }
                                    ) { album ->

                                        AlbumRow(
                                            album = album,

                                            onClick = {
                                                onAlbumClick(album)
                                            }
                                        )
                                    }
                                }
                            }

                            LibraryTab.ARTISTS -> {

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    items(
                                        artists,

                                        key = {
                                            it.name
                                        }
                                    ) { artist ->

                                        ArtistRow(
                                            artist = artist,

                                            onClick = {
                                                onArtistSelected(artist.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        )
                        .background(
                            AeroColors.GlassSurfaceBase.copy(
                                alpha = 0.42f
                            )
                        )
                        .border(
                            1.dp,
                            AeroColors.GlassSurfaceBase.copy(
                                alpha = 0.65f
                            ),
                            RoundedCornerShape(
                                20.dp
                            )
                        )
                        .padding(
                            horizontal = 10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Folder,

                    contentDescription =
                        null,

                    tint =
                        AeroColors.Accent
                )

                Text(
                    text =
                        pluralStringResource(
                            R.plurals.music_folders_count,
                            selectedFolders.size,
                            selectedFolders.size
                        ),

                    color =
                        AeroColors.TextPrimary,

                    style = MaterialTheme.typography.bodyMedium,

                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 8.dp
                            )
                )

                IconButton(
                    onClick =
                        onAddFolder
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Add,

                        contentDescription =
                            stringResource(R.string.cd_add_music_folder),

                        tint =
                            AeroColors.Accent
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            /*
             * El MiniPlayer que antes vivía aquí (visible solo
             * dentro de Música/Álbumes) pasó a ser global en
             * MainActivity a partir de la Fase 2: ahora se ve desde
             * cualquier sección, no solo desde la biblioteca —
             * coherente con que el reproductor es una sección
             * principal de la app.
             */
        }
    }
}

@Composable
private fun SortOrderSelector(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box {

        Text(
            text = stringResource(R.string.library_sort_prefix, sortOrderLabel(sortOrder)),

            color = AeroColors.Accent,

            style = MaterialTheme.typography.labelMedium,

            modifier =
                Modifier.clickable {
                    expanded = true
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },

            // Fase 7 (Aero Dark UI refinement, 0.5.0): sin esto el
            // menú usaba el blanco/negro por defecto de Material —
            // pasaba desapercibido en Aero Claro pero quedaba como
            // un recuadro blanco genérico en Aero Dark, el mismo
            // tipo de problema que la zona inferior de la barra de
            // navegación. Mismo color que ya usan diálogos/hojas
            // inferiores (Sleep Timer, confirmar borrado, etc.).
            containerColor = AeroColors.DialogSurface
        ) {

            SortOrder.entries.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = sortOrderLabel(option),
                            color = AeroColors.TextPrimary
                        )
                    },

                    onClick = {
                        onSortOrderChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    onClick: () -> Unit
) {

    val firstTrack =
        album.tracks.firstOrNull()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                )
                .clickable {
                    onClick()
                }
                .padding(10.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AlbumArt(
            path = firstTrack?.path,
            artist = album.artist,
            album = album.name,

            modifier =
                Modifier
                    .size(52.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
        )

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = album.name,
                color = AeroColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )

            Text(
                text = album.artist,
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }

        Text(
            text = "${album.tracks.size}",
            color = AeroColors.TextTertiary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ArtistRow(
    artist: Artist,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                )
                .clickable {
                    onClick()
                }
                .padding(14.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            AeroColors.AlbumArtPlaceholderGradient
                        )
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White
            )
        }

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = artist.name,
                color = AeroColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )

            Text(
                text =
                    pluralStringResource(
                        R.plurals.album_count,
                        artist.albumCount,
                        artist.albumCount
                    ) +
                    " · " +
                    pluralStringResource(
                        R.plurals.song_count,
                        artist.tracks.size,
                        artist.tracks.size
                    ),

                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
