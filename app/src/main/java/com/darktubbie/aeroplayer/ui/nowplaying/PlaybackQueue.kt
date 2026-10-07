package com.darktubbie.aeroplayer.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.ui.effects.aeroPressScale
import com.darktubbie.aeroplayer.ui.components.AlbumArt
import com.darktubbie.aeroplayer.ui.layout.aeroGlass
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlinx.coroutines.delay

/**
 * Una fila de la Playback Queue (Fase 8, 0.6.0).
 *
 * [index] es el índice real dentro de la playlist de Media3: es la
 * identidad de la fila (la misma canción puede estar repetida en la
 * cola) y lo que se le entrega a mover/quitar/reproducir.
 */
data class QueueItem(
    val index: Int,
    val track: AudioTrack
)

/**
 * Playback Queue: vista asociada a Now Playing (se abre con "^" y se
 * cierra con "v"), no un destino de navegación aparte. Muestra la
 * cola en orden de reproducción, resalta la canción actual y permite
 * reordenar (arrastrando el handle), quitar y saltar a una canción.
 *
 * No toca Media3 directamente: todo pasa por callbacks que terminan
 * en PlayerRepository, así que la lógica de reproducción no cambia.
 *
 * Reordenar queda deshabilitado con shuffle activo: en ese modo el
 * orden mostrado es el orden aleatorio real de Media3 y mover filas
 * ahí no tendría un significado claro.
 */
