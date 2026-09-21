package com.darktubbie.aeroplayer.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.data.Playlist
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Lista de playlists (Fase 3, 0.4.x), colgada de la fila
 * "Playlists" en Más — mismo patrón de overlay a pantalla completa
 * que FoldersScreen/FavoritesScreen.
 *
 * [trackCountFor] se recibe como función en vez de un Map ya
 * calculado para no forzar a MainActivity a resolver el contenido
 * de todas las playlists (fingerprint -> AudioTrack) solo para
 * mostrar un número, cuando la pantalla puede pedirlo bajo demanda
 * por playlist.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    trackCountFor: (Playlist) -> Int,
    onCreatePlaylist: (String) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onBack: () -> Unit
) {

    var showCreateDialog by remember {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = onBack) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Playlists",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                IconButton(
                    onClick = { showCreateDialog = true }
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nueva playlist",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Todavía no creaste ninguna playlist",
                        color = AeroColors.OnBackgroundSubtitle,
                        fontSize = 13.sp
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        playlists,
                        key = { it.id }
                    ) { playlist ->

                        val count =
                            trackCountFor(playlist)

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(alpha = 0.38f)
                                    )
                                    .clickable {
                                        onOpenPlaylist(playlist)
                                    }
                                    .padding(14.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = AeroColors.Accent
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = playlist.name,
                                    color = AeroColors.TextPrimary,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )

                                Text(
                                    text =
                                        if (count == 1) {
                                            "1 canción"
                                        } else {
                                            "$count canciones"
                                        },

                                    color = AeroColors.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDeletePlaylist(playlist)
                                },

                                modifier = Modifier.size(36.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar playlist",
                                    tint = AeroColors.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {

        PlaylistNameDialog(
            title = "Nueva playlist",
            initialValue = "",

            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },

            onDismiss = {
                showCreateDialog = false
            }
        )
    }
}

/**
 * Diálogo simple de texto para crear/renombrar una playlist —
 * compartido por [PlaylistsScreen] y [PlaylistDetailScreen].
 */
@Composable
fun PlaylistNameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var text by remember {
        mutableStateOf(initialValue)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.45f)
                )
                .clickable(onClick = onDismiss),

        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(onClick = {})
                    .clip(RoundedCornerShape(20.dp))
                    .background(AeroColors.DialogSurface)
                    .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = title,
                    color = AeroColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = onDismiss) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = AeroColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.7f))
                        .padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        )
            ) {

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,

                    textStyle =
                        TextStyle(
                            color = AeroColors.TextPrimary,
                            fontSize = 14.sp
                        ),

                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (text.isNotBlank()) {
                                AeroColors.Accent.copy(alpha = 0.85f)
                            } else {
                                AeroColors.Accent.copy(alpha = 0.35f)
                            }
                        )
                        .clickable(enabled = text.isNotBlank()) {
                            onConfirm(text)
                        }
                        .padding(vertical = 12.dp),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Guardar",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
