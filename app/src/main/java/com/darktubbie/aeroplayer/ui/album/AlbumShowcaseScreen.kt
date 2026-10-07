package com.darktubbie.aeroplayer.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.Album
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.layout.aeroGlass
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import com.darktubbie.aeroplayer.ui.effects.LocalAmbientPlayback
import com.darktubbie.aeroplayer.ui.effects.aeroPressScale
import com.darktubbie.aeroplayer.ui.effects.isSystemReduceMotionEnabled

/**
 * Album Showcase (Fase 8, 0.6.0): vista dedicada a un álbum donde la
 * portada es el elemento principal. La portada funciona como "sleeve"
 * y un disco estilizado asoma parcialmente por detrás — es solo un
 * elemento visual (no gira de forma continua ni se controla).
 *
 * No reemplaza las vistas de álbumes existentes: es una capa nueva
 * que se abre desde ellas. Todo lo que reproduce se delega por
 * callbacks al MainViewModel/PlayerRepository ya existentes.
 *
 * El tracklist se ordena por el número de pista de los tags (disco y
 * luego pista) cuando está disponible; las canciones sin ese tag
 * conservan el orden con el que ya las entrega la biblioteca.
 */
@Composable
fun AlbumShowcaseScreen(
    album: Album,
    currentTrackUri: String?,
    onPlayAlbum: (List<AudioTrack>) -> Unit,
    onShuffleAlbum: (List<AudioTrack>) -> Unit,
    onTrackClick: (List<AudioTrack>, Int) -> Unit,
    onBack: () -> Unit
) {

    BackHandler(onBack = onBack)

    val tracks =
        remember(album) {

            // sortedWith es estable: las pistas sin número (0) van
            // al final y mantienen su orden relativo original.
            album.tracks.sortedWith(
                compareBy<AudioTrack> {
                    if (it.trackNumber > 0) 0 else 1
                }.thenBy {
                    it.trackNumber
                }
            )
        }

    val year =
        remember(album) {

            album.tracks
                .map { it.year }
                .filter { it > 0 }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        }

    val totalMs =
        remember(album) {
            album.tracks.sumOf { it.duration }
        }

    // El vinilo solo GIRA (dibujo) cuando suena una canción de ESTE
    // álbum. Es una lectura del estado de reproducción, nunca una
    // escritura: nada de lo que hace el vinilo llega al reproductor.
    val albumUris =
        remember(album) {
            album.tracks.map { it.uri }.toSet()
        }

    val spinning =
        LocalAmbientPlayback.current.isPlaying &&
            currentTrackUri in albumUris

    // Entrada sutil: la página sube y aparece; el disco se revela
    // un instante después (ver AlbumHero).
    var entered by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    AeroBackground {

        AnimatedVisibility(
            visible = entered,
            enter =
                fadeIn(tween(300)) +
                    slideInVertically(
                        animationSpec = tween(
                            350,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffsetY = { it / 14 }
                    )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
            ) {

                IconButton(
                    onClick = onBack,
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                            top = 8.dp
                        )
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription =
                            stringResource(R.string.cd_back),
                        tint = Color.White
                    )
                }

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp),

                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp
                        )
                ) {

                    item {

                        AlbumHero(
                            track = tracks.firstOrNull(),
                            albumName = album.name,
                            albumArtist = album.artist,
                            spinning = spinning
                        )
                    }

                    item {

                        AlbumInfo(
                            album = album,
                            year = year,
                            totalMs = totalMs
                        )
                    }

                    item {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),

                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            ShowcaseButton(
                                label =
                                    stringResource(
                                        R.string.showcase_play
                                    ),
                                icon = Icons.Default.PlayArrow,
                                accent = true,
                                onClick = {
                                    onPlayAlbum(tracks)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ShowcaseButton(
                                label =
                                    stringResource(
                                        R.string.showcase_shuffle
                                    ),
                                icon = Icons.Default.Shuffle,
                                accent = false,
                                onClick = {
                                    onShuffleAlbum(tracks)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    itemsIndexed(
                        tracks,
                        key = { index, track ->
                            "$index|${track.uri}"
                        }
                    ) { index, track ->

                        ShowcaseTrackRow(
                            position = index + 1,
                            track = track,
                            isCurrent =
                                track.uri == currentTrackUri,
                            onClick = {
                                onTrackClick(tracks, index)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composición física: el disco está medio guardado dentro del sleeve.
 *
 * El centro del disco queda justo sobre el borde derecho de la
 * portada, así que su mitad izquierda queda tapada por el sleeve y
 * solo la mitad derecha sobresale (~50 % visible). El disco es un
 * poco más chico que el alto de la portada, de modo que NO asoma por
 * arriba ni por abajo. Orden de profundidad: disco -> sombra que el
 * sleeve proyecta sobre él -> sleeve.
 *
 * Al abrir, el disco arranca centrado detrás de la portada (oculto
 * del todo) y se desliza hasta su sitio con un giro corto; el
 * líquido reacciona a ese movimiento y se asienta solo.
 */
@Composable
private fun AlbumHero(
    track: AudioTrack?,
    albumName: String,
    albumArtist: String,
    spinning: Boolean
) {

    val context =
        LocalContext.current

    val reduceMotion =
        remember {
            isSystemReduceMotionEnabled(context)
        }

    val vinyl =
        remember {
            VinylState(reduceMotion)
        }

    // Bucle de animación: un solo bucle por fotograma que solo
    // existe mientras algo se mueve. Con el disco quieto y sin
    // música no se pide ningún fotograma.
    LaunchedEffect(vinyl.kick, spinning) {

        var last =
            withFrameNanos { it }

        while (true) {

            val now =
                withFrameNanos { it }

            val dt =
                ((now - last) / 1_000_000_000f)
                    .coerceIn(0.001f, 0.033f)

            last = now

            vinyl.step(dt, spinning)

            if (!spinning && vinyl.isAtRest()) {
                break
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {

        val density =
            LocalDensity.current

        val sleeveSize: Dp =
            minOf(maxWidth * 0.56f, 250.dp)

        // Algo más chico que el alto del sleeve: no debe asomar por
        // arriba ni por abajo.
        val discSize: Dp =
            sleeveSize * 0.96f / VINYL_DISC_FRACTION

        val discRadius: Dp =
            discSize * VINYL_DISC_FRACTION / 2f

        // Centro del disco sobre el borde derecho del sleeve =>
        // ~50 % visible. Ancho total del conjunto = sleeve + radio.
        val groupWidth: Dp =
            sleeveSize + discRadius

        val startPadding: Dp =
            ((maxWidth - groupWidth) / 2)
                .coerceAtLeast(0.dp)

        with(density) {

            vinyl.radiusPx = discRadius.toPx()

            // Parte desde el centro del sleeve (disco oculto) hasta
            // el borde derecho.
            vinyl.entryShiftPx = (sleeveSize / 2).toPx()

            vinyl.maxOffsetPx = discRadius.toPx() * 0.22f
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(sleeveSize + 44.dp)
                    .padding(start = startPadding)
        ) {

            // 1. Disco (detrás).
            VinylDisc(
                state = vinyl,
                size = discSize,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = sleeveSize - discSize / 2)
            )

            // 2. Sombra que el sleeve proyecta sobre el disco: da la
            // lectura de que el disco entra POR DEBAJO de la portada.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = sleeveSize)
                        .width(28.dp)
                        .height(sleeveSize * 0.96f)
                        .graphicsLayer {
                            alpha = vinyl.entry
                        }
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.42f),
                                    Color.Transparent
                                )
                            )
                        )
            )

            // 3. Sleeve (delante).
            AlbumSleeve(
                track = track,
                albumName = albumName,
                albumArtist = albumArtist,
                size = sleeveSize,
                modifier =
                    Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun AlbumSleeve(
    track: AudioTrack?,
    albumName: String,
    albumArtist: String,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val shape =
        RoundedCornerShape(14.dp)

    Box(
        modifier =
            modifier
                .size(size)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(shape)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.35f),
                    shape
                )
    ) {

        AlbumArt(
            path = track?.path,
            artist = albumArtist,
            album = albumName,
            highRes = true,
            modifier = Modifier.fillMaxSize()
        )

        // Brillo diagonal de cristal sobre la portada.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.24f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.12f)
                        )
                    )
        )

        // Lomo: sombra fina en el borde izquierdo del sleeve.
        Box(
            modifier =
                Modifier
                    .width(8.dp)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
        )
    }
}

@Composable
private fun AlbumInfo(
    album: Album,
    year: Int?,
    totalMs: Long
) {

    val songs =
        pluralStringResource(
            R.plurals.song_count,
            album.tracks.size,
            album.tracks.size
        )

    val meta =
        buildList {

            if (year != null) {
                add(year.toString())
            }

            add(songs)

            if (totalMs > 0L) {
                add(formatTotalDuration(totalMs))
            }
        }.joinToString(" · ")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
    ) {

        Text(
            text = album.name,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = album.artist,
            color = AeroColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = meta,
            color = AeroColors.TextTertiary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ShowcaseButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier
                .height(52.dp)
                .aeroPressScale()
                .aeroGlass(
                    shape = RoundedCornerShape(26.dp),
                    glassColor =
                        if (accent) {
                            AeroColors.Accent
                        } else {
                            AeroColors.GlassSurfaceBase
                        },
                    baseAlpha = if (accent) 0.6f else 0.38f,
                    elevation = 4.dp
                )
                .clickable(onClick = onClick),

        horizontalArrangement =
            Arrangement.Center,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AeroColors.TextPrimary
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ShowcaseTrackRow(
    position: Int,
    track: AudioTrack,
    isCurrent: Boolean,
    onClick: () -> Unit
) {

    val shape =
        RoundedCornerShape(16.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isCurrent) {
                        AeroColors.Accent.copy(alpha = 0.26f)
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.34f)
                    }
                )
                .clickable(onClick = onClick)
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart
        ) {

            if (isCurrent) {

                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = AeroColors.Accent,
                    modifier = Modifier.size(20.dp)
                )

            } else {

                Text(
                    text = position.toString(),
                    color = AeroColors.TextTertiary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Text(
            text = track.title,
            color =
                if (isCurrent) {
                    AeroColors.Accent
                } else {
                    AeroColors.TextPrimary
                },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (track.duration > 0L) {

            Text(
                text = formatTrackDuration(track.duration),
                color = AeroColors.TextTertiary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

private fun formatTrackDuration(
    ms: Long
): String {

    val totalSeconds =
        (ms / 1000).coerceAtLeast(0L)

    return "%d:%02d".format(
        totalSeconds / 60,
        totalSeconds % 60
    )
}

private fun formatTotalDuration(
    ms: Long
): String {

    val minutes =
        (ms / 60_000L).coerceAtLeast(1L)

    return if (minutes >= 60) {
        "${minutes / 60} h ${minutes % 60} min"
    } else {
        "$minutes min"
    }
}
