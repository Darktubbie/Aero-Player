package com.darktubbie.aeroplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.ape.ApeEditorScreen
import com.darktubbie.aeroplayer.ui.components.AddToPlaylistSheet
import com.darktubbie.aeroplayer.ui.components.MiniPlayer
import com.darktubbie.aeroplayer.ui.effects.AmbientPlaybackInfo
import com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity
import com.darktubbie.aeroplayer.ui.effects.LocalAmbientPlayback
import com.darktubbie.aeroplayer.ui.theme.LocalAeroColorScheme
import com.darktubbie.aeroplayer.ui.theme.colorScheme
import com.darktubbie.aeroplayer.ui.home.HomeScreen
import com.darktubbie.aeroplayer.ui.library.LibraryScreen
import com.darktubbie.aeroplayer.ui.library.LibraryTab
import com.darktubbie.aeroplayer.ui.more.AddTracksToPlaylistScreen
import com.darktubbie.aeroplayer.ui.more.FavoritesScreen
import com.darktubbie.aeroplayer.ui.more.FoldersScreen
import com.darktubbie.aeroplayer.ui.more.HistoryScreen
import com.darktubbie.aeroplayer.ui.more.MoreScreen
import com.darktubbie.aeroplayer.ui.more.PlaylistDetailScreen
import com.darktubbie.aeroplayer.ui.more.PlaylistsScreen
import com.darktubbie.aeroplayer.ui.more.SettingsScreen
import com.darktubbie.aeroplayer.ui.navigation.AppDestination
import com.darktubbie.aeroplayer.ui.navigation.BottomNavBar
import com.darktubbie.aeroplayer.ui.nowplaying.EmptyNowPlayingPlaceholder
import com.darktubbie.aeroplayer.ui.nowplaying.NowPlayingScreen
import com.darktubbie.aeroplayer.ui.nowplaying.SleepTimerSheet

