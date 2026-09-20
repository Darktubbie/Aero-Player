package com.darktubbie.aeroplayer

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darktubbie.aeroplayer.data.Album
import com.darktubbie.aeroplayer.data.Artist
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.FolderRepository
import com.darktubbie.aeroplayer.data.LibraryGrouping
import com.darktubbie.aeroplayer.data.LibraryRepository
import com.darktubbie.aeroplayer.playback.PlayerRepository
import com.darktubbie.aeroplayer.ui.library.LibraryTab
import com.darktubbie.aeroplayer.ui.library.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla principal.
 *
 * Fase 3 del plan: el estado que antes vivía directamente en
 * MainActivity (selectedFolders, tracks, isScanning) se mueve
 * aquí, y el escaneo pasa de un Thread crudo a una corrutina en
 * viewModelScope: cancelable automáticamente si el ViewModel se
 * destruye, en vez de seguir corriendo en segundo plano sin
 * control.
 *
 * Fase 5 del plan: se añade PlayerRepository (Media3) como pieza
 * nueva y aislada. Sigue siendo un solo ViewModel compartido, no
 * uno por pantalla — con el tamaño actual del proyecto separar
 * LibraryViewModel/PlayerViewModel sería una capa extra sin
 * beneficio real (ver Sección D del informe).
 *
 * Es AndroidViewModel (no ViewModel a secas) porque los
 * repositorios necesitan un Context de aplicación para
 * SharedPreferences, ContentResolver y la conexión a
 * PlaybackService.
 */
