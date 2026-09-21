package com.darktubbie.aeroplayer.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.data.SettingsRepository
import com.darktubbie.aeroplayer.ui.components.AeroBackground
import com.darktubbie.aeroplayer.ui.effects.AmbientIntensity
import com.darktubbie.aeroplayer.ui.library.SortOrder
import com.darktubbie.aeroplayer.ui.library.sortOrderLabel
import com.darktubbie.aeroplayer.ui.theme.AeroColors
import com.darktubbie.aeroplayer.ui.theme.AppTheme

private const val REPO_URL =
    "https://github.com/Darktubbie/Aero-Player"

/**
 * Ajustes reales, organizados por categoría (Fase 5, 0.4.x) —
 * reemplaza la pantalla mínima de la Fase 1 que solo tenía el
 * toggle de Bluetooth.
 *
 * Categorías deliberadamente acotadas a lo que el proyecto
 * realmente tiene para ofrecer hoy — nada de opciones de relleno:
 * - Apariencia: tema (Aero claro / Dark Aero, post-0.4.0).
 * - Reproducción: pausa por Bluetooth (Fase 1). Shuffle/repeat
 *   también son persistentes desde esta fase, pero no tienen un
 *   toggle propio acá: son un comportamiento de fondo ("recordar lo
 *   que el usuario dejó"), no una preferencia que alguien vaya a
 *   querer prender/apagar.
 * - Animaciones: intensidad de efectos ambientales — el resto
 *   (respeto a "reducir movimiento" del sistema, pausa si no hay
 *   música) ya es automático y no necesita UI, ver
 *   [com.darktubbie.aeroplayer.ui.effects.MidgroundLayer].
 * - Biblioteca: orden predeterminado de Songs (mismo valor que
 *   [com.darktubbie.aeroplayer.ui.library.LibraryScreen], mostrado
 *   acá también porque es exactamente el tipo de preferencia que el
 *   brief pide agrupar en Ajustes) y acceso directo a Carpetas.
 * - Aplicación: versión instalada y repositorio.
 *
 * [sortOrder]/[onSortOrderChange], [ambientIntensity]/
 * [onAmbientIntensityChange] y [appTheme]/[onAppThemeChange] vienen
 * de MainViewModel (afectan otras pantallas); la preferencia de
 * Bluetooth sigue siendo standalone vía SettingsRepository porque
 * nada más en la app necesita reaccionar a ella en vivo.
 */
