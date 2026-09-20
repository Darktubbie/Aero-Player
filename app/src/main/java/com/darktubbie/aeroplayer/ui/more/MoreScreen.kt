package com.darktubbie.aeroplayer.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Sección "Más" (Fase 1 del plan de evolución visual).
 *
 * Artistas, Favoritos y Playlists siguen como filas inertes
 * "Próximamente" (fases futuras). Carpetas ya es funcional (antes
 * no había ninguna forma de quitar una carpeta agregada — ver
 * [FoldersScreen]).
 */
@Composable
fun MoreScreen(
    onOpenFolders: () -> Unit
) {

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            Text(
                text = "Más",
                fontSize = 30.sp,
                color = Color.White
            )

            Text(
                text = "Próximamente en Aero Player",
                fontSize = 14.sp,
                color = AeroColors.OnBackgroundSubtitle,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            MoreRow(icon = Icons.Default.Person, title = "Artistas")
            MoreRow(icon = Icons.Default.Star, title = "Favoritos")
            MoreRow(icon = Icons.Default.PlaylistPlay, title = "Playlists")

            MoreRow(
                icon = Icons.Default.Folder,
                title = "Carpetas",
                onClick = onOpenFolders
            )

            MoreRow(icon = Icons.Default.Settings, title = "Ajustes")
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .background(
                    Color.White.copy(alpha = 0.32f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(18.dp)
                )
                .let {
                    if (onClick != null) {
                        it.clickable(onClick = onClick)
                    } else {
                        it
                    }
                }
                .padding(16.dp),

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AeroColors.Accent
            )

            Text(
                text = title,
                color = AeroColors.TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Text(
            text = if (onClick != null) "" else "Próximamente",
            color = AeroColors.TextTertiary,
            fontSize = 12.sp
        )
    }
}
