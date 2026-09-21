package com.darktubbie.aeroplayer.ui.more

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Gestión de carpetas de música (hace real la fila "Carpetas" que
 * desde la Fase 1 vivía como placeholder "Próximamente" dentro de
 * Más).
 *
 * Antes no existía ninguna forma de quitar una carpeta ya agregada
 * — `onRemoveFolder` existía en el código desde la Fase 2 pero
 * nunca se invocaba desde ninguna pantalla. Esta pantalla es esa UI
 * que faltaba.
 */
@Composable
fun FoldersScreen(
    selectedFolders: List<String>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onBack: () -> Unit
) {

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                    text = "Carpetas",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedFolders.isEmpty()) {

                Text(
                    text = "No hay ninguna carpeta agregada todavía.",
                    color = AeroColors.TextSecondary,
                    fontSize = 13.sp
                )

            } else {

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {

                    items(selectedFolders) { folder ->

                        FolderRow(
                            folder = folder,
                            onRemove = { onRemoveFolder(folder) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            AeroColors.Accent.copy(alpha = 0.85f)
                        )
                        .clickable { onAddFolder() }
                        .padding(vertical = 12.dp),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Agregar carpeta",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: String,
    onRemove: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.28f))
                .padding(horizontal = 14.dp, vertical = 12.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = AeroColors.Accent
            )

            Text(
                text = displayName(folder),
                color = AeroColors.TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        IconButton(onClick = onRemove) {

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar carpeta",
                tint = AeroColors.TextSecondary
            )
        }
    }
}

/**
 * Las carpetas se guardan como URI de árbol SAF
 * (`content://.../tree/primary:Music`), poco legible tal cual. Se
 * muestra solo la parte después de los dos puntos, que suele ser el
 * nombre/ruta relativa real de la carpeta.
 */
private fun displayName(folder: String): String {

    return try {

        val documentId =
            Uri.parse(folder)
                .lastPathSegment

        val colonIndex =
            documentId?.indexOf(':') ?: -1

        if (documentId != null && colonIndex != -1) {
            documentId.substring(colonIndex + 1)
                .ifBlank { "Almacenamiento interno" }
        } else {
            documentId ?: folder
        }

    } catch (_: Exception) {

        folder
    }
}
