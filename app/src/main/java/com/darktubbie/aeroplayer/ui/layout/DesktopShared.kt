package com.darktubbie.aeroplayer.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.Album
import com.darktubbie.aeroplayer.data.Artist
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlinx.coroutines.delay

/**
 * Secciones de navegación de Aero Desktop (Fases 2 y 3, 0.6.0),
 * compartidas entre [AeroDesktopHorizontalScreen] (sidebar) y
 * [AeroDesktopVerticalScreen] (tabs) — puramente interno de estas
 * pantallas, no toca [com.darktubbie.aeroplayer.ui.navigation.AppDestination].
 */
internal enum class DesktopSection {
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FAVORITES,
    HISTORY
}

internal fun formatDesktopTime(ms: Long): String {

    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

/**
 * Vidrio Aero real (rediseño post-referencias visuales, Fase 2/3):
 * degradado de cuerpo + brillo especular blanco cerca de arriba —
 * la misma seña de identidad que ya usan las burbujas ambientales de
 * la app (ver [com.darktubbie.aeroplayer.ui.effects.AmbientEventShapes]),
 * adaptada de una burbuja circular a un panel/cápsula rectangular.
 */
internal fun Modifier.aeroGlass(
    shape: Shape,
    glassColor: Color,
    baseAlpha: Float = 0.30f,
    elevation: Dp = 10.dp
): Modifier =
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.25f),
            spotColor = Color.Black.copy(alpha = 0.25f)
        )
        .clip(shape)
        .background(glassColor.copy(alpha = baseAlpha))
        .background(
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.32f),
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.10f)
            )
        )
        .border(
            1.dp,
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.10f)
                    )
            ),
            shape
        )

/**
 * Encabezado de contenido (rediseño post-referencias visuales): el
 * pedido explícito de las referencias era que "el artwork no se
 * sienta como una imagen colocada arbitrariamente" y que la pantalla
 * tenga sensación de reproductor multimedia completo, no una lista
 * de Material 3 ocupando todo. Se usa tanto en Desktop horizontal
 * como vertical, arriba de la lista de cada sección: carátula/ícono
 * grande + título + subtítulo + botón de Play de toda la sección.
 *
 * [artworkPath] nulo (Songs, la vista sin drill-down de Albums/
 * Artists/Playlists, Favoritos, Historial) muestra [placeholderIcon]
 * dentro del mismo vidrio con acento en vez de una carátula real —
 * no hay una carátula única que represente "todas las canciones".
 */
