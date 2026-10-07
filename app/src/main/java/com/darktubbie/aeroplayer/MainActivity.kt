package com.darktubbie.aeroplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.darktubbie.aeroplayer.ui.components.AeroRestModeOverlay
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.darktubbie.aeroplayer.ui.effects.AeroTouchEffectsLayer
import com.darktubbie.aeroplayer.ui.effects.LocalAmbientIntensity
import com.darktubbie.aeroplayer.ui.effects.aeroTouchEffects
import com.darktubbie.aeroplayer.ui.effects.rememberAeroTouchEffectsState
import com.darktubbie.aeroplayer.ui.effects.LocalAmbientPlayback
import androidx.compose.material3.MaterialTheme
import com.darktubbie.aeroplayer.ui.theme.AeroTypography
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import com.darktubbie.aeroplayer.ui.theme.AppTheme
import com.darktubbie.aeroplayer.ui.theme.LocalAeroColorScheme
import com.darktubbie.aeroplayer.ui.theme.colorScheme
import com.darktubbie.aeroplayer.ui.theme.materialColorScheme
import com.darktubbie.aeroplayer.ui.home.HomeScreen
import com.darktubbie.aeroplayer.ui.layout.AeroDesktopHorizontalScreen
import com.darktubbie.aeroplayer.ui.layout.AeroDesktopMode
import com.darktubbie.aeroplayer.ui.layout.AeroDesktopVerticalScreen
import com.darktubbie.aeroplayer.ui.layout.AeroLayoutMode
import com.darktubbie.aeroplayer.ui.layout.LocalAeroLayoutMode
import com.darktubbie.aeroplayer.ui.layout.rememberAeroLayoutMode
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
import com.darktubbie.aeroplayer.ui.album.AlbumShowcaseScreen
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
/**
 * Fase 7 (0.6.0): tiempo sin tocar la pantalla antes de que Aero
 * Rest Mode se active (con el ajuste en ON). 30s — ni tan corto que
 * moleste mientras se mira la pantalla sin tocarla (leyendo una
 * letra, viendo pasar canciones), ni tan largo que tarde en notarse.
 */