@Composable
fun SettingsScreen(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    ambientIntensity: AmbientIntensity,
    onAmbientIntensityChange: (AmbientIntensity) -> Unit,
    appTheme: AppTheme,
    onAppThemeChange: (AppTheme) -> Unit,
    onOpenFolders: () -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val settingsRepository =
        remember {
            SettingsRepository(context)
        }

    var pauseOnBluetoothDisconnect by remember {
        mutableStateOf(
            settingsRepository
                .isPauseOnBluetoothDisconnectEnabled()
        )
    }

    val appVersion =
        remember {

            runCatching {

                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName

            }.getOrNull() ?: "—"
        }

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
                    text = "Ajustes",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    SettingsSectionHeader("Apariencia")
                }

                item {

                    SettingsCard {

                        Text(
                            text = "Tema",
                            color = AeroColors.TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            IntensityChip(
                                label = "Aero claro",
                                selected = appTheme == AppTheme.LIGHT_AERO,
                                onClick = {
                                    onAppThemeChange(AppTheme.LIGHT_AERO)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IntensityChip(
                                label = "Dark Aero",
                                selected = appTheme == AppTheme.DARK_AERO,
                                onClick = {
                                    onAppThemeChange(AppTheme.DARK_AERO)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader("Reproducción")
                }

                item {

                    SettingsCard {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        "Pausar al desconectar " +
                                        "Bluetooth",
                                    color = AeroColors.TextPrimary,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text =
                                        "Pausa la música si se " +
                                        "desconecta el dispositivo " +
                                        "Bluetooth que la está " +
                                        "reproduciendo",
                                    color = AeroColors.TextTertiary,
                                    fontSize = 12.sp,
                                    modifier =
                                        Modifier.padding(
                                            top = 4.dp,
                                            end = 8.dp
                                        )
                                )
                            }

                            Switch(
                                checked = pauseOnBluetoothDisconnect,

                                onCheckedChange = { enabled ->

                                    pauseOnBluetoothDisconnect =
                                        enabled

                                    settingsRepository
                                        .setPauseOnBluetoothDisconnectEnabled(
                                            enabled
                                        )
                                },

                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor =
                                            AeroColors.Accent
                                    )
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader("Animaciones")
                }

                item {

                    SettingsCard {

                        Text(
                            text = "Intensidad de efectos ambientales",
                            color = AeroColors.TextPrimary,
                            fontSize = 14.sp
                        )

                        Text(
                            text =
                                "Se pausan solos si no hay música " +
                                "sonando, y respetan \"eliminar " +
                                "animaciones\" del sistema",
                            color = AeroColors.TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = 12.dp
                            )
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            AmbientIntensity.entries.forEach { option ->

                                IntensityChip(
                                    label =
                                        ambientIntensityLabel(option),
                                    selected = option == ambientIntensity,
                                    onClick = {
                                        onAmbientIntensityChange(option)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsSectionHeader("Biblioteca")
                }

                item {

                    SettingsCard {

                        Text(
                            text = "Orden predeterminado de Songs",
                            color = AeroColors.TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        AeroColors.GlassSurfaceBase.copy(alpha = 0.5f)
                                    ),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            SortOrderPicker(
                                sortOrder = sortOrder,
                                onSortOrderChange = onSortOrderChange
                            )
                        }
                    }
                }

                item {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    AeroColors.GlassSurfaceBase.copy(alpha = 0.32f)
                                )
                                .border(
                                    1.dp,
                                    AeroColors.GlassSurfaceBase.copy(alpha = 0.55f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable(onClick = onOpenFolders)
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
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AeroColors.Accent
                            )

                            Text(
                                text = "Carpetas de música",
                                color = AeroColors.TextPrimary,
                                fontSize = 14.sp,
                                modifier =
                                    Modifier.padding(start = 10.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AeroColors.TextTertiary
                        )
                    }
                }

                item {
                    SettingsSectionHeader("Aplicación")
                }

                item {

                    SettingsCard {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Versión",
                                color = AeroColors.TextPrimary,
                                fontSize = 14.sp
                            )

                            Text(
                                text = appVersion,
                                color = AeroColors.TextTertiary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Repositorio",
                            color = AeroColors.Accent,
                            fontSize = 13.sp,
                            modifier =
                                Modifier.clickable {

                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(REPO_URL)
                                        )
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String
) {

    Text(
        text = title,
        color = AeroColors.OnBackgroundSubtitle,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 4.dp,
            bottom = 2.dp
        )
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(AeroColors.GlassSurfaceBase.copy(alpha = 0.32f))
                .border(
                    1.dp,
                    AeroColors.GlassSurfaceBase.copy(alpha = 0.55f),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp),

        content = content
    )
}

@Composable
private fun IntensityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) {
                        AeroColors.Accent
                    } else {
                        AeroColors.GlassSurfaceBase.copy(alpha = 0.5f)
                    }
                )
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            color =
                if (selected) {
                    Color.White
                } else {
                    AeroColors.TextSecondary
                },
            fontSize = 10.sp,
            fontWeight =
                if (selected) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                }
        )
    }
}

@Composable
private fun SortOrderPicker(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        SortOrder.entries.forEach { option ->

            val selected =
                option == sortOrder

            Text(
                text = sortOrderLabel(option),

                color =
                    if (selected) {
                        Color.White
                    } else {
                        AeroColors.TextSecondary
                    },

                fontSize = 10.sp,

                modifier =
                    Modifier
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) {
                                AeroColors.Accent
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable {
                            onSortOrderChange(option)
                        }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
            )
        }
    }
}

private fun ambientIntensityLabel(
    intensity: AmbientIntensity
): String {

    return when (intensity) {
        AmbientIntensity.OFF -> "Apagado"
        AmbientIntensity.STATIC -> "Estático"
        AmbientIntensity.LOW -> "Bajo"
        AmbientIntensity.NORMAL -> "Normal"
        AmbientIntensity.HIGH -> "Alto"
    }
}
