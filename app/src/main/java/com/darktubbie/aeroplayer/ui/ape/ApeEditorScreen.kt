package com.darktubbie.aeroplayer.ui.ape

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.ape.ApeEvent
import com.darktubbie.aeroplayer.ape.ApeRepository
import com.darktubbie.aeroplayer.ape.ApeSaveResult
import com.darktubbie.aeroplayer.ape.ApeWriter
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.EffectAssetRepository
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.effects.AmbientEventType
import com.darktubbie.aeroplayer.ui.effects.ambientEventTypeEmoji
import com.darktubbie.aeroplayer.ui.effects.ambientEventTypeFromKey
import com.darktubbie.aeroplayer.ui.effects.ambientEventTypeKey
import com.darktubbie.aeroplayer.ui.effects.ambientEventTypeLabel
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import kotlinx.coroutines.launch

/**
 * Editor básico de Aero Player Effects (Fase 8 del plan de
 * evolución visual).
 *
 * Flujo, tal como lo pidió el brief original:
 * 1. La canción ya viene seleccionada (es la que está sonando —
 *    se abre desde Now Playing, no hace falta un selector aparte).
 * 2. Esta pantalla ES el editor.
 * 3. Reproducir/pausar: reutiliza el transporte real de la app
 *    ([onPlayPauseClick]/[onSeek]), no un reproductor aparte.
 * 4. "Seleccionar un momento" = la posición actual de reproducción.
 * 5-6. Elegir un efecto integrado + su duración.
 * 7. Guardar el `.ape` (ver [ApeWriter]).
 *
 * Solo efectos integrados (bubble/fish/jellyfish/cloud/leaf, este
 * último agregado en la Fase 6 de ".aero" 0.5.0) — un editor de
 * efectos personalizados es explícitamente una fase futura, no
 * esto.
 */
