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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.darktubbie.aeroplayer.ui.library.LibraryScreen
import com.darktubbie.aeroplayer.ui.nowplaying.NowPlayingScreen

/*
 * AudioTrack, FolderRepository y LibraryRepository viven en el
 * paquete `data` (Fase 2). El estado y la lógica de escaneo viven
 * en MainViewModel (Fase 3). La UI vive en el paquete `ui` (Fase
 * 4): LibraryScreen, AeroBackground, AlbumArt, AeroColors,
 * MiniPlayer (Fase 6) y ahora NowPlayingScreen (Fase 7).
 *
 * MainActivity solo hace lo que de verdad es responsabilidad de
 * la Activity: pedir permisos, lanzar el picker SAF, montar
 * Compose y decidir qué pantalla se ve. No se introdujo Navigation
 * Compose para esto: con dos pantallas, un enum + un `when` es
 * más simple y no añade una dependencia nueva para algo tan
 * pequeño.
 */
class MainActivity : ComponentActivity() {

    private enum class Screen {
        LIBRARY,
        NOW_PLAYING
    }

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

        if (
            checkSelfPermission(
                audioPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    audioPermission
                ),
                100
            )
        }

        enableEdgeToEdge()

        setContent {

            var screen by remember {
                mutableStateOf(Screen.LIBRARY)
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

            when {

                screen == Screen.NOW_PLAYING &&
                currentTrack != null -> {

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

                        onBack = {
                            screen = Screen.LIBRARY
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
                        }
                    )
                }

                else -> {

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

                        onPlayPauseClick = {
                            viewModel.togglePlayPause()
                        },

                        onOpenNowPlaying = {
                            screen = Screen.NOW_PLAYING
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
                        }
                    )
                }
            }
        }
    }
}
