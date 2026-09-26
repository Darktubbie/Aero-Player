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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darktubbie.aeroplayer.R
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
 * - Apariencia: tema (Light Aero / Aero Dark, post-0.4.0) e idioma
 *   de la interfaz (Fase 2, 0.5.0).
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
 * [onAmbientIntensityChange], [appTheme]/[onAppThemeChange] y
 * [appLanguage]/[onAppLanguageChange] vienen de MainViewModel
 * (afectan otras pantallas, o —en el caso del idioma— a toda la
 * app); la preferencia de Bluetooth sigue siendo standalone vía
 * SettingsRepository porque nada más en la app necesita reaccionar
 * a ella en vivo.
 *
 * [onAppLanguageChange] recrea la Activity (ver
 * [com.darktubbie.aeroplayer.MainActivity.attachBaseContext]): es
 * la única forma de que Compose vuelva a resolver
 * `stringResource()` con el nuevo idioma.
 */
@Composable
fun SettingsScreen(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    ambientIntensity: AmbientIntensity,
    onAmbientIntensityChange: (AmbientIntensity) -> Unit,
    appTheme: AppTheme,
    onAppThemeChange: (AppTheme) -> Unit,
    appLanguage: String,
    onAppLanguageChange: (String) -> Unit,
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
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White
                    )
                }

                Text(
                    text = stringResource(R.string.settings_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                }

                item {

                    SettingsCard {

                        Text(
                            text = stringResource(R.string.settings_theme_label),
                            color = AeroColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            IntensityChip(
                                label = stringResource(R.string.settings_theme_light),
                                selected = appTheme == AppTheme.LIGHT_AERO,
                                onClick = {
                                    onAppThemeChange(AppTheme.LIGHT_AERO)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IntensityChip(
                                label = stringResource(R.string.settings_theme_dark),
                                selected = appTheme == AppTheme.DARK_AERO,
                                onClick = {
                                    onAppThemeChange(AppTheme.DARK_AERO)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.settings_language_label),
                            color = AeroColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            IntensityChip(
                                label = stringResource(R.string.settings_language_es),
                                selected = appLanguage == "es",
                                onClick = {
                                    onAppLanguageChange("es")
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IntensityChip(
                                label = stringResource(R.string.settings_language_en),
                                selected = appLanguage == "en",
                                onClick = {
                                    onAppLanguageChange("en")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.settings_section_playback))
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
                                    text = stringResource(R.string.settings_bluetooth_title),
                                    color = AeroColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                Text(
                                    text = stringResource(R.string.settings_bluetooth_subtitle),
                                    color = AeroColors.TextTertiary,
                                    style = MaterialTheme.typography.labelMedium,
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
                    SettingsSectionHeader(stringResource(R.string.settings_section_animations))
                }

                item {

                    SettingsCard {

                        Text(
                            text = stringResource(R.string.settings_ambient_intensity_label),
                            color = AeroColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Text(
                            text = stringResource(R.string.settings_ambient_intensity_hint),
                            color = AeroColors.TextTertiary,
                            style = MaterialTheme.typography.labelMedium,
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
                    SettingsSectionHeader(stringResource(R.string.settings_section_library))
                }

                item {

                    SettingsCard {

                        Text(
                            text = stringResource(R.string.settings_default_sort),
                            color = AeroColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
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
                                text = stringResource(R.string.settings_music_folders),
                                color = AeroColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
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
                    SettingsSectionHeader(stringResource(R.string.settings_section_app))
                }

                item {

                    SettingsCard {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = stringResource(R.string.settings_version),
                                color = AeroColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Text(
                                text = appVersion,
                                color = AeroColors.TextTertiary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.settings_repository),
                            color = AeroColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
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
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
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
                    AeroColors.OnAccent
                } else {
                    AeroColors.TextSecondary
                },
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight =
                        if (selected) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Normal
                        }
                )
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
                        AeroColors.OnAccent
                    } else {
                        AeroColors.TextSecondary
                    },

                style = MaterialTheme.typography.labelSmall,

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

@Composable
private fun ambientIntensityLabel(
    intensity: AmbientIntensity
): String {

    return when (intensity) {
        AmbientIntensity.OFF -> stringResource(R.string.intensity_off)
        AmbientIntensity.STATIC -> stringResource(R.string.intensity_static)
        AmbientIntensity.LOW -> stringResource(R.string.intensity_low)
        AmbientIntensity.NORMAL -> stringResource(R.string.intensity_normal)
        AmbientIntensity.HIGH -> stringResource(R.string.intensity_high)
    }
}
