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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onScan: () -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistSelected: (String) -> Unit,
    onClearArtistFilter: () -> Unit
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
                            "Aero Player",

                        fontSize =
                            30.sp,

                        color =
                            Color.White
                    )

                    Text(
                        text =
                            "Your music, your way.",

                        fontSize =
                            14.sp,

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
                                Color.White.copy(
                                    alpha = 0.22f
                                )
                            )
                            .border(
                                1.dp,
                                Color.White.copy(
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
                                "Settings",

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
                            Color.White.copy(
                                alpha = 0.38f
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(
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
                                    "Library",

                                fontSize =
                                    22.sp,

                                color =
                                    AeroColors.TextPrimary
                            )

                            Text(
                                text =
                                    if (
                                        isScanning
                                    ) {

                                        "Scanning music..."

                                    } else if (
                                        tracks.isEmpty()
                                    ) {

                                        "No songs scanned yet"

                                    } else {

                                        "${tracks.size} songs found"
                                    },

                                fontSize =
                                    13.sp,

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
                                    "Refresh library",

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
                                    "Scanning your music...",

                                color =
                                    AeroColors.TextPrimary,

                                fontSize =
                                    15.sp
                            )

                            Text(
                                text =
                                    "Please wait",

                                color =
                                    AeroColors.TextSecondary,

                                fontSize =
                                    12.sp
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

                                        "Select a music folder first"

                                    } else {

                                        "Your music will appear here"
                                    },

                                color =
                                    AeroColors.TextPrimary,

                                fontSize =
                                    15.sp
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
                                        "Scan music",

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
                                        Color.White.copy(
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
                                            "Search songs, albums, artists",

                                        color =
                                            AeroColors.TextSecondary,

                                        fontSize = 13.sp
                                    )
                                }

                                BasicTextField(
                                    value =
                                        searchQuery,

                                    onValueChange =
                                        onSearchQueryChange,

                                    singleLine = true,

                                    textStyle =
                                        TextStyle(
                                            color =
                                                AeroColors.TextPrimary,

                                            fontSize = 13.sp
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
                                            "Clear search",

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
                                                LibraryTab.SONGS -> "Songs"
                                                LibraryTab.ALBUMS -> "Albums"
                                                LibraryTab.ARTISTS -> "Artists"
                                            },

                                        color =
                                            if (selected) {
                                                AeroColors.Accent
                                            } else {
                                                AeroColors.TextSecondary
                                            },

                                        fontSize = 13.sp
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
                                        "Artist: $artistFilter",

                                    color =
                                        AeroColors.Accent,

                                    fontSize = 12.sp
                                )

                                Icon(
                                    imageVector =
                                        Icons.Default.Close,

                                    contentDescription =
                                        "Clear artist filter",

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

                                Text(
                                    text =
                                        if (
                                            sortOrder ==
                                            SortOrder.TITLE
                                        ) {
                                            "Sort: Title"
                                        } else {
                                            "Sort: Artist"
                                        },

                                    color =
                                        AeroColors.Accent,

                                    fontSize = 12.sp,

                                    modifier =
                                        Modifier.clickable {

                                            onSortOrderChange(
                                                if (
                                                    sortOrder ==
                                                    SortOrder.TITLE
                                                ) {
                                                    SortOrder.ARTIST
                                                } else {
                                                    SortOrder.TITLE
                                                }
                                            )
                                        }
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
                                                Color.White.copy(
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

                                            fontSize =
                                                14.sp,

                                            maxLines =
                                                1
                                        )

                                        Text(
                                            text =
                                                track.artist,

                                            color =
                                                AeroColors.TextSecondary,

                                            fontSize =
                                                12.sp,

                                            maxLines =
                                                1
                                        )

                                        Text(
                                            text =
                                                track.album,

                                            color =
                                                AeroColors.TextTertiary,

                                            fontSize =
                                                11.sp,

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
                                                    "Playing"
                                                } else {
                                                    "Paused"
                                                },

                                            tint =
                                                AeroColors.Accent
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
                            Color.White.copy(
                                alpha = 0.42f
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(
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
                        "${selectedFolders.size} music folder(s)",

                    color =
                        AeroColors.TextPrimary,

                    fontSize =
                        14.sp,

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
                            "Add music folder",

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
                    Color.White.copy(alpha = 0.38f)
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
                fontSize = 15.sp,
                maxLines = 1
            )

            Text(
                text = album.artist,
                color = AeroColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Text(
            text = "${album.tracks.size}",
            color = AeroColors.TextTertiary,
            fontSize = 12.sp
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
                    Color.White.copy(alpha = 0.38f)
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
                fontSize = 15.sp,
                maxLines = 1
            )

            Text(
                text =
                    "${artist.albumCount} album" +
                    (if (artist.albumCount != 1) "s" else "") +
                    " · ${artist.tracks.size} song" +
                    (if (artist.tracks.size != 1) "s" else ""),

                color = AeroColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
