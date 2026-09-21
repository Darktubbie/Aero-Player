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
import androidx.compose.material.icons.filled.History
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
 * Ya no quedan filas "Próximamente": Favoritos, Playlists,
 * Historial, Carpetas y Ajustes son funcionales (ver
 * [FavoritesScreen], [PlaylistsScreen]/[PlaylistDetailScreen],
 * [HistoryScreen], [FoldersScreen] y [SettingsScreen]).
 *
 * No hay fila "Artistas": Artists ya es una de las tres pestañas
 * (Songs/Albums/Artists) dentro de la propia LibraryScreen,
 * accesible desde Música o Álbumes — tener además una fila acá
 * diciendo "Próximamente" era engañoso, la función ya existe.
 */
@Composable
fun MoreScreen(
    onOpenFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenHistory: () -> Unit
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
                text = "Herramientas y ajustes de Aero Player",
                fontSize = 14.sp,
                color = AeroColors.OnBackgroundSubtitle,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            MoreRow(
                icon = Icons.Default.Star,
                title = "Favoritos",
                onClick = onOpenFavorites
            )

            MoreRow(icon = Icons.Default.PlaylistPlay, title = "Playlists", onClick = onOpenPlaylists)

            MoreRow(
                icon = Icons.Default.History,
                title = "Historial",
                onClick = onOpenHistory
            )

            MoreRow(
                icon = Icons.Default.Folder,
                title = "Carpetas",
                onClick = onOpenFolders
            )

            MoreRow(
                icon = Icons.Default.Settings,
                title = "Ajustes",
                onClick = onOpenSettings
            )
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
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.32f)
                )
                .border(
                    1.dp,
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.55f),
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