private const val AERO_REST_MODE_TIMEOUT_MS = 30_000L

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Fase 2 (0.5.0) — sistema de idiomas: envuelve el Context base
     * con una [Configuration] que fija el idioma elegido por el
     * usuario (persistido en [SettingsRepository], leído acá
     * directamente porque esto corre antes de que exista el
     * ViewModel). Por defecto "es", explícito — la app NO sigue el
     * idioma del sistema, para que el idioma mostrado sea siempre
     * el que el usuario eligió en Ajustes, no una sorpresa según el
     * teléfono en el que corra.
     *
     * `createConfigurationContext` hace que cualquier recurso
     * resuelto a través de esta Activity (y por lo tanto todo
     * `stringResource()`/`pluralStringResource()` de Compose, que
     * lee del `Context` de la Activity) salga de `res/values-en` o
     * `res/values` según corresponda, sin depender de
     * AppCompatDelegate ni de convertir la Activity en
     * AppCompatActivity.
     */
    override fun attachBaseContext(newBase: Context) {

        val languageCode =
            newBase.getSharedPreferences(
                "aero_player",
                Context.MODE_PRIVATE
            ).getString("app_language", null) ?: "es"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        super.attachBaseContext(
            newBase.createConfigurationContext(config)
        )
    }

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

        // Fase 7 (Aero Dark UI refinement, 0.5.0): se captura acá
        // (todavía dentro de onCreate, con acceso directo a `window`
        // vía `this`) para poder referenciarlo más abajo dentro del
        // lambda de setContent, que no tiene la Activity como
        // receptor implícito.
        val activityWindow = window

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

            // Fase 8 (0.6.0): Album Showcase. Se guarda la clave
            // (nombre, artista) y no el Album en sí, para que el
            // overlay siempre muestre la versión actual de la
            // biblioteca (p. ej. tras un re-escaneo).
            var openAlbumKey by remember {
                mutableStateOf<Pair<String, String>?>(null)
            }

            // Fase 7 (0.6.0): Aero Rest Mode. `lastInteractionAtMs`
            // se actualiza desde el propio Box raíz (ver más abajo,
            // PointerEventPass.Initial — observa sin consumir, así
            // que no interfiere con ningún botón/gesto normal de la
            // app) y desde el toque que sale del overlay.
            val restModeEnabled by
                viewModel.restModeEnabled

            var isResting by remember {
                mutableStateOf(false)
            }

            var lastInteractionAtMs by remember {
                mutableStateOf(System.currentTimeMillis())
            }

            // No debe empezar ya "descansando" al volver de segundo
            // plano solo porque pasó tiempo real mientras la app
            // estaba oculta — eso se sentiría como un bug, no como
            // un modo de reposo.
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {

                val observer =
                    LifecycleEventObserver { _, event ->

                        if (event == Lifecycle.Event.ON_RESUME) {
                            isResting = false
                            lastInteractionAtMs = System.currentTimeMillis()
                        }
                    }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(restModeEnabled) {

                if (!restModeEnabled) {
                    isResting = false
                    return@LaunchedEffect
                }

                while (true) {

                    delay(1000)

                    val idleForMs =
                        System.currentTimeMillis() - lastInteractionAtMs

                    if (!isResting && idleForMs >= AERO_REST_MODE_TIMEOUT_MS) {
                        isResting = true
                    }
                }
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

            // Fase 7 (Aero Dark UI refinement, 0.5.0): antes las
            // barras del sistema (navegación/status) tenían un color
            // blanco fijo en styles.xml sin importar el tema Aero
            // elegido — la causa concreta de la franja blanca que
            // quedaba abajo de todo con Aero Dark seleccionado, ya
            // que esa franja no es parte del contenido de Compose,
            // es la barra del sistema operativo. Se controla acá en
            // cada cambio de tema en vez de en el XML estático:
            // transparente en ambos (deja ver el contenido de Compose
            // detrás) y con iconos claros u oscuros según corresponda.
            LaunchedEffect(appTheme) {

                val insetsController =
                    WindowCompat.getInsetsController(
                        activityWindow,
                        activityWindow.decorView
                    )

                val isLightTheme =
                    appTheme == AppTheme.LIGHT_AERO

                insetsController.isAppearanceLightStatusBars =
                    isLightTheme

                insetsController.isAppearanceLightNavigationBars =
                    isLightTheme
            }

            val appLanguage by
                viewModel.appLanguage

            val aeroDesktopMode by
                viewModel.aeroDesktopMode

            // Fase 1 (0.6.0): todavía nadie rama sobre este valor
            // más abajo — las Fases 2 y 3 son quienes van a leer
            // LocalAeroLayoutMode.current para decidir qué pantalla
            // mostrar. Se calcula y se provee ya desde esta fase
            // para que esas fases no tengan que volver a tocar
            // MainActivity para engancharse.
            val layoutMode by
                rememberAeroLayoutMode(aeroDesktopMode)

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

                    // Fase 8 (0.6.0): tocar un álbum abre el Album
                    // Showcase (desde ahí se reproduce completo o
                    // en aleatorio); antes lo reproducía directo.
                    onAlbumClick = { album ->
                        openAlbumKey = album.name to album.artist
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
            MaterialTheme(
                typography = AeroTypography,
                colorScheme = appTheme.materialColorScheme()
            ) {

            CompositionLocalProvider(
                LocalAmbientPlayback provides
                    AmbientPlaybackInfo(
                        isPlaying = isPlaying,
                        trackPath = currentTrack?.path,
                        apeVersion = apeVersion,
                        trackArtist = currentTrack?.artist ?: "",
                        trackAlbum = currentTrack?.album ?: "",

                        getPositionMs = {
                            viewModel.currentPositionMs()
                        }
                    ),

                LocalAmbientIntensity provides ambientIntensity,

                LocalAeroColorScheme provides appTheme.colorScheme(),

                LocalAeroLayoutMode provides layoutMode
            ) {

            // Fase 10 (0.6.0): microinteracciones de toque. Tiene que
            // crearse acá adentro (necesita LocalAmbientIntensity).
            val touchEffects =
                rememberAeroTouchEffectsState()

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        // Fase 7 (Aero Dark UI refinement, 0.5.0):
                        // segunda mitad del fix de la franja
                        // inferior blanca — esta es la raíz visual de
                        // TODA la pantalla (contenido + MiniPlayer +
                        // BottomNavBar), y antes no pintaba nada
                        // detrás suyo, dejando ver el fondo blanco
                        // por defecto de la ventana de Android
                        // (heredado de Theme.Material.Light) en
                        // cualquier hueco — el margen alrededor de la
                        // barra de navegación flotante y el área de
                        // gestos del sistema en la parte de abajo,
                        // sobre todo. Con esto, ese hueco SIEMPRE
                        // tiene un color acorde al tema, sin importar
                        // qué pase con las barras del sistema.
                        .background(
                            AeroColors.BackgroundGradient.last()
                        )
                        // Fase 7 (0.6.0, Aero Rest Mode): observa
                        // CUALQUIER toque en cualquier parte de la
                        // app para reiniciar el contador de
                        // inactividad — `PointerEventPass.Initial`
                        // es clave: mira el evento sin consumirlo,
                        // así que ningún botón/gesto normal de abajo
                        // se ve afectado.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(
                                        PointerEventPass.Initial
                                    )
                                    lastInteractionAtMs =
                                        System.currentTimeMillis()
                                }
                            }
                        }
                        // Fase 10 (0.6.0): detecta TOQUES (no scroll)
                        // para la onda/burbujas; mismo criterio que
                        // Rest Mode: observa en Initial, no consume.
                        .aeroTouchEffects(touchEffects)
            ) {

            // Fase 4 (0.6.0): con `configChanges` declarado en el
            // manifest, rotar ya no recrea la Activity — Compose solo
            // recompone con la nueva orientación. Este Crossfade hace
            // que el cambio entre Mobile / Desktop horizontal /
            // Desktop vertical se sienta intencional (fundido corto)
            // en vez de un salto seco. Dentro se usa `mode` (el valor
            // que está mostrando cada capa durante la transición), no
            // `layoutMode` directamente.
            Crossfade(
                targetState = layoutMode,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(durationMillis = 350),
                label = "aeroLayoutModeTransition"
            ) { mode ->

            if (mode == AeroLayoutMode.DESKTOP_HORIZONTAL) {

                // Fase 2 (0.6.0): estos 4 valores viven acá adentro
                // (no arriba, junto a currentTrack/isPlaying) para
                // no hacer recomponer TODA la pantalla en cada tick
                // de posición cuando se está en Mobile — la interfaz
                // Mobile ya los lee igual de acotado, solo que
                // dentro de la rama REPRODUCTOR del `when` de más
                // abajo en vez de acá.
                val desktopPositionMs by
                    viewModel.positionMs

                val desktopDurationMs by
                    viewModel.durationMs

                val desktopShuffleEnabled by
                    viewModel.shuffleEnabled

                val desktopRepeatMode by
                    viewModel.repeatMode

                AeroDesktopHorizontalScreen(
                    tracks = tracks,
                    albums = albums,
                    artists = artists,
                    playlists = playlists,
                    favoriteTracks = favoriteTracks,

                    historyTracks =
                        historyTracks.map { it.first },

                    tracksInPlaylist = { playlist ->
                        viewModel.tracksInPlaylist(playlist)
                    },

                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    positionMs = desktopPositionMs,
                    durationMs = desktopDurationMs,
                    shuffleEnabled = desktopShuffleEnabled,
                    repeatMode = desktopRepeatMode,

                    // Fase 2 (0.6.0): a diferencia de Mobile, en
                    // Desktop tocar cualquier canción de cualquier
                    // sección reproduce esa lista completa como cola
                    // empezando ahí (comportamiento clásico de
                    // reproductor de escritorio) — ver
                    // MainViewModel.playFromList.
                    onTrackClick = { track, listContext ->
                        viewModel.playFromList(listContext, track)
                    },

                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                    },

                    onSkipNext = {
                        viewModel.skipNext()
                    },

                    onSkipPrevious = {
                        viewModel.skipPrevious()
                    },

                    onSeek = { ms ->
                        viewModel.seekTo(ms)
                    },

                    onToggleShuffle = {
                        viewModel.toggleShuffle()
                    },

                    onCycleRepeatMode = {
                        viewModel.cycleRepeatMode()
                    },

                    onTick = {
                        viewModel.refreshPlaybackPosition()
                    },

                    onExitDesktopMode = {
                        viewModel.setAeroDesktopMode(AeroDesktopMode.OFF)
                    },

                    onOpenAlbumShowcase = { album ->
                        openAlbumKey = album.name to album.artist
                    }
                )

            } else if (mode == AeroLayoutMode.DESKTOP_VERTICAL) {

                // Fase 3 (0.6.0): mismo motivo que en la rama
                // DESKTOP_HORIZONTAL de arriba — leídos acá adentro
                // para no recomponer de más en Mobile.
                val desktopPositionMs by
                    viewModel.positionMs

                val desktopDurationMs by
                    viewModel.durationMs

                val desktopShuffleEnabled by
                    viewModel.shuffleEnabled

                val desktopRepeatMode by
                    viewModel.repeatMode

                AeroDesktopVerticalScreen(
                    tracks = tracks,
                    albums = albums,
                    artists = artists,
                    playlists = playlists,
                    favoriteTracks = favoriteTracks,

                    historyTracks =
                        historyTracks.map { it.first },

                    tracksInPlaylist = { playlist ->
                        viewModel.tracksInPlaylist(playlist)
                    },

                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    positionMs = desktopPositionMs,
                    durationMs = desktopDurationMs,
                    shuffleEnabled = desktopShuffleEnabled,
                    repeatMode = desktopRepeatMode,

                    onTrackClick = { track, listContext ->
                        viewModel.playFromList(listContext, track)
                    },

                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                    },

                    onSkipNext = {
                        viewModel.skipNext()
                    },

                    onSkipPrevious = {
                        viewModel.skipPrevious()
                    },

                    onSeek = { ms ->
                        viewModel.seekTo(ms)
                    },

                    onToggleShuffle = {
                        viewModel.toggleShuffle()
                    },

                    onCycleRepeatMode = {
                        viewModel.cycleRepeatMode()
                    },

                    onTick = {
                        viewModel.refreshPlaybackPosition()
                    },

                    onExitDesktopMode = {
                        viewModel.setAeroDesktopMode(AeroDesktopMode.OFF)
                    },

                    onOpenAlbumShowcase = { album ->
                        openAlbumKey = album.name to album.artist
                    }
                )

            } else {

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

                            val homePositionMs by
                                viewModel.positionMs

                            val homeDurationMs by
                                viewModel.durationMs

                            val aeroPicks by
                                viewModel.aeroPicks

                            HomeScreen(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                positionMs = homePositionMs,
                                durationMs = homeDurationMs,
                                recentlyPlayed = historyTracks.map { it.first },
                                favoriteTracks = favoriteTracks,
                                playlists = playlists,

                                playlistTrackCount = { playlist ->
                                    viewModel.tracksInPlaylist(playlist).size
                                },

                                aeroPicks = aeroPicks,

                                onContinueListeningClick = {
                                    navigateTo(AppDestination.REPRODUCTOR)
                                },

                                onContinueListeningPlayPause = {
                                    viewModel.togglePlayPause()
                                },

                                onRecentlyPlayedClick = { track ->
                                    viewModel.playFromHistory(track)
                                },

                                onViewAllRecentlyPlayed = {
                                    historyScreenOpen = true
                                },

                                onFavoriteClick = { track ->

                                    if (track.uri == currentTrackUri) {
                                        viewModel.togglePlayPause()
                                    } else {
                                        viewModel.playFavorites(track)
                                    }
                                },

                                onViewAllFavorites = {
                                    favoritesScreenOpen = true
                                },

                                onPlaylistClick = { playlist ->
                                    playlistsScreenOpen = true
                                    openPlaylistId = playlist.id
                                },

                                onViewAllPlaylists = {
                                    playlistsScreenOpen = true
                                },

                                onAeroPickClick = { track ->
                                    viewModel.playFromAeroPicks(track)
                                },

                                onOpenLibrary = {
                                    viewModel.onLibraryTabSelected(
                                        LibraryTab.SONGS
                                    )
                                    navigateTo(AppDestination.MUSICA)
                                },

                                onOpenFavorites = {
                                    favoritesScreenOpen = true
                                },

                                onOpenPlaylists = {
                                    playlistsScreenOpen = true
                                },

                                onOpenFolders = {
                                    foldersScreenOpen = true
                                },

                                onOpenSettings = {
                                    settingsScreenOpen = true
                                }
                            )
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

                                // Fase 8 (0.6.0): leídos solo acá
                                // adentro, mismo criterio que
                                // positionMs: no recomponer el resto
                                // de la app al cambiar la cola.
                                val queueItems by
                                    viewModel.queueItems

                                val currentQueueIndex by
                                    viewModel.currentQueueIndex

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
                                    },

                                    queueItems = queueItems,

                                    currentQueueIndex = currentQueueIndex,

                                    onPlayQueueItem = { index ->
                                        viewModel.playQueueItem(index)
                                    },

                                    onRemoveQueueItem = { index ->
                                        viewModel.removeQueueItem(index)
                                    },

                                    onMoveQueueItem = { from, to ->
                                        viewModel.moveQueueItem(from, to)
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

            }

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

                    appLanguage = appLanguage,

                    onAppLanguageChange = { code ->
                        viewModel.setLanguage(code)
                        recreate()
                    },

                    aeroDesktopMode = aeroDesktopMode,

                    onAeroDesktopModeChange = { mode ->
                        viewModel.setAeroDesktopMode(mode)
                    },

                    restModeEnabled = restModeEnabled,

                    onRestModeEnabledChange = { enabled ->
                        viewModel.setRestModeEnabled(enabled)
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

            // Fase 8 (0.6.0): Album Showcase como overlay a pantalla
            // completa, igual que Favoritos/Playlists/Historial —
            // vive fuera del Crossfade de layouts, así que sirve
            // tal cual para Mobile y para Aero Desktop.
            openAlbumKey?.let { key ->

                val showcaseAlbum =
                    albums.firstOrNull {
                        it.name == key.first &&
                            it.artist == key.second
                    }

                if (showcaseAlbum == null) {

                    // El álbum ya no existe (p. ej. se quitó su
                    // carpeta): se cierra en vez de dejar una
                    // pantalla vacía.
                    LaunchedEffect(key) {
                        openAlbumKey = null
                    }

                } else {

                    AlbumShowcaseScreen(
                        album = showcaseAlbum,
                        currentTrackUri = currentTrackUri,

                        onPlayAlbum = { albumTracks ->
                            viewModel.playAlbumTracks(
                                albumTracks,
                                shuffle = false
                            )
                        },

                        onShuffleAlbum = { albumTracks ->
                            viewModel.playAlbumTracks(
                                albumTracks,
                                shuffle = true
                            )
                        },

                        onTrackClick = { albumTracks, index ->
                            viewModel.playAlbumTrack(
                                albumTracks,
                                index
                            )
                        },

                        onBack = {
                            openAlbumKey = null
                        }
                    )
                }
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

            // Fase 10 (0.6.0): capa de dibujo de las ondas. Va arriba
            // de todo el contenido y de los overlays, pero debajo de
            // Rest Mode; no recibe toques.
            AeroTouchEffectsLayer(touchEffects)

            // Fase 7 (0.6.0): último hijo del Box raíz a propósito
            // — tiene que quedar arriba de todo lo demás (incluidos
            // los overlays de arriba: editor .aero, hoja de Sleep
            // Timer, agregar a playlist, Historial).
            if (isResting) {

                AeroRestModeOverlay(
                    onExit = {
                        isResting = false
                        lastInteractionAtMs = System.currentTimeMillis()
                    }
                )
            }
            }
            }
            }
        }
    }
}
