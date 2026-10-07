package com.darktubbie.aeroplayer.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Los 3 modos de interfaz que Aero Player puede mostrar (Fase 1,
 * 0.6.0 — "The Future We Remember Update"). Ninguno de los dos
 * modos Desktop tiene todavía una pantalla propia: esta fase
 * solamente prepara la arquitectura para que las Fases 2 y 3 puedan
 * construir sobre ella sin tocar de nuevo [AeroDesktopMode] ni la
 * detección de orientación.
 *
 * - [MOBILE]: la interfaz actual, sin cambios. Es el único valor
 *   posible mientras [AeroDesktopMode] esté en OFF, y es lo que se
 *   sigue mostrando en esta fase incluso con AUTO activado, porque
 *   las pantallas Desktop todavía no existen (ver
 *   [com.darktubbie.aeroplayer.MainActivity]).
 * - [DESKTOP_HORIZONTAL]: Fase 2.
 * - [DESKTOP_VERTICAL]: Fase 3.
 */
enum class AeroLayoutMode {
    MOBILE,
    DESKTOP_HORIZONTAL,
    DESKTOP_VERTICAL
}

/**
 * Función pura (sin Compose) para poder decidir el modo desde un
 * test o desde código que no compone, dado un ajuste guardado y si
 * la ventana es más ancha que alta.
 *
 * Con [AeroDesktopMode.OFF] siempre devuelve [AeroLayoutMode.MOBILE],
 * sin importar la orientación — es la garantía de que el ajuste
 * apagado (el default) deja la app exactamente como está hoy.
 */
fun resolveAeroLayoutMode(
    desktopMode: AeroDesktopMode,
    isLandscape: Boolean
): AeroLayoutMode {

    if (desktopMode == AeroDesktopMode.OFF) {
        return AeroLayoutMode.MOBILE
    }

    return if (isLandscape) {
        AeroLayoutMode.DESKTOP_HORIZONTAL
    } else {
        AeroLayoutMode.DESKTOP_VERTICAL
    }
}

/**
 * Versión componible de [resolveAeroLayoutMode]. Desde la Fase 4
 * (0.6.0) `MainActivity` declara `configChanges`, así que rotar ya
 * NO recrea la Activity: la navegación, la reproducción, el
 * progreso y cualquier estado `remember` se conservan.
 *
 * Corrección (revisión post-Fase 4): la orientación NO se lee de
 * `LocalConfiguration`. `MainActivity.attachBaseContext` aplica el
 * idioma con `createConfigurationContext(...)`, y ese contexto
 * envuelto guarda una `Configuration` congelada al crear la
 * Activity — antes no se notaba porque rotar recreaba todo, pero
 * sin recreación `LocalConfiguration.orientation` se queda con el
 * valor viejo y Aero Desktop seguía mostrando el layout vertical en
 * horizontal. El tamaño real de la ventana ([LocalWindowInfo]) sí
 * cambia al rotar sin depender de ninguna `Configuration`, así que
 * "horizontal" = ventana más ancha que alta.
 */
@Composable
fun rememberAeroLayoutMode(
    desktopMode: AeroDesktopMode
): State<AeroLayoutMode> {

    val windowSize = LocalWindowInfo.current.containerSize

    val isLandscape = windowSize.width > windowSize.height

    return remember(desktopMode, isLandscape) {
        derivedStateOf {
            resolveAeroLayoutMode(desktopMode, isLandscape)
        }
    }
}
