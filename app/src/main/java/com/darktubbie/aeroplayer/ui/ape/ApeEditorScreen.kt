package com.darktubbie.aeroplayer.ui.ape

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.ape.ApeEvent
import com.darktubbie.aeroplayer.ape.ApeRepository
import com.darktubbie.aeroplayer.ape.ApeSaveResult
import com.darktubbie.aeroplayer.ape.ApeWriter
import com.darktubbie.aeroplayer.data.AudioTrack
import com.darktubbie.aeroplayer.data.EffectAssetRepository
import com.darktubbie.aeroplayer.ui.components.AeroBackground
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
 * Solo efectos integrados (bubble/fish/jellyfish/cloud) — un editor
 * de efectos personalizados es explícitamente una fase futura, no
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
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Column(
                    modifier =
                        Modifier.padding(start = 4.dp)
                ) {

                    Text(
                        text = "Editor APE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = track.title,
                        color = AeroColors.TextSecondary,
                        fontSize = 13.sp,
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
                        fontSize = 13.sp
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
                                if (isPlaying) "Pausar" else "Reproducir",

                            tint = AeroColors.Accent
                        )
                    }

                    Text(
                        text = formatTime(durationMs),
                        color = AeroColors.TextSecondary,
                        fontSize = 13.sp
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
                        "Añadir efecto en ${formatTime(positionMs)}",

                    color = AeroColors.TextPrimary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    EffectOption(
                        emoji = "🫧",
                        label = "Burbuja",
                        selected = selectedEffect == "bubble",
                        onClick = { selectedEffect = "bubble" }
                    )

                    EffectOption(
                        emoji = "🐠",
                        label = "Pez",
                        selected = selectedEffect == "fish",
                        onClick = { selectedEffect = "fish" }
                    )

                    EffectOption(
                        emoji = "🪼",
                        label = "Medusa",
                        selected = selectedEffect == "jellyfish",
                        onClick = { selectedEffect = "jellyfish" }
                    )

                    EffectOption(
                        emoji = "☁️",
                        label = "Nube",
                        selected = selectedEffect == "cloud",
                        onClick = { selectedEffect = "cloud" }
                    )
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
                        text = "Duración",
                        color = AeroColors.TextSecondary,
                        fontSize = 13.sp,
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
                            contentDescription = "Menos duración",
                            tint = AeroColors.TextPrimary
                        )
                    }

                    Text(
                        text = "${durationSeconds}s",
                        color = AeroColors.TextPrimary,
                        fontSize = 14.sp,
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
                            contentDescription = "Más duración",
                            tint = AeroColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassButton(
                    text = "Añadir efecto aquí",

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
                text = "Eventos (${events.size})",
                color = Color.White,
                fontSize = 15.sp
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
                    if (saving) "Guardando…" else "Guardar .ape",

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
                                    "Guardado en: ${result.path}"

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

                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!loaded) {

                Text(
                    text = "Cargando eventos existentes…",
                    color = AeroColors.TextSecondary,
                    fontSize = 12.sp,
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
                .background(Color.White.copy(alpha = 0.34f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.6f),
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
                        Color.White.copy(alpha = 0.25f)
                    }
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 12.dp),

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
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
                    "Imagen personalizada activa"
                } else {
                    "Usando el ícono integrado"
                },

            color = AeroColors.TextSecondary,
            fontSize = 12.sp,
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
                    "Elegir imagen personalizada",
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
                        "Quitar imagen personalizada",
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
                        Color.White.copy(alpha = 0.22f)
                    }
                )
                .border(
                    1.dp,

                    if (selected) {
                        AeroColors.Accent
                    } else {
                        Color.White.copy(alpha = 0.4f)
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
            fontSize = 11.sp
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
                .background(Color.White.copy(alpha = 0.26f))
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
            fontSize = 13.sp
        )

        IconButton(onClick = onDelete) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar evento",
                tint = AeroColors.TextSecondary
            )
        }
    }
}

private fun effectEmoji(effect: String): String =

    when (effect.lowercase()) {
        "bubble", "bubble_front" -> "🫧"
        "fish" -> "🐠"
        "jellyfish" -> "🪼"
        "cloud" -> "☁️"
        else -> "✨"
    }

private fun effectLabel(effect: String): String =

    when (effect.lowercase()) {
        "bubble", "bubble_front" -> "Burbuja"
        "fish" -> "Pez"
        "jellyfish" -> "Medusa"
        "cloud" -> "Nube"
        else -> effect
    }

private fun formatTime(ms: Long): String {

    val totalSeconds =
        (ms / 1000).coerceAtLeast(0L)

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}