/*
 * AudioTrack, FolderRepository y LibraryRepository viven en el
 * paquete `data`. El estado y la lógica de escaneo viven en
 * MainViewModel. La UI vive en el paquete `ui`: LibraryScreen,
 * AeroBackground, AlbumArt, AeroColors, MiniPlayer, NowPlayingScreen
 * y, desde la Fase 1 del plan de evolución visual, HomeScreen,
 * MoreScreen y la navegación real en ui/navigation.
 *
 * MainActivity solo hace lo que de verdad es responsabilidad de
 * la Activity: pedir permisos, lanzar el picker SAF, montar
 * Compose y decidir qué pantalla se ve. No se introdujo Navigation
 * Compose para esto: con 5 destinos fijos y sin deep links ni back
 * stack complejo, un enum + un `when` sigue siendo más simple y no
 * añade una dependencia nueva.
 *
 * MUSICA y ALBUMES reutilizan LibraryScreen tal cual ya existía
 * (con sus propias pestañas internas Songs/Albums/Artists): al
 * seleccionar cualquiera de esos dos destinos en la barra inferior
 * simplemente se le pide a MainViewModel que active el LibraryTab
 * correspondiente. No se duplicó ni se reescribió LibraryScreen.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val folderPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                viewModel.addFolder(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * READ_MEDIA_AUDIO solo existe desde API 33.
         * En API 26-32 (minSdk 26) el permiso equivalente
         * para consultar MediaStore es READ_EXTERNAL_STORAGE.
         */
        val audioPermission =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                Manifest.permission.READ_MEDIA_AUDIO

            } else {

                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        /*
         * Fase 5 (0.4.x): POST_NOTIFICATIONS también es un permiso
         * runtime desde API 33 (antes, cualquier app podía mostrar
         * notificaciones sin pedir nada). Sin este permiso, la
         * notificación de reproducción que Media3 genera solo a
         * partir de la MediaSession (ver PlaybackService) queda
         * creada pero el sistema no la muestra — los controles de
         * notificación parecían "no funcionar" en Android 13+
         * cuando en realidad nunca se había pedido el permiso.
         * Pendiente detectado en la auditoría de la Fase 0.
         */
        val missingPermissions =
            mutableListOf(audioPermission)

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            missingPermissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        val permissionsToRequest =
            missingPermissions.filter {

                checkSelfPermission(it) !=
                    PackageManager.PERMISSION_GRANTED
            }

        if (permissionsToRequest.isNotEmpty()) {

            requestPermissions(
                permissionsToRequest.toTypedArray(),
                100
            )
        }

        enableEdgeToEdge()

        setContent {

            var destination by remember {
                mutableStateOf(AppDestination.INICIO)
            }

            var previousDestination by remember {
                mutableStateOf(AppDestination.INICIO)
            }

            var apeEditorOpen by remember {
                mutableStateOf(false)
            }

            var apeVersion by remember {
                mutableStateOf(0)
            }

            var foldersScreenOpen by remember {
                mutableStateOf(false)
            }

            var sleepTimerSheetOpen by remember {
                mutableStateOf(false)
            }

            var settingsScreenOpen by remember {
                mutableStateOf(false)
            }

            var favoritesScreenOpen by remember {
                mutableStateOf(false)
            }

            var playlistsScreenOpen by remember {
                mutableStateOf(false)
            }

            var openPlaylistId by remember {
                mutableStateOf<String?>(null)
            }

            var addTracksToPlaylistOpen by remember {
                mutableStateOf(false)
            }

            var addToPlaylistTrack by remember {
                mutableStateOf<AudioTrack?>(null)
            }

            var historyScreenOpen by remember {
                mutableStateOf(false)
            }

            fun navigateTo(
                target: AppDestination
            ) {

                if (target != destination) {

                    previousDestination = destination
                    destination = target
                }
            }

            val selectedFolders by
                viewModel.selectedFolders

            val tracks by
                viewModel.tracks

            val isScanning by
                viewModel.isScanning

            val currentTrackUri by
                viewModel.currentTrackUri

            val isPlaying by
                viewModel.isPlaying

            val currentTrack =
                remember(
                    tracks,
                    currentTrackUri
                ) {

                    tracks.firstOrNull {
                        it.uri == currentTrackUri
                    }
                }

            val displayedTracks by
                viewModel.displayedTracks

            val albums by
                viewModel.albums

            val artists by
                viewModel.artists

            val libraryTab by
                viewModel.libraryTab

            val searchQuery by
                viewModel.searchQuery

            val sortOrder by
                viewModel.sortOrder

            val artistFilter by
                viewModel.artistFilter

            val sleepTimerState by
                viewModel.sleepTimerState

            val favoritePaths by
                viewModel.favoritePaths

            val favoriteTracks by
                viewModel.favoriteTracks

            val playlists by
                viewModel.playlists

            val historyTracks by
                viewModel.historyTracks

            val ambientIntensity by
                viewModel.ambientIntensity

            val appTheme by
                viewModel.appTheme

            /*
             * LibraryScreen es una sola función que ya sabe
             * mostrar Songs/Albums/Artists según libraryTab (sus
             * propias pestañas internas siguen ahí, sin tocar). Se
             * reutiliza tal cual tanto para MUSICA como para
             * ALBUMES, en vez de duplicarla o partirla — lo único
             * que cambia entre esos dos destinos es qué LibraryTab
             * queda activo al llegar (ver el onSelect de
             * BottomNavBar más abajo).
             */
            @Composable
            fun libraryScreenContent() {

                LibraryScreen(
                    selectedFolders = selectedFolders,
                    tracks = tracks,
                    displayedTracks = displayedTracks,
                    albums = albums,
                    artists = artists,
                    isScanning = isScanning,
                    currentTrackUri = currentTrackUri,
                    isPlaying = isPlaying,
                    libraryTab = libraryTab,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    artistFilter = artistFilter,
                    favoritePaths = favoritePaths,

                    onAddFolder = {
                        folderPicker.launch(null)
                    },

                    onRemoveFolder = { folder ->
                        viewModel.removeFolder(folder)
                    },

                    onScan = {
                        viewModel.scanMusic()
                    },

                    onTrackClick = { track ->
                        viewModel.playTrack(track)
                    },

                    onTabSelected = { tab ->
                        viewModel.onLibraryTabSelected(tab)
                    },

                    onSearchQueryChange = { query ->
                        viewModel.onSearchQueryChange(query)
                    },

                    onSortOrderChange = { order ->
                        viewModel.onSortOrderChange(order)
                    },

                    onAlbumClick = { album ->
                        viewModel.playAlbum(album)
                    },

                    onArtistSelected = { artistName ->
                        viewModel.selectArtist(artistName)
                    },

                    onClearArtistFilter = {
                        viewModel.clearArtistFilter()
                    },

                    onToggleFavorite = { track ->
                        viewModel.toggleFavorite(track)
                    },

                    onAddToPlaylist = { track ->
                        addToPlaylistTrack = track
                    }
                )
            }

            /*
             * Fase 7: el sistema de efectos ambientales (Midground/
             * Foreground) necesita saber si hay música sonando y,
             * cuando la hay, poder leer la posición actual y
             * resolver el .ape de la pista — sin que cada pantalla
             * (Inicio, Más, etc.) tenga que recibir y reenviar esos
             * datos manualmente. Se provee una sola vez aquí.
             */
            CompositionLocalProvider(
                LocalAmbientPlayback provides
                    AmbientPlaybackInfo(
                        isPlaying = isPlaying,
                        trackPath = currentTrack?.path,
                        apeVersion = apeVersion,

                        getPositionMs = {
                            viewModel.currentPositionMs()
                        }
                    ),

                LocalAmbientIntensity provides ambientIntensity,

                LocalAeroColorScheme provides appTheme.colorScheme()
            ) {

            Box(
                modifier =
                    Modifier.fillMaxSize()
            ) {

            Column(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                ) {

                    when (destination) {

                        AppDestination.INICIO -> {
                            HomeScreen()
                        }

                        AppDestination.MUSICA -> {
                            libraryScreenContent()
                        }

                        AppDestination.ALBUMES -> {
                            libraryScreenContent()
                        }

                        AppDestination.REPRODUCTOR -> {

                            if (currentTrack != null) {

                                val positionMs by
                                    viewModel.positionMs

                                val durationMs by
                                    viewModel.durationMs

                                val shuffleEnabled by
                                    viewModel.shuffleEnabled

                                val repeatMode by
                                    viewModel.repeatMode

                                NowPlayingScreen(
                                    track = currentTrack,
                                    isPlaying = isPlaying,
                                    positionMs = positionMs,
                                    durationMs = durationMs,
                                    shuffleEnabled = shuffleEnabled,
                                    repeatMode = repeatMode,
                                    isFavorite =
                                        favoritePaths.contains(
                                            currentTrack.path
                                        ),

                                    onBack = {
                                        destination =
                                            previousDestination
                                    },

                                    onPlayPauseClick = {
                                        viewModel.togglePlayPause()
                                    },

                                    onNext = {
                                        viewModel.skipNext()
                                    },

                                    onPrevious = {
                                        viewModel.skipPrevious()
                                    },

                                    onSeek = { position ->
                                        viewModel.seekTo(position)
                                    },

                                    onToggleShuffle = {
                                        viewModel.toggleShuffle()
                                    },

                                    onCycleRepeat = {
                                        viewModel.cycleRepeatMode()
                                    },

                                    onTick = {
                                        viewModel.refreshPlaybackPosition()
                                    },

                                    onOpenApeEditor = {
                                        apeEditorOpen = true
                                    },

                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(
                                            currentTrack
                                        )
                                    },

                                    onAddToPlaylist = {
                                        addToPlaylistTrack = currentTrack
                                    },

                                    sleepTimerState = sleepTimerState,

                                    onOpenSleepTimer = {
                                        sleepTimerSheetOpen = true
                                    }
                                )

                            } else {

                                EmptyNowPlayingPlaceholder()
                            }
                        }

                        AppDestination.MAS -> {

                            MoreScreen(
                                onOpenFolders = {
                                    foldersScreenOpen = true
                                },

                                onOpenSettings = {
                                    settingsScreenOpen = true
                                },

                                onOpenFavorites = {
                                    favoritesScreenOpen = true
                                },

                                onOpenPlaylists = {
                                    playlistsScreenOpen = true
                                },

                                onOpenHistory = {
                                    historyScreenOpen = true
                                }
                            )
                        }
                    }
                }

                /*
                 * MiniPlayer global (Fase 2): visible desde
                 * cualquier sección salvo Reproductor, donde ya se
                 * ve la pantalla completa de Now Playing. Antes
                 * vivía solo dentro de LibraryScreen (Música/
                 * Álbumes); ahora acompaña la navegación completa,
                 * consistente con que el reproductor pasa a ser una
                 * sección principal de la app.
                 */
                if (
                    currentTrack != null &&
                    destination != AppDestination.REPRODUCTOR
                ) {

                    MiniPlayer(
                        track = currentTrack,
                        isPlaying = isPlaying,

                        onPlayPauseClick = {
                            viewModel.togglePlayPause()
                        },

                        onOpenNowPlaying = {
                            navigateTo(AppDestination.REPRODUCTOR)
                        },

                        modifier =
                            Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            )
                    )
                }

                BottomNavBar(
                    current = destination,

                    onSelect = { target ->

                        when (target) {

                            AppDestination.MUSICA -> {
                                viewModel.onLibraryTabSelected(
                                    LibraryTab.SONGS
                                )
                            }

                            AppDestination.ALBUMES -> {
                                viewModel.onLibraryTabSelected(
                                    LibraryTab.ALBUMS
                                )
                            }

                            else -> {
                                // Sin acción extra para el resto
                                // de destinos.
                            }
                        }

                        navigateTo(target)
                    }
                )
            }

            if (apeEditorOpen && currentTrack != null) {

                ApeEditorScreen(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    positionMs = viewModel.positionMs.value,
                    durationMs = viewModel.durationMs.value,
                    selectedFolders = selectedFolders,

                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                    },

                    onSeek = { position ->
                        viewModel.seekTo(position)
                    },

                    onSaved = {
                        apeVersion += 1
                    },

                    onBack = {
                        apeEditorOpen = false
                    }
                )
            }

            if (foldersScreenOpen) {

                FoldersScreen(
                    selectedFolders = selectedFolders,

                    onAddFolder = {
                        folderPicker.launch(null)
                    },

                    onRemoveFolder = { folder ->
                        viewModel.removeFolder(folder)
                    },

                    onBack = {
                        foldersScreenOpen = false
                    }
                )
            }

            if (sleepTimerSheetOpen) {

                SleepTimerSheet(
                    state = sleepTimerState,

                    onSelectMinutes = { minutes ->
                        viewModel.startSleepTimerByMinutes(minutes)
                        sleepTimerSheetOpen = false
                    },

                    onSelectSongs = { count ->
                        viewModel.startSleepTimerBySongs(count)
                        sleepTimerSheetOpen = false
                    },

                    onCancel = {
                        viewModel.cancelSleepTimer()
                        sleepTimerSheetOpen = false
                    },

                    onDismiss = {
                        sleepTimerSheetOpen = false
                    }
                )
            }

            if (settingsScreenOpen) {

                SettingsScreen(
                    sortOrder = sortOrder,

                    onSortOrderChange = { order ->
                        viewModel.onSortOrderChange(order)
                    },

                    ambientIntensity = ambientIntensity,

                    onAmbientIntensityChange = { intensity ->
                        viewModel.setAmbientIntensity(intensity)
                    },

                    appTheme = appTheme,

                    onAppThemeChange = { theme ->
                        viewModel.setAppTheme(theme)
                    },

                    onOpenFolders = {
                        settingsScreenOpen = false
                        foldersScreenOpen = true
                    },

                    onBack = {
                        settingsScreenOpen = false
                    }
                )
            }

            if (favoritesScreenOpen) {

                FavoritesScreen(
                    tracks = favoriteTracks,
                    currentTrackUri = currentTrackUri,
                    isPlaying = isPlaying,

                    onTrackClick = { track ->

                        if (track.uri == currentTrackUri) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playFavorites(track)
                        }
                    },

                    onPlayAll = {
                        viewModel.playFavorites()
                    },

                    onToggleFavorite = { track ->
                        viewModel.toggleFavorite(track)
                    },

                    onBack = {
                        favoritesScreenOpen = false
                    }
                )
            }

            if (playlistsScreenOpen) {

                val openPlaylist =
                    openPlaylistId?.let { id ->
                        playlists.find { it.id == id }
                    }

                when {

                    openPlaylist != null &&
                        addTracksToPlaylistOpen -> {

                        AddTracksToPlaylistScreen(
                            playlistName = openPlaylist.name,
                            allTracks = tracks,

                            isTrackInPlaylist = { track ->
                                viewModel.playlistContainsTrack(
                                    openPlaylist,
                                    track
                                )
                            },

                            onToggleTrack = { track ->
                                viewModel.togglePlaylistTrack(
                                    openPlaylist,
                                    track
                                )
                            },

                            onBack = {
                                addTracksToPlaylistOpen = false
                            }
                        )
                    }

                    openPlaylist != null -> {

                        PlaylistDetailScreen(
                            playlist = openPlaylist,

                            tracks =
                                viewModel.tracksInPlaylist(
                                    openPlaylist
                                ),

                            currentTrackUri = currentTrackUri,
                            isPlaying = isPlaying,

                            onTrackClick = { track ->
                                viewModel.playPlaylist(
                                    openPlaylist,
                                    track
                                )
                            },

                            onPlayAll = {
                                viewModel.playPlaylist(openPlaylist)
                            },

                            onMoveTrack = { track, delta ->
                                viewModel.movePlaylistTrack(
                                    openPlaylist,
                                    track,
                                    delta
                                )
                            },

                            onRemoveTrack = { track ->
                                viewModel.removeTrackFromPlaylist(
                                    openPlaylist,
                                    track
                                )
                            },

                            onRename = { newName ->
                                viewModel.renamePlaylist(
                                    openPlaylist,
                                    newName
                                )
                            },

                            onDeletePlaylist = {
                                viewModel.deletePlaylist(openPlaylist)
                                openPlaylistId = null
                            },

                            onOpenAddTracks = {
                                addTracksToPlaylistOpen = true
                            },

                            onBack = {
                                openPlaylistId = null
                            }
                        )
                    }

                    else -> {

                        PlaylistsScreen(
                            playlists = playlists,

                            trackCountFor = { playlist ->
                                viewModel.tracksInPlaylist(
                                    playlist
                                ).size
                            },

                            onCreatePlaylist = { name ->
                                viewModel.createPlaylist(name)
                            },

                            onOpenPlaylist = { playlist ->
                                openPlaylistId = playlist.id
                            },

                            onDeletePlaylist = { playlist ->
                                viewModel.deletePlaylist(playlist)
                            },

                            onBack = {
                                playlistsScreenOpen = false
                                openPlaylistId = null
                                addTracksToPlaylistOpen = false
                            }
                        )
                    }
                }
            }

            addToPlaylistTrack?.let { track ->

                AddToPlaylistSheet(
                    track = track,
                    playlists = playlists,

                    isTrackInPlaylist = { playlist ->
                        viewModel.playlistContainsTrack(
                            playlist,
                            track
                        )
                    },

                    onTogglePlaylist = { playlist ->
                        viewModel.togglePlaylistTrack(
                            playlist,
                            track
                        )
                    },

                    onCreatePlaylistWithTrack = { name ->
                        viewModel.createPlaylistWithTrack(
                            name,
                            track
                        )
                    },

                    onDismiss = {
                        addToPlaylistTrack = null
                    }
                )
            }

            if (historyScreenOpen) {

                HistoryScreen(
                    entries = historyTracks,

                    onTrackClick = { track ->
                        viewModel.playFromHistory(track)
                    },

                    onClearHistory = {
                        viewModel.clearHistory()
                    },

                    onBack = {
                        historyScreenOpen = false
                    }
                )
            }
            }
            }
        }
    }
}
