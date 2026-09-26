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
import com.darktubbie.aeroplayer.data.FavoritesRepository
import com.darktubbie.aeroplayer.data.FolderRepository
import com.darktubbie.aeroplayer.data.HistoryEntry
import com.darktubbie.aeroplayer.data.HistoryRepository
import com.darktubbie.aeroplayer.data.LibraryGrouping
import com.darktubbie.aeroplayer.data.LibraryRepository
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.data.PlaylistRepository
import com.darktubbie.aeroplayer.data.SettingsRepository
import com.darktubbie.aeroplayer.data.trackFingerprint
import com.darktubbie.aeroplayer.playback.PlayerRepository
import com.darktubbie.aeroplayer.playback.SleepTimerMode
import com.darktubbie.aeroplayer.playback.SleepTimerState
import com.darktubbie.aeroplayer.ui.effects.AmbientIntensity
import com.darktubbie.aeroplayer.ui.library.LibraryTab
import com.darktubbie.aeroplayer.ui.library.SortOrder
import com.darktubbie.aeroplayer.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val favoritesRepository =
        FavoritesRepository(application)

    private val playlistRepository =
        PlaylistRepository(application)

    private val historyRepository =
        HistoryRepository(application)

    private val settingsRepository =
        SettingsRepository(application)

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

    private val _sleepTimerState =
        mutableStateOf(SleepTimerState())

    val sleepTimerState: State<SleepTimerState>
        get() = _sleepTimerState

    private var sleepTimerJob: Job? = null

    private val _favoritePaths =
        mutableStateOf<Set<String>>(emptySet())

    val favoritePaths: State<Set<String>>
        get() = _favoritePaths

    private val _playlists =
        mutableStateOf<List<Playlist>>(emptyList())

    val playlists: State<List<Playlist>>
        get() = _playlists

    private val _historyEntries =
        mutableStateOf<List<HistoryEntry>>(emptyList())

    private val _ambientIntensity =
        mutableStateOf(AmbientIntensity.NORMAL)

    val ambientIntensity: State<AmbientIntensity>
        get() = _ambientIntensity

    private val _appTheme =
        mutableStateOf(AppTheme.LIGHT_AERO)

    val appTheme: State<AppTheme>
        get() = _appTheme

    /**
     * Idioma de la interfaz (Fase 2, 0.5.0): "es" o "en". A
     * diferencia de [_appTheme], no se restaura en un bloque del
     * `init` de más abajo porque [com.darktubbie.aeroplayer.MainActivity.attachBaseContext]
     * ya necesita leer este mismo valor de [SettingsRepository]
     * *antes* de que este ViewModel exista, así que se inicializa
     * acá directo con el mismo default ("es") para que ambos
     * lugares queden siempre de acuerdo.
     */
    private val _appLanguage =
        mutableStateOf(
            settingsRepository.loadLanguageCode() ?: "es"
        )

    val appLanguage: State<String>
        get() = _appLanguage

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

            sortTracks(
                result,
                _sortOrder.value
            )
        }

    /**
     * Canciones marcadas como favoritas, ordenadas con el mismo
     * criterio elegido para Songs (Fase 2, 0.4.x). Se recalcula
     * sola si cambian las pistas, los favoritos guardados o el
     * orden — no depende de búsqueda/filtro de artista, a
     * diferencia de [displayedTracks].
     *
     * Si una canción favorita se elimina del almacenamiento, deja
     * de aparecer en `_tracks` en el próximo escaneo y por lo
     * tanto también desaparece de aquí sola, sin lógica extra.
     */
    val favoriteTracks: State<List<AudioTrack>> =
        derivedStateOf {

            sortTracks(
                _tracks.value.filter {
                    _favoritePaths.value.contains(it.path)
                },
                _sortOrder.value
            )
        }

    /**
     * Ordenamiento compartido entre [displayedTracks] y
     * [favoriteTracks] (Fase 2, 0.4.x) — antes vivía duplicado
     * inline dentro de cada derivedStateOf.
     */
    private fun sortTracks(
        list: List<AudioTrack>,
        order: SortOrder
    ): List<AudioTrack> {

        return when (order) {

            SortOrder.TITLE ->
                list.sortedBy {
                    it.title.lowercase()
                }

            SortOrder.ARTIST ->
                list.sortedBy {
                    it.artist.lowercase()
                }

            SortOrder.ALBUM ->
                list.sortedBy {
                    it.album.lowercase()
                }

            SortOrder.DURATION ->
                list.sortedBy {
                    it.duration
                }

            SortOrder.DATE_NEWEST ->
                list.sortedByDescending {
                    it.dateModifiedMs
                }

            SortOrder.DATE_OLDEST ->
                list.sortedBy {
                    it.dateModifiedMs
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

        _favoritePaths.value =
            favoritesRepository.loadFavoritePaths()

        _playlists.value =
            playlistRepository.loadPlaylists()

        _historyEntries.value =
            historyRepository.loadHistory()

        settingsRepository.loadAmbientIntensityName()
            ?.let { name ->

                runCatching {
                    AmbientIntensity.valueOf(name)
                }.getOrNull()
                    ?.let { restored ->
                        _ambientIntensity.value = restored
                    }
            }

        settingsRepository.loadThemeName()
            ?.let { name ->

                runCatching {
                    AppTheme.valueOf(name)
                }.getOrNull()
                    ?.let { restored ->
                        _appTheme.value = restored
                    }
            }

        // Fase 1 (0.4.x): restaura el orden elegido la última vez.
        // Si no hay nada guardado (instalación previa a esta fase),
        // se mantiene el valor por defecto (TITLE) del State.
        libraryRepository.loadSortOrderName()
            ?.let { name ->

                runCatching {
                    SortOrder.valueOf(name)
                }.getOrNull()
                    ?.let { restored ->
                        _sortOrder.value = restored
                    }
            }

        // Fase 5 (0.4.x): shuffle/repeat guardados, para que
        // PlayerRepository los aplique si la conexión resulta ser
        // a un ExoPlayer recién creado (ver connect()).
        playerRepository.pendingShuffleRestore =
            settingsRepository.isShuffleEnabled() to
                settingsRepository.getRepeatMode()

        playerRepository.connect()

        playerRepository.onTrackAutoAdvanced = {
            onTrackAutoAdvanced()
        }

        playerRepository.onTrackLeftBehind = { trackUri, playedMs ->
            onTrackLeftBehind(trackUri, playedMs)
        }
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

        settingsRepository.setShuffleEnabled(
            playerRepository.shuffleEnabled.value
        )
    }

    fun cycleRepeatMode() {

        playerRepository.cycleRepeatMode()

        settingsRepository.setRepeatMode(
            playerRepository.repeatMode.value
        )
    }

    /**
     * Cambia la intensidad de efectos ambientales (Fase 5, 0.4.x)
     * y la persiste. El nuevo valor llega a [MidgroundLayer][
     * com.darktubbie.aeroplayer.ui.effects.MidgroundLayer] a través
     * de [com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity],
     * que MainActivity provee leyendo este mismo State.
     */
    fun setAmbientIntensity(
        intensity: AmbientIntensity
    ) {

        _ambientIntensity.value = intensity

        settingsRepository.saveAmbientIntensityName(
            intensity.name
        )
    }

    /**
     * Cambia el tema (Aero claro / Dark Aero, post-0.4.0) y lo
     * persiste. Igual que la intensidad ambiental, el nuevo valor
     * llega a toda la app a través de un CompositionLocal
     * ([com.darktubbie.aeroplayer.ui.theme.LocalAeroColorScheme])
     * que MainActivity provee leyendo este mismo State.
     */
    fun setAppTheme(
        theme: AppTheme
    ) {

        _appTheme.value = theme

        settingsRepository.saveThemeName(
            theme.name
        )
    }

    /**
     * Solo persiste — no recrea la Activity. Eso lo hace
     * [com.darktubbie.aeroplayer.MainActivity] después de llamar a
     * esto, porque es la única que tiene la referencia a sí misma
     * necesaria para `recreate()`.
     */
    fun setLanguage(
        code: String
    ) {

        _appLanguage.value = code

        settingsRepository.saveLanguageCode(code)
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

        libraryRepository.saveSortOrderName(
            order.name
        )
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

    /**
     * Índice fingerprint -> AudioTrack (Fase 3, 0.4.x), para
     * resolver el contenido de una playlist en O(1) por canción en
     * vez de recorrer toda la biblioteca por cada entrada de cada
     * playlist. Se recalcula solo cuando cambian las pistas.
     */
    private val tracksByFingerprint: State<Map<String, AudioTrack>> =
        derivedStateOf {

            _tracks.value.associateBy {
                trackFingerprint(it)
            }
        }

    /**
     * Canciones reales de una playlist, en su orden, resolviendo
     * cada fingerprint contra la biblioteca actual (Fase 3, 0.4.x).
     *
     * Si una canción de la playlist ya no está en la biblioteca
     * (se borró el archivo, o se le editaron los tags y su
     * fingerprint cambió), simplemente se omite — igual que
     * Favoritos, sin lógica de "canción rota" adicional.
     */
    fun tracksInPlaylist(
        playlist: Playlist
    ): List<AudioTrack> {

        val index =
            tracksByFingerprint.value

        return playlist.trackKeys.mapNotNull {
            index[it]
        }
    }

    /**
     * Historial resuelto contra la biblioteca actual, más reciente
     * primero (Fase 4, 0.4.x).
     *
     * Si una canción del historial ya no está en la biblioteca (se
     * borró el archivo, o cambiaron sus tags y su fingerprint ya no
     * coincide), esa entrada se omite sola — mismo criterio que
     * Favoritos/Playlists, sin lógica de "entrada rota" adicional.
     */
    val historyTracks: State<List<Pair<AudioTrack, Long>>> =
        derivedStateOf {

            val index =
                tracksByFingerprint.value

            _historyEntries.value.mapNotNull { entry ->

                index[entry.trackKey]?.let { track ->
                    track to entry.playedAtMs
                }
            }
        }

    /**
     * Llamado por [PlayerRepository.onTrackLeftBehind] cada vez que
     * se deja atrás una pista (Fase 4, 0.4.x).
     *
     * Umbral para que cuente como "canción reproducida": al menos
     * 30 segundos escuchados, o la mitad de la duración total si la
     * canción dura menos de un minuto (para que un tema muy corto
     * no necesite escucharse casi entero para figurar en el
     * historial). 30 segundos es el mismo orden de magnitud que
     * usan otros reproductores para distinguir "la escuchó" de
     * "la tocó sin querer y pasó a la siguiente".
     */
    private fun onTrackLeftBehind(
        trackUri: String,
        playedMs: Long
    ) {

        val track =
            _tracks.value.find {
                it.uri == trackUri
            } ?: return

        val threshold =
            minOf(30_000L, track.duration / 2)

        if (playedMs >= threshold) {

            _historyEntries.value =
                historyRepository.addEntry(
                    trackFingerprint(track),
                    System.currentTimeMillis(),
                    _historyEntries.value
                )
        }
    }

    /**
     * Reproduce una canción del historial de forma individual (no
     * como parte de una cola con el resto del historial): tocar
     * una entrada vieja es "quiero escuchar esto de nuevo", no
     * "quiero seguir el orden cronológico hacia atrás".
     */
    fun playFromHistory(
        track: AudioTrack
    ) {

        playerRepository.playQueue(
            listOf(track),
            0
        )
    }

    fun clearHistory() {

        _historyEntries.value =
            historyRepository.clearHistory()
    }

    fun clearArtistFilter() {
        _artistFilter.value = null
    }

    /*
     * ---------------------------------------------------------
     * FAVORITOS (Fase 2, 0.4.x)
     * ---------------------------------------------------------
     */

    fun isFavorite(
        track: AudioTrack
    ): Boolean {

        return _favoritePaths.value.contains(
            track.path
        )
    }

    fun toggleFavorite(
        track: AudioTrack
    ) {

        _favoritePaths.value =
            favoritesRepository.toggleFavorite(
                track.path,
                _favoritePaths.value
            )
    }

    /**
     * Reproduce toda la lista de favoritos como cola, empezando
     * por [track] si se toca una en particular (por ejemplo desde
     * FavoritesScreen), o desde el principio si no se especifica
     * ninguna (botón "reproducir todos").
     */
    fun playFavorites(
        startingFrom: AudioTrack? = null
    ) {

        val list =
            favoriteTracks.value

        if (list.isEmpty()) {
            return
        }

        val startIndex =
            startingFrom
                ?.let { list.indexOf(it) }
                ?.takeIf { it != -1 }
                ?: 0

        playerRepository.playQueue(
            list,
            startIndex
        )
    }

    /*
     * ---------------------------------------------------------
     * PLAYLISTS (Fase 3, 0.4.x)
     * ---------------------------------------------------------
     */

    fun createPlaylist(
        name: String
    ) {

        _playlists.value =
            playlistRepository.createPlaylist(
                name,
                _playlists.value
            )
    }

    /**
     * Crea una playlist nueva con [track] ya adentro — usado por
     * el botón "Nueva playlist" del selector "Agregar a playlist"
     * (Fase 3, 0.4.x), que crea y agrega en el mismo gesto.
     */
    fun createPlaylistWithTrack(
        name: String,
        track: AudioTrack
    ) {

        val updated =
            playlistRepository.createPlaylist(
                name,
                _playlists.value
            )

        val created =
            updated.lastOrNull()
                ?: run {
                    _playlists.value = updated
                    return
                }

        _playlists.value =
            playlistRepository.addTrack(
                created.id,
                trackFingerprint(track),
                updated
            )
    }

    fun renamePlaylist(
        playlist: Playlist,
        newName: String
    ) {

        _playlists.value =
            playlistRepository.renamePlaylist(
                playlist.id,
                newName,
                _playlists.value
            )
    }

    fun deletePlaylist(
        playlist: Playlist
    ) {

        _playlists.value =
            playlistRepository.deletePlaylist(
                playlist.id,
                _playlists.value
            )
    }

    /**
     * Devuelve true si [track] ya está en [playlist] — usado por
     * el selector "Agregar a playlist" para mostrar qué playlists
     * ya contienen la canción tocada.
     */
    fun playlistContainsTrack(
        playlist: Playlist,
        track: AudioTrack
    ): Boolean {

        return playlist.trackKeys.contains(
            trackFingerprint(track)
        )
    }

    /**
     * Alterna [track] dentro de [playlist]: la agrega si no
     * estaba, la quita si ya estaba. Es el mismo gesto de
     * "toggle" que ya se usa para favoritos, aplicado por
     * playlist en vez de a una única colección global.
     */
    fun togglePlaylistTrack(
        playlist: Playlist,
        track: AudioTrack
    ) {

        val key =
            trackFingerprint(track)

        _playlists.value =
            if (playlistContainsTrack(playlist, track)) {

                playlistRepository.removeTrack(
                    playlist.id,
                    key,
                    _playlists.value
                )

            } else {

                playlistRepository.addTrack(
                    playlist.id,
                    key,
                    _playlists.value
                )
            }
    }

    fun removeTrackFromPlaylist(
        playlist: Playlist,
        track: AudioTrack
    ) {

        _playlists.value =
            playlistRepository.removeTrack(
                playlist.id,
                trackFingerprint(track),
                _playlists.value
            )
    }

    /**
     * Mueve una canción una posición dentro de la playlist. [delta]
     * es -1 (subir) o +1 (bajar); fuera de rango no hace nada.
     *
     * Alternativa deliberadamente más simple que drag-and-drop
     * completo: para el volumen de canciones típico de una
     * playlist local, mover de a una posición por toque es
     * suficiente y evita sumar una librería de reordenamiento
     * solo para esto.
     */
    fun movePlaylistTrack(
        playlist: Playlist,
        track: AudioTrack,
        delta: Int
    ) {

        val key =
            trackFingerprint(track)

        val currentIndex =
            playlist.trackKeys.indexOf(key)

        val targetIndex =
            currentIndex + delta

        if (
            currentIndex == -1 ||
            targetIndex !in playlist.trackKeys.indices
        ) {
            return
        }

        val reordered =
            playlist.trackKeys.toMutableList()

        reordered.removeAt(currentIndex)
        reordered.add(targetIndex, key)

        _playlists.value =
            playlistRepository.reorderTracks(
                playlist.id,
                reordered,
                _playlists.value
            )
    }

    /**
     * Reproduce toda la playlist como cola, empezando por
     * [startingFrom] si se especifica o desde el principio.
     */
    fun playPlaylist(
        playlist: Playlist,
        startingFrom: AudioTrack? = null
    ) {

        val list =
            tracksInPlaylist(playlist)

        if (list.isEmpty()) {
            return
        }

        val startIndex =
            startingFrom
                ?.let { list.indexOf(it) }
                ?.takeIf { it != -1 }
                ?: 0

        playerRepository.playQueue(
            list,
            startIndex
        )
    }

    /*
     * ---------------------------------------------------------
     * AERO PICKS (Fase 3, roadmap 0.5.0 — Aero Home)
     * ---------------------------------------------------------
     *
     * Recomendaciones 100% locales: nada de IA, servidores ni
     * internet, solo señales que ya existen en el dispositivo
     * (Historial, Favoritos y Playlists). Cada aparición de una
     * canción en esas colecciones suma puntos a su artista y a su
     * álbum — Favoritos pesa el doble que Historial/Playlists por
     * ser una señal explícita del usuario, mientras que el
     * Historial puede sumar varias veces al mismo artista si lo
     * escucha seguido, que es justamente la "frecuencia" que pide
     * el brief.
     *
     * Se excluyen las canciones ya favoritas (para no repetir la
     * sección Favorites de Home) y solo entran candidatas con
     * puntaje mayor a 0: si la biblioteca todavía no tiene
     * historial, favoritos ni playlists, la lista queda vacía a
     * propósito — HomeScreen muestra un estado vacío en vez de
     * inventar contenido.
     */
    val aeroPicks: State<List<AudioTrack>> =
        derivedStateOf {

            val artistScore =
                mutableMapOf<String, Int>()

            val albumScore =
                mutableMapOf<String, Int>()

            fun addSignal(
                track: AudioTrack,
                weight: Int
            ) {

                artistScore[track.artist] =
                    (artistScore[track.artist] ?: 0) + weight

                albumScore[track.album] =
                    (albumScore[track.album] ?: 0) + weight
            }

            val index =
                tracksByFingerprint.value

            _historyEntries.value.forEach { entry ->

                index[entry.trackKey]?.let {
                    addSignal(it, 1)
                }
            }

            favoriteTracks.value.forEach {
                addSignal(it, 2)
            }

            _playlists.value.forEach { playlist ->

                playlist.trackKeys.forEach { key ->

                    index[key]?.let {
                        addSignal(it, 1)
                    }
                }
            }

            val favoritePathSet =
                _favoritePaths.value

            _tracks.value
                .filter {
                    !favoritePathSet.contains(it.path)
                }
                .map { track ->

                    track to (
                        (artistScore[track.artist] ?: 0) * 2 +
                            (albumScore[track.album] ?: 0)
                    )
                }
                .filter {
                    it.second > 0
                }
                .sortedWith(
                    compareByDescending<Pair<AudioTrack, Int>> {
                        it.second
                    }.thenBy {
                        it.first.title
                    }
                )
                .take(10)
                .map {
                    it.first
                }
        }

    /**
     * Reproduce una canción tocada desde Aero Picks de forma
     * individual, igual que [playFromHistory]: no usa
     * [displayedTracks] como cola porque esa lista respeta la
     * búsqueda/orden/filtro de artista activos en Música, que no
     * tienen por qué coincidir con lo que se ve en Home.
     */
    fun playFromAeroPicks(
        track: AudioTrack
    ) {

        playerRepository.playQueue(
            listOf(track),
            0
        )
    }

    /*
     * ---------------------------------------------------------
     * SLEEP TIMER (Fase 1, 0.4.x)
     * ---------------------------------------------------------
     *
     * Vive en el ViewModel (no en un Composable de Now Playing)
     * para que siga corriendo aunque el usuario navegue a otra
     * pantalla, tal como pide el brief. viewModelScope se cancela
     * solo si el ViewModel se destruye, lo cual en la práctica
     * significa "la app se cerró", momento en el que de todos
     * modos ya no tiene sentido seguir contando.
     */

    /**
     * Inicia el modo "por tiempo": pausa cuando pasan [minutes]
     * minutos. Cancela cualquier temporizador previo, sea del
     * mismo modo o del otro.
     */
    fun startSleepTimerByMinutes(
        minutes: Int
    ) {

        cancelSleepTimer()

        val totalMs =
            minutes * 60_000L

        _sleepTimerState.value =
            SleepTimerState(
                mode = SleepTimerMode.ByTime(totalMs),
                remainingMs = totalMs
            )

        sleepTimerJob =
            viewModelScope.launch {

                var remaining = totalMs

                while (remaining > 0) {

                    delay(1_000)

                    remaining -= 1_000

                    _sleepTimerState.value =
                        _sleepTimerState.value.copy(
                            remainingMs =
                                remaining.coerceAtLeast(0L)
                        )
                }

                playerRepository.pause()

                _sleepTimerState.value =
                    SleepTimerState()
            }
    }

    /**
     * Inicia el modo "por número de canciones": pausa después de
     * que terminen [songCount] canciones (ver
     * [PlayerRepository.onTrackAutoAdvanced] para qué cuenta como
     * "una canción terminada"). No usa una corrutina de espera
     * activa — el conteo avanza reactivamente en
     * [onTrackAutoAdvanced] cada vez que el reproductor lo notifica.
     */
    fun startSleepTimerBySongs(
        songCount: Int
    ) {

        cancelSleepTimer()

        _sleepTimerState.value =
            SleepTimerState(
                mode = SleepTimerMode.BySongs(songCount),
                remainingSongs = songCount
            )
    }

    /**
     * Cancela el temporizador activo, sea cual sea su modo, sin
     * pausar la reproducción.
     */
    fun cancelSleepTimer() {

        sleepTimerJob?.cancel()

        sleepTimerJob = null

        _sleepTimerState.value =
            SleepTimerState()
    }

    /**
     * Llamado por [PlayerRepository.onTrackAutoAdvanced] cada vez
     * que una canción termina de sonar por sí sola. Solo actúa si
     * el modo activo es [SleepTimerMode.BySongs] — en modo por
     * tiempo o sin temporizador no hace nada.
     */
    private fun onTrackAutoAdvanced() {

        val state =
            _sleepTimerState.value

        val mode =
            state.mode

        if (mode !is SleepTimerMode.BySongs) {
            return
        }

        val remaining =
            state.remainingSongs - 1

        if (remaining <= 0) {

            playerRepository.pause()

            _sleepTimerState.value =
                SleepTimerState()

        } else {

            _sleepTimerState.value =
                state.copy(
                    remainingSongs = remaining
                )
        }
    }

    override fun onCleared() {

        sleepTimerJob?.cancel()

        playerRepository.release()

        super.onCleared()
    }
}
