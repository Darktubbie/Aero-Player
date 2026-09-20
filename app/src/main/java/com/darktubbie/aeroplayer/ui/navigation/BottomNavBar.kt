package com.darktubbie.aeroplayer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
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
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Barra de navegación inferior real (Fase 1), reemplaza la fila de
 * texto no funcional "Library / Favorites / Playlists" que antes
 * vivía dentro de LibraryScreen.
 *
 * Se mantiene el mismo lenguaje visual glass ya usado en el resto
 * de la app (superficie translúcida + borde blanco semitransparente
 * + AeroColors), sin introducir un sistema visual nuevo — eso
 * corresponde a la Fase 3.
 */
@Composable
fun BottomNavBar(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
                .clip(
                    RoundedCornerShape(24.dp)
                )
                .background(
                    Color.White.copy(alpha = 0.48f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.72f),
                    RoundedCornerShape(24.dp)
                )
                .padding(vertical = 10.dp),

        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        NavItem(
            icon = Icons.Default.Home,
            label = "Inicio",
            selected = current == AppDestination.INICIO,
            onClick = { onSelect(AppDestination.INICIO) }
        )

        NavItem(
            icon = Icons.Default.MusicNote,
            label = "Música",
            selected = current == AppDestination.MUSICA,
            onClick = { onSelect(AppDestination.MUSICA) }
        )

        NavItem(
            icon = Icons.Default.Album,
            label = "Álbumes",
            selected = current == AppDestination.ALBUMES,
            onClick = { onSelect(AppDestination.ALBUMES) }
        )

        NavItem(
            icon = Icons.Default.PlayCircle,
            label = "Reproductor",
            selected = current == AppDestination.REPRODUCTOR,
            onClick = { onSelect(AppDestination.REPRODUCTOR) }
        )

        NavItem(
            icon = Icons.Default.MoreHoriz,
            label = "Más",
            selected = current == AppDestination.MAS,
            onClick = { onSelect(AppDestination.MAS) }
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val tint =
        if (selected) {
            AeroColors.Accent
        } else {
            AeroColors.TextMuted
        }

    Column(
        modifier =
            Modifier.clickable(
                onClick = onClick
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint
        )

        Text(
            text = label,
            color = tint,
            fontSize = 11.sp
        )
    }
}