@Composable
fun PlaybackQueueScreen(
    items: List<QueueItem>,
    currentIndex: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onClose: () -> Unit,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Copia de trabajo mientras se arrastra: el reordenamiento se ve
    // al instante en la UI y recién al soltar se confirma UN solo
    // movimiento en el reproductor (evita parpadeos por el viaje de
    // ida y vuelta del MediaController en cada cruce de fila).
    var working by remember(items) {
        mutableStateOf(items)
    }

    var draggingKey by remember {
        mutableStateOf<Int?>(null)
    }

    var dragDelta by remember {
        mutableStateOf(0f)
    }

    val listState =
        rememberLazyListState()

    val canReorder =
        !shuffleEnabled

    // Al abrir, deja la canción actual a la vista.
    LaunchedEffect(Unit) {

        val position =
            items.indexOfFirst { it.index == currentIndex }

        if (position > 0) {
            listState.scrollToItem(position)
        }
    }

    /*
     * Si la fila arrastrada pasó el centro de una vecina, se
     * intercambian en la copia de trabajo y se compensa dragDelta
     * para que la fila siga pegada al dedo.
     */
    fun swapIfNeeded() {

        val key =
            draggingKey
                ?: return

        val visible =
            listState.layoutInfo.visibleItemsInfo

        val dragged =
            visible.firstOrNull { it.key == key }
                ?: return

        val center =
            dragged.offset + dragged.size / 2f + dragDelta

        val target =
            visible.firstOrNull {
                it.key != key &&
                    center >= it.offset &&
                    center <= it.offset + it.size
            }
                ?: return

        val from =
            working.indexOfFirst { it.index == key }

        val to =
            working.indexOfFirst { it.index == target.key }

        if (from < 0 || to < 0) {
            return
        }

        working =
            working.toMutableList().apply {
                add(to, removeAt(from))
            }

        dragDelta += (dragged.offset - target.offset)
    }

    // Auto-scroll mientras la fila arrastrada está cerca de un borde.
    LaunchedEffect(draggingKey) {

        if (draggingKey == null) {
            return@LaunchedEffect
        }

        while (true) {

            delay(16)

            val key =
                draggingKey
                    ?: break

            val info =
                listState.layoutInfo

            val dragged =
                info.visibleItemsInfo
                    .firstOrNull { it.key == key }
                    ?: continue

            val center =
                dragged.offset + dragged.size / 2f + dragDelta

            val edge = 120f

            val step =
                when {
                    center < info.viewportStartOffset + edge -> -18f
                    center > info.viewportEndOffset - edge -> 18f
                    else -> 0f
                }

            if (step != 0f) {

                val consumed =
                    listState.scrollBy(step)

                // La lista se movió bajo el dedo: se compensa.
                dragDelta += consumed

                swapIfNeeded()
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        AeroColors.BackgroundGradient
                    )
                )
                .statusBarsPadding()
    ) {

        // --- Encabezado: título + "v" para volver a Now Playing ---
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 16.dp,
                        top = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(R.string.queue_title),
                color = AeroColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .aeroGlass(
                            shape = CircleShape,
                            glassColor = AeroColors.GlassSurfaceBase,
                            baseAlpha = 0.45f,
                            elevation = 4.dp
                        )
                        .clickable(onClick = onClose),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription =
                        stringResource(R.string.cd_close_queue),
                    tint = AeroColors.TextPrimary
                )
            }
        }

        Text(
            text =
                pluralStringResource(
                    R.plurals.song_count,
                    items.size,
                    items.size
                ) +
                    " · " +
                    stringResource(
                        if (canReorder) {
                            R.string.queue_playing_next
                        } else {
                            R.string.queue_shuffle_order_hint
                        }
                    ),

            color = AeroColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            modifier =
                Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 10.dp
                )
        )

        // --- Lista ---
        LazyColumn(
            state = listState,

            verticalArrangement =
                Arrangement.spacedBy(8.dp),

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
        ) {

            itemsIndexed(
                working,
                key = { _, item -> item.index }
            ) { _, item ->

                val isDragged =
                    draggingKey == item.index

                QueueRow(
                    item = item,
                    isCurrent = item.index == currentIndex,
                    canReorder = canReorder,
                    isDragged = isDragged,
                    dragOffset = if (isDragged) dragDelta else 0f,

                    onClick = {
                        onPlayItem(item.index)
                    },

                    onRemove = {
                        onRemoveItem(item.index)
                    },

                    onDragStart = {
                        draggingKey = item.index
                        dragDelta = 0f
                    },

                    onDrag = { dy ->
                        dragDelta += dy
                        swapIfNeeded()
                    },

                    onDragEnd = {

                        val key = draggingKey

                        if (key != null) {

                            val finalPosition =
                                working.indexOfFirst {
                                    it.index == key
                                }

                            if (
                                finalPosition in items.indices &&
                                items[finalPosition].index != key
                            ) {
                                onMoveItem(
                                    key,
                                    items[finalPosition].index
                                )
                            }
                        }

                        draggingKey = null
                        dragDelta = 0f
                    },

                    onDragCancel = {
                        working = items
                        draggingKey = null
                        dragDelta = 0f
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }

        // --- Barra inferior: shuffle y repeat existentes ---
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            QueueControlPill(
                active = shuffleEnabled,
                description =
                    stringResource(R.string.cd_shuffle),
                onClick = onToggleShuffle,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = AeroColors.TextPrimary
                )
            }

            QueueControlPill(
                active = repeatMode != Player.REPEAT_MODE_OFF,
                description =
                    stringResource(R.string.cd_repeat),
                onClick = onCycleRepeat,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector =
                        if (repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        },
                    contentDescription = null,
                    tint = AeroColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun QueueControlPill(
    active: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(52.dp)
                .aeroPressScale()
                .aeroGlass(
                    shape = RoundedCornerShape(26.dp),
                    glassColor =
                        if (active) {
                            AeroColors.Accent
                        } else {
                            AeroColors.GlassSurfaceBase
                        },
                    baseAlpha = if (active) 0.55f else 0.35f,
                    elevation = 4.dp
                )
                .clickable(onClick = onClick),

        contentAlignment =
            Alignment.Center
    ) {

        icon()
    }
}

@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    canReorder: Boolean,
    isDragged: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {

    val shape =
        RoundedCornerShape(20.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .zIndex(if (isDragged) 1f else 0f)
                .graphicsLayer {
                    translationY = dragOffset
                    if (isDragged) {
                        scaleX = 1.02f
                        scaleY = 1.02f
                    }
                }
                .clip(shape)
                .background(
                    if (isCurrent) {
                        AeroColors.Accent.copy(alpha = 0.28f)
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                    }
                )
                .border(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color =
                        if (isCurrent) {
                            AeroColors.Accent.copy(alpha = 0.8f)
                        } else {
                            AeroColors.GlassSurfaceBase.copy(alpha = 0.5f)
                        },
                    shape = shape
                )
                .clickable(onClick = onClick)
                .padding(
                    start = 10.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 2.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            AlbumArt(
                path = item.track.path,
                artist = item.track.artist,
                album = item.track.album,
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
            )

            if (isCurrent) {

                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = AeroColors.Accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = item.track.title,
                color =
                    if (isCurrent) {
                        AeroColors.Accent
                    } else {
                        AeroColors.TextPrimary
                    },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.track.artist,
                color = AeroColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onRemove
        ) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription =
                    stringResource(R.string.cd_remove_from_queue),
                tint = AeroColors.TextSecondary
            )
        }

        // Handle de reordenar: el arrastre vive SOLO acá, para que
        // el resto de la fila siga siendo "tocar para reproducir" y
        // la lista siga haciendo scroll normal.
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .alpha(if (canReorder) 1f else 0.35f)
                    .then(
                        if (canReorder) {
                            Modifier.pointerInput(item.index) {
                                detectDragGestures(
                                    onDragStart = { onDragStart() },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount.y)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragCancel() }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription =
                    stringResource(R.string.cd_reorder_queue),
                tint = AeroColors.TextSecondary
            )
        }
    }
}