class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val folderRepository =
        FolderRepository(application)

    private val libraryRepository =
        LibraryRepository(application)

    private val playerRepository =
        PlayerRepository(application)

    private val _selectedFolders =
        mutableStateOf<List<String>>(emptyList())

    val selectedFolders: State<List<String>>
        get() = _selectedFolders

    private val _tracks =
        mutableStateOf<List<AudioTrack>>(emptyList())

    val tracks: State<List<AudioTrack>>
        get() = _tracks

    private val _isScanning =
        mutableStateOf(false)

    val isScanning: State<Boolean>
        get() = _isScanning

    val currentTrackUri: State<String?>
        get() = playerRepository.currentTrackUri

    val isPlaying: State<Boolean>
        get() = playerRepository.isPlaying

    val positionMs: State<Long>
        get() = playerRepository.positionMs

    val durationMs: State<Long>
        get() = playerRepository.durationMs

    val shuffleEnabled: State<Boolean>
        get() = playerRepository.shuffleEnabled

    val repeatMode: State<Int>
        get() = playerRepository.repeatMode

    private val _libraryTab =
        mutableStateOf(LibraryTab.SONGS)

    val libraryTab: State<LibraryTab>
        get() = _libraryTab

    private val _searchQuery =
        mutableStateOf("")

    val searchQuery: State<String>
        get() = _searchQuery

    private val _sortOrder =
        mutableStateOf(SortOrder.TITLE)

    val sortOrder: State<SortOrder>
        get() = _sortOrder

    private val _artistFilter =
        mutableStateOf<String?>(null)

    val artistFilter: State<String?>
        get() = _artistFilter

    /*
     * Derivadas de _tracks (+ búsqueda/orden/filtro): solo se
     * recalculan cuando alguno de esos valores realmente cambia,
     * no en cada recomposición de la pantalla — por ejemplo,
     * escribir en la barra de búsqueda no dispara un nuevo cálculo
     * de `albums` ni `artists` a menos que también cambien.
     */

    val displayedTracks: State<List<AudioTrack>> =
        derivedStateOf {

            var result = _tracks.value

            _artistFilter.value?.let { artist ->

                result =
                    result.filter {
                        it.artist == artist
                    }
            }

            val query =
                _searchQuery.value.trim()

            if (query.isNotEmpty()) {

                result =
                    result.filter { track ->

                        track.title.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true) ||
                        track.album.contains(query, ignoreCase = true)
                    }
            }

            when (_sortOrder.value) {

                SortOrder.TITLE ->
                    result.sortedBy {
                        it.title.lowercase()
                    }

                SortOrder.ARTIST ->
                    result.sortedBy {
                        it.artist.lowercase()
                    }
            }
        }

    val albums: State<List<Album>> =
        derivedStateOf {

            val allAlbums =
                LibraryGrouping.groupByAlbum(
                    _tracks.value
                )

            val query =
                _searchQuery.value.trim()

            if (query.isEmpty()) {

                allAlbums

            } else {

                allAlbums.filter {

                    it.name.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
                }
            }
        }

    val artists: State<List<Artist>> =
        derivedStateOf {

            val allArtists =
                LibraryGrouping.groupByArtist(
                    _tracks.value
                )

            val query =
                _searchQuery.value.trim()

            if (query.isEmpty()) {

                allArtists

            } else {

                allArtists.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }

    init {

        _selectedFolders.value =
            folderRepository.loadFolders()

        // Carga la biblioteca guardada inmediatamente.
        _tracks.value =
            libraryRepository.loadTracks()

        playerRepository.connect()
    }

    fun addFolder(
        uri: Uri
    ) {

        _selectedFolders.value =
            folderRepository.addFolder(
                uri,
                _selectedFolders.value
            )
    }

    fun removeFolder(
        folder: String
    ) {

        _selectedFolders.value =
            folderRepository.removeFolder(
                folder,
                _selectedFolders.value
            )

        // La biblioteca se vuelve a consultar
        // cuando el usuario pulse Refresh.
        _tracks.value = emptyList()

        libraryRepository.saveTracks(
            _tracks.value
        )
    }

    fun scanMusic() {

        if (
            _isScanning.value ||
            _selectedFolders.value.isEmpty()
        ) {
            return
        }

        _isScanning.value = true

        val folders =
            _selectedFolders.value.toList()

        /*
         * viewModelScope se cancela automáticamente cuando el
         * ViewModel se destruye (a diferencia del Thread crudo
         * anterior, que seguía corriendo aunque la pantalla ya
         * no estuviera visible).
         */
        viewModelScope.launch(
            Dispatchers.IO
        ) {

            val folderPaths =
                folderRepository.resolveFolderPaths(
                    folders
                )

            val foundTracks =
                libraryRepository.queryMediaStore(
                    folderPaths
                )

            _tracks.value = foundTracks

            // Guarda inmediatamente el resultado.
            libraryRepository.saveTracks(
                foundTracks
            )

            _isScanning.value = false
        }
    }

    /**
     * Reproduce [track] usando la lista actualmente visible
     * (respetando búsqueda/orden/filtro de artista) como cola,
     * empezando en esa canción. Si ya es la canción sonando,
     * simplemente alterna play/pause en vez de reiniciarla.
     */
    fun playTrack(
        track: AudioTrack
    ) {

        val visibleTracks =
            displayedTracks.value

        val startIndex =
            visibleTracks.indexOf(track)

        if (startIndex == -1) {
            return
        }

        playerRepository.playQueue(
            visibleTracks,
            startIndex
        )
    }

    fun togglePlayPause() {
        playerRepository.togglePlayPause()
    }

    fun skipNext() {
        playerRepository.skipNext()
    }

    fun skipPrevious() {
        playerRepository.skipPrevious()
    }

    fun seekTo(
        positionMs: Long
    ) {
        playerRepository.seekTo(positionMs)
    }

    /**
     * Llamado periódicamente por Now Playing mientras está en
     * pantalla, para mantener la barra de progreso al día (ver
     * comentario en PlayerRepository.refreshPosition).
     */
    fun refreshPlaybackPosition() {
        playerRepository.refreshPosition()
    }

    /**
     * Ver [PlayerRepository.currentPositionMs] — usado por el
     * sistema de Aero Player Effects (Fase 7) para sondear la
     * posición sin depender de la pantalla Now Playing.
     */
    fun currentPositionMs(): Long =
        playerRepository.currentPositionMs()

    fun toggleShuffle() {
        playerRepository.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerRepository.cycleRepeatMode()
    }

    fun onSearchQueryChange(
        query: String
    ) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(
        order: SortOrder
    ) {
        _sortOrder.value = order
    }

    fun onLibraryTabSelected(
        tab: LibraryTab
    ) {
        _libraryTab.value = tab
    }

    /**
     * Reproduce el álbum completo como cola, empezando por su
     * primera pista.
     */
    fun playAlbum(
        album: Album
    ) {

        if (album.tracks.isEmpty()) {
            return
        }

        playerRepository.playQueue(
            album.tracks,
            0
        )
    }

    /**
     * Filtra la pestaña Songs a un artista concreto (tocado desde
     * la pestaña Artists) y cambia a esa pestaña.
     */
    fun selectArtist(
        artistName: String
    ) {

        _artistFilter.value = artistName

        _libraryTab.value = LibraryTab.SONGS
    }

    fun clearArtistFilter() {
        _artistFilter.value = null
    }

    override fun onCleared() {

        playerRepository.release()

        super.onCleared()
    }
}