@Composable
internal fun DesktopContentHeader(
    title: String,
    subtitle: String?,
    artworkPath: String?,
    artworkArtist: String,
    artworkAlbum: String,
    placeholderIcon: ImageVector,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    // Fase 8 (0.6.0): solo se pasa con un álbum seleccionado;
    // abre el Album Showcase de ese álbum.
    onOpenShowcase: (() -> Unit)? = null
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {

        if (artworkPath != null) {

            AlbumArt(
                path = artworkPath,
                artist = artworkArtist,
                album = artworkAlbum,
                highRes = true,
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
            )

        } else {

            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .aeroGlass(
                            shape = RoundedCornerShape(16.dp),
                            glassColor = AeroColors.Accent,
                            baseAlpha = 0.45f
                        ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = null,
                    tint = AeroColors.TextPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {

            if (subtitle != null) {

                Text(
                    text = subtitle,
                    color = AeroColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = title,
                color = AeroColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .aeroGlass(
                                shape = CircleShape,
                                glassColor = AeroColors.Accent,
                                baseAlpha = 0.55f,
                                elevation = 4.dp
                            )
                            .clickable(onClick = onPlayAll),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_action),
                        tint = AeroColors.TextPrimary
                    )
                }

                if (onOpenShowcase != null) {

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .aeroGlass(
                                    shape = CircleShape,
                                    glassColor = AeroColors.GlassSurfaceBase,
                                    baseAlpha = 0.45f,
                                    elevation = 4.dp
                                )
                                .clickable(onClick = onOpenShowcase),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.AlbumIcon,
                            contentDescription =
                                stringResource(R.string.cd_open_album_showcase),
                            tint = AeroColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lista de canciones central, compartida entre Desktop horizontal
 * (panel central) y Desktop vertical (contenido bajo los tabs).
 * Cada fila es su propia cápsula de vidrio con espacio entre ellas
 * (rediseño post-referencias) en vez de una lista plana de Material.
 *
 * La key combina uri + posición en la lista: Historial puede repetir
 * la misma canción varias veces (se reprodujo más de una vez), y
 * usar solo `track.uri` como key de LazyColumn crashea apenas dos
 * filas comparten uri.
 */
@Composable
internal fun DesktopTrackList(
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    onTrackClick: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {

    if (tracks.isEmpty()) {

        Text(
            text = stringResource(R.string.library_no_songs_scanned),
            color = AeroColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )

    } else {

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            itemsIndexed(
                items = tracks,
                key = { index, track -> "${track.uri}_$index" }
            ) { _, track ->

                DesktopTrackRow(
                    track = track,

                    isCurrent =
                        track.uri == currentTrack?.uri,

                    isPlaying =
                        isPlaying &&
                            track.uri == currentTrack?.uri,

                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun DesktopTrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {

    val rowShape = RoundedCornerShape(14.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,

        modifier =
            Modifier
                .fillMaxWidth()
                .aeroGlass(
                    shape = rowShape,
                    glassColor =
                        if (isCurrent) {
                            AeroColors.Accent
                        } else {
                            AeroColors.GlassSurfaceBase
                        },
                    baseAlpha = if (isCurrent) 0.30f else 0.16f,
                    elevation = if (isCurrent) 4.dp else 2.dp
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {

        AlbumArt(
            // Antes decía `track.albumArtPath`, un campo que solo
            // usa LibraryRepository para persistencia en disco, no
            // para pintar — por eso no se veía ninguna carátula
            // real, solo el placeholder.
            path = track.path,
            artist = track.artist,
            album = track.album,
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,

                color =
                    if (isCurrent) {
                        AeroColors.Accent
                    } else {
                        AeroColors.TextPrimary
                    },

                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isCurrent && isPlaying) {

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = AeroColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Barra de progreso + tiempos, con su propio estado de arrastre.
 */
@Composable
private fun DesktopSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    Column(modifier = modifier) {

        val sliderRange =
            0f..(durationMs.coerceAtLeast(1L).toFloat())

        Slider(
            value =
                if (isDragging) {
                    dragValue
                } else {
                    positionMs.toFloat()
                },

            onValueChange = { value ->
                isDragging = true
                dragValue = value
            },

            onValueChangeFinished = {
                onSeek(dragValue.toLong())
                isDragging = false
            },

            valueRange = sliderRange,

            colors =
                SliderDefaults.colors(
                    thumbColor = AeroColors.Accent,
                    activeTrackColor = AeroColors.Accent,
                    inactiveTrackColor =
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.35f)
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = formatDesktopTime(
                    if (isDragging) dragValue.toLong() else positionMs
                ),
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = formatDesktopTime(durationMs),
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Fila de controles de reproducción (shuffle/prev/play-pause/next/
 * repeat), reutilizada por las dos variantes de [NowPlayingDock].
 */
@Composable
private fun DesktopPlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    playButtonSize: Dp,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onToggleShuffle) {

            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.cd_shuffle),

                tint =
                    if (shuffleEnabled) {
                        AeroColors.Accent
                    } else {
                        AeroColors.TextSecondary
                    }
            )
        }

        IconButton(onClick = onSkipPrevious) {

            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.cd_previous),
                tint = AeroColors.TextPrimary
            )
        }

        Box(
            modifier =
                Modifier
                    .size(playButtonSize)
                    // El botón de play/pause es el elemento más
                    // mirado del dock: cuerpo teñido con el acento
                    // del tema (no gris neutro) + el mismo brillo
                    // especular de arriba, para que se sienta como
                    // una esfera Aero encendida.
                    .aeroGlass(
                        shape = CircleShape,
                        glassColor = AeroColors.Accent,
                        baseAlpha = 0.5f,
                        elevation = 6.dp
                    ),

            contentAlignment = Alignment.Center
        ) {

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

                    tint = AeroColors.TextPrimary
                )
            }
        }

        IconButton(onClick = onSkipNext) {

            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.cd_next),
                tint = AeroColors.TextPrimary
            )
        }

        IconButton(onClick = onCycleRepeatMode) {

            Icon(
                imageVector =
                    if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },

                contentDescription = stringResource(R.string.cd_repeat),

                tint =
                    if (repeatMode != Player.REPEAT_MODE_OFF) {
                        AeroColors.Accent
                    } else {
                        AeroColors.TextSecondary
                    }
            )
        }
    }
}

/**
 * Panel "Now Playing" (rediseño post-referencias visuales): en las
 * dos imágenes de referencia, el reproductor NO es un panel lateral
 * ni una tarjeta flotante — es una barra que ocupa TODO el ancho del
 * dispositivo, pegada al borde inferior, como una parte física de la
 * estación/reproductor. Por eso ahora [compact] ya no distingue
 * "panel chico vs. panel grande" sino "una sola fila ancha (Desktop
 * horizontal, con espacio de sobra)" vs. "3 filas apiladas (Desktop
 * vertical, ancho de teléfono)" — ambas son la MISMA barra inferior
 * de borde a borde, solo con el layout interno que le queda a cada
 * ancho. La forma (sin redondear las esquinas de abajo) es a
 * propósito: se ve como el borde del "dispositivo", no como una
 * tarjeta flotando en el medio de la pantalla.
 *
 * `onTick`: `positionMs`/`durationMs` no se refrescan solos —
 * [com.darktubbie.aeroplayer.MainViewModel.positionMs] solo cambia
 * cuando algo llama a `refreshPlaybackPosition()` (mismo sondeo que
 * ya hace [com.darktubbie.aeroplayer.ui.nowplaying.NowPlayingScreen]
 * con `onTick` + `delay(500)`), así que este dock hace el mismo
 * sondeo una sola vez acá para que lo hereden ambas pantallas.
 */
@Composable
internal fun NowPlayingDock(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onPlayPauseClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onTick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {

    LaunchedEffect(Unit) {

        while (true) {

            onTick()

            delay(500)
        }
    }

    val surfaceShape =
        RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )

    val surfaceModifier =
        modifier
            .aeroGlass(
                shape = surfaceShape,
                glassColor = AeroColors.GlassSurfaceBase,
                baseAlpha = 0.40f,
                elevation = 14.dp
            )
            .padding(
                horizontal = 20.dp,
                vertical = if (compact) 14.dp else 12.dp
            )

    if (compact) {

        Column(modifier = surfaceModifier) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                AlbumArt(
                    path = currentTrack?.path,
                    artist = currentTrack?.artist ?: "",
                    album = currentTrack?.album ?: "",
                    animateChanges = true,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = currentTrack?.title ?: "—",
                        color = AeroColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = currentTrack?.artist ?: "",
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            DesktopSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth()
            )

            DesktopPlaybackControls(
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onPlayPauseClick = onPlayPauseClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                playButtonSize = 44.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

    } else {

        Row(
            modifier = surfaceModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {

            AlbumArt(
                path = currentTrack?.path,
                artist = currentTrack?.artist ?: "",
                album = currentTrack?.album ?: "",
                animateChanges = true,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.width(220.dp)) {

                Text(
                    text = currentTrack?.title ?: "—",
                    color = AeroColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = currentTrack?.artist ?: "",
                    color = AeroColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            DesktopPlaybackControls(
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onPlayPauseClick = onPlayPauseClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                playButtonSize = 48.dp
            )

            Spacer(modifier = Modifier.width(24.dp))

            DesktopSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Encabezado de la sección activa, compartido por Desktop horizontal
 * y vertical (mismo título/ícono/carátula según sección y drill-down)
 * para que las dos orientaciones sean "dos formas de existir" del
 * mismo diseño y no dos interfaces sin relación.
 */
@Composable
internal fun DesktopSectionHeader(
    section: DesktopSection,
    selectedAlbum: Album?,
    selectedArtist: Artist?,
    selectedPlaylist: Playlist?,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAlbumShowcase: ((Album) -> Unit)? = null
) {

    val title: String
    val subtitle: String?
    val artworkPath: String?
    val artworkArtist: String
    val artworkAlbum: String
    val icon: ImageVector

    when (section) {

        DesktopSection.SONGS -> {
            title = stringResource(R.string.library_tab_songs)
            subtitle = null
            artworkPath = null
            artworkArtist = ""
            artworkAlbum = ""
            icon = Icons.Default.LibraryMusic
        }

        DesktopSection.ALBUMS -> {
            title = selectedAlbum?.name
                ?: stringResource(R.string.library_tab_albums)
            subtitle = selectedAlbum?.artist
            artworkPath = selectedAlbum?.tracks?.firstOrNull()?.path
            artworkArtist = selectedAlbum?.artist ?: ""
            artworkAlbum = selectedAlbum?.name ?: ""
            icon = Icons.Default.AlbumIcon
        }

        DesktopSection.ARTISTS -> {
            title = selectedArtist?.name
                ?: stringResource(R.string.library_tab_artists)
            subtitle = null
            artworkPath = selectedArtist?.tracks?.firstOrNull()?.path
            artworkArtist = selectedArtist?.name ?: ""
            artworkAlbum = ""
            icon = Icons.Default.Person
        }

        DesktopSection.PLAYLISTS -> {
            title = selectedPlaylist?.name
                ?: stringResource(R.string.playlists_title)
            subtitle = null
            artworkPath = null
            artworkArtist = ""
            artworkAlbum = ""
            icon = Icons.Default.QueueMusic
        }

        DesktopSection.FAVORITES -> {
            title = stringResource(R.string.favorites_title)
            subtitle = null
            artworkPath = null
            artworkArtist = ""
            artworkAlbum = ""
            icon = Icons.Default.Favorite
        }

        DesktopSection.HISTORY -> {
            title = stringResource(R.string.history_title)
            subtitle = null
            artworkPath = null
            artworkArtist = ""
            artworkAlbum = ""
            icon = Icons.Default.History
        }
    }

    DesktopContentHeader(
        title = title,
        subtitle = subtitle,
        artworkPath = artworkPath,
        artworkArtist = artworkArtist,
        artworkAlbum = artworkAlbum,
        placeholderIcon = icon,
        onPlayAll = onPlayAll,
        modifier = modifier,
        onOpenShowcase =
            if (
                section == DesktopSection.ALBUMS &&
                selectedAlbum != null &&
                onOpenAlbumShowcase != null
            ) {
                { onOpenAlbumShowcase(selectedAlbum) }
            } else {
                null
            }
    )
}