@Composable
fun ApeEditorScreen(
    track: AudioTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    selectedFolders: List<String>,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var events by
        remember(track.path) {
            mutableStateOf<List<ApeEvent>>(emptyList())
        }

    var loaded by
        remember(track.path) {
            mutableStateOf(false)
        }

    LaunchedEffect(track.path) {

        loaded = false

        events =
            ApeRepository.get(track.path)?.effects
                ?: emptyList()

        loaded = true
    }

    var selectedEffect by
        remember {
            mutableStateOf("bubble")
        }

    var durationSeconds by
        remember {
            mutableStateOf(4)
        }

    var saving by
        remember {
            mutableStateOf(false)
        }

    var statusMessage by
        remember {
            mutableStateOf<String?>(null)
        }

    var statusIsError by
        remember {
            mutableStateOf(false)
        }

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(onClick = onBack) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White
                    )
                }

                Column(
                    modifier =
                        Modifier.padding(start = 4.dp)
                ) {

                    Text(
                        text = stringResource(R.string.ape_editor_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Text(
                        text = track.title,
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = formatTime(positionMs),
                        color = AeroColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    IconButton(onClick = onPlayPauseClick) {

                        Icon(
                            imageVector =
                                if (isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },

                            contentDescription =
                                if (isPlaying) stringResource(R.string.cd_pause_action) else stringResource(R.string.cd_play_action),

                            tint = AeroColors.Accent
                        )
                    }

                    Text(
                        text = formatTime(durationMs),
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Slider(
                    value =
                        positionMs.toFloat().coerceAtMost(
                            durationMs.coerceAtLeast(1L).toFloat()
                        ),

                    valueRange =
                        0f..durationMs.coerceAtLeast(1L).toFloat(),

                    onValueChange = {
                        onSeek(it.toLong())
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard {

                Text(
                    text =
                        stringResource(R.string.ape_add_effect_at, formatTime(positionMs)),

                    color = AeroColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),

                    // Fix (Fase 6, 0.5.0): con 5 opciones (bubble/
                    // fish/jellyfish/cloud/leaf) ya no entraban
                    // cómodas en el ancho de la tarjeta — un Row
                    // común no las achica, las desborda, y por eso
                    // la última (hojas) se veía cortada/aplastada
                    // contra el borde. Con scroll horizontal quedan
                    // a su tamaño normal y se desliza para ver las
                    // que no entran.
                    modifier =
                        Modifier.horizontalScroll(
                            rememberScrollState()
                        )
                ) {

                    // Fase 6 de ".aero" (0.5.0) — mejor organización:
                    // antes eran 4 EffectOption(...) casi idénticos
                    // hardcodeados a mano (uno por tipo). Ahora es un
                    // loop sobre AmbientEventType.entries — agregar un
                    // tipo nuevo (como LEAF en esta misma fase) ya no
                    // requiere copiar/pegar un bloque acá.
                    for (type in AmbientEventType.entries) {

                        val key =
                            ambientEventTypeKey(type)

                        EffectOption(
                            emoji = ambientEventTypeEmoji(type),
                            label = ambientEventTypeLabel(type),
                            selected = selectedEffect == key,
                            onClick = { selectedEffect = key }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CustomAssetSection(
                    selectedEffect = selectedEffect
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = stringResource(R.string.ape_duration_label),
                        color = AeroColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    IconButton(
                        onClick = {

                            if (durationSeconds > 1) {
                                durationSeconds -= 1
                            }
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.cd_less_duration),
                            tint = AeroColors.TextPrimary
                        )
                    }

                    Text(
                        text = stringResource(R.string.ape_duration_seconds, durationSeconds),
                        color = AeroColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(32.dp)
                    )

                    IconButton(
                        onClick = {

                            if (durationSeconds < 15) {
                                durationSeconds += 1
                            }
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_more_duration),
                            tint = AeroColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassButton(
                    text = stringResource(R.string.ape_add_effect_here),

                    onClick = {

                        events =
                            events +
                            ApeEvent(
                                time = positionMs,
                                effect = selectedEffect,
                                duration =
                                    durationSeconds * 1000L
                            )

                        statusMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ape_events_count, events.size),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(
                    events.sortedBy { it.time }
                ) { event ->

                    EventRow(
                        event = event,

                        onDelete = {

                            events =
                                events - event

                            statusMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassButton(
                text =
                    if (saving) stringResource(R.string.ape_saving) else stringResource(R.string.ape_save_button),

                enabled = !saving,

                onClick = {

                    saving = true
                    statusMessage = null

                    scope.launch {

                        val result =
                            ApeWriter.save(
                                context = context,
                                audioPath = track.path,
                                selectedFolders = selectedFolders,
                                events = events
                            )

                        saving = false

                        when (result) {

                            is ApeSaveResult.Success -> {
                                statusIsError = false
                                statusMessage =
                                    context.getString(
                                        R.string.ape_saved_at,
                                        result.path
                                    )

                                onSaved()
                            }

                            is ApeSaveResult.Error -> {
                                statusIsError = true
                                statusMessage = result.message
                            }
                        }
                    }
                }
            )

            statusMessage?.let { message ->

                Text(
                    text = message,

                    color =
                        if (statusIsError) {
                            Color(0xFFFF8A80)
                        } else {
                            AeroColors.Accent
                        },

                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!loaded) {

                Text(
                    text = stringResource(R.string.ape_loading_events),
                    color = AeroColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.34f))
                .border(
                    1.dp,
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),

        content = content
    )
}

@Composable
private fun GlassButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled) {
                        AeroColors.Accent.copy(alpha = 0.85f)
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.25f)
                    }
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 12.dp),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Recurso personalizado del efecto seleccionado (Fase 9 del plan de
 * evolución visual): reemplaza el emoji/silueta integrada de ese
 * tipo por una imagen elegida por el usuario, en todas las
 * canciones (es una preferencia global, no de este `.ape`).
 *
 * Solo imágenes estáticas por ahora — GIF animado queda para un
 * editor de efectos más completo, explícitamente fuera de esta
 * fase.
 */
@Composable
private fun CustomAssetSection(
    selectedEffect: String
) {

    val context = LocalContext.current

    val assetRepository =
        remember(context) {
            EffectAssetRepository(context)
        }

    var assetVersion by
        remember {
            mutableStateOf(0)
        }

    val currentAssetPath =
        remember(selectedEffect, assetVersion) {
            assetRepository.getAssetPath(selectedEffect)
        }

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                assetRepository.setAsset(
                    selectedEffect,
                    uri
                )

                assetVersion += 1
            }
        }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text =
                if (currentAssetPath != null) {
                    stringResource(R.string.ape_custom_image_active)
                } else {
                    stringResource(R.string.ape_using_builtin_icon)
                },

            color = AeroColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = {
                imagePicker.launch("image/*")
            }
        ) {

            Icon(
                imageVector = Icons.Default.Image,
                contentDescription =
                    stringResource(R.string.ape_choose_custom_image),
                tint = AeroColors.Accent
            )
        }

        if (currentAssetPath != null) {

            IconButton(
                onClick = {

                    assetRepository.clearAsset(selectedEffect)
                    assetVersion += 1
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription =
                        stringResource(R.string.ape_remove_custom_image),
                    tint = AeroColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EffectOption(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        AeroColors.Accent.copy(alpha = 0.35f)
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.22f)
                    }
                )
                .border(
                    1.dp,

                    if (selected) {
                        AeroColors.Accent
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.4f)
                    },

                    RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        Text(text = emoji, fontSize = 20.sp)

        Text(
            text = label,
            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun EventRow(
    event: ApeEvent,
    onDelete: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.26f))
                .padding(horizontal = 14.dp, vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text =
                "${formatTime(event.time)} · " +
                "${effectEmoji(event.effect)} " +
                "${effectLabel(event.effect)} · " +
                "${event.duration / 1000}s",

            color = AeroColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
        )

        IconButton(onClick = onDelete) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_remove_event),
                tint = AeroColors.TextSecondary
            )
        }
    }
}

// Fase 6 de ".aero" (0.5.0) — mejor organización: delegan en el
// registro único ([ambientEventTypeFromKey]/[ambientEventTypeEmoji]/
// [ambientEventTypeLabel]) en vez de repetir acá su propio `when` de
// Strings. El fallback para un nombre no reconocido se mantiene
// igual que antes (compatibilidad con `.ape` viejos que puedan
// mencionar un efecto que esta versión no conoce): "✨" genérico
// para el emoji, el texto crudo tal cual para el label.
private fun effectEmoji(effect: String): String =

    ambientEventTypeFromKey(effect)
        ?.let { ambientEventTypeEmoji(it) }
        ?: "✨"

@Composable
private fun effectLabel(effect: String): String =

    ambientEventTypeFromKey(effect)
        ?.let { ambientEventTypeLabel(it) }
        ?: effect

private fun formatTime(ms: Long): String {

    val totalSeconds =
        (ms / 1000).coerceAtLeast(0L)

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}
