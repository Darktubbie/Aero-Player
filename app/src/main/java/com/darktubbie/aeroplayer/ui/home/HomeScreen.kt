package com.darktubbie.aeroplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/**
 * Sección "Inicio" (Fase 1 del plan de evolución visual).
 *
 * Por ahora es solo un destino de navegación real con un placeholder
 * de bienvenida: no implementa todavía contenido (accesos directos,
 * "reanudar", recomendaciones, etc.) — eso no forma parte del
 * alcance de esta fase.
 */
@Composable
fun HomeScreen() {

    AeroBackground {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "Aero Player",
                fontSize = 30.sp,
                color = Color.White
            )

            Text(
                text = "Your music, your way.",
                fontSize = 14.sp,
                color = AeroColors.OnBackgroundSubtitle
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                        .clip(
                            RoundedCornerShape(28.dp)
                        )
                        .background(
                            Color.White.copy(alpha = 0.38f)
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.7f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = AeroColors.Accent
                )

                Text(
                    text = "Bienvenido a Aero Player",
                    color = AeroColors.TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Text(
                    text = "Usa Música o Álbumes para explorar tu biblioteca.",
                    color = AeroColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
