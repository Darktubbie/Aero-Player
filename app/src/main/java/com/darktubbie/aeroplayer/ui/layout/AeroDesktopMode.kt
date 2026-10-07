package com.darktubbie.aeroplayer.ui.layout

/**
 * Ajuste persistente de Ajustes (Fase 1, 0.6.0 — "The Future We
 * Remember Update") que decide si Aero Player puede mostrar las
 * interfaces Aero Desktop (horizontal en Fase 2, vertical en Fase 3)
 * en lugar de la interfaz Mobile actual.
 *
 * - [OFF] (default): se comporta exactamente como hoy. Actualizar a
 *   0.6.0 no cambia la interfaz de nadie sin que lo pida.
 * - [AUTO]: [AeroLayoutMode] pasa a decidirse según la orientación —
 *   horizontal usa Aero Desktop horizontal, vertical usa Aero
 *   Desktop vertical. No hay variantes manuales ("forzar horizontal"
 *   estando en vertical, etc.): la orientación real del dispositivo
 *   es la única señal, tal como se acordó para esta fase.
 *
 * Se persiste igual que [com.darktubbie.aeroplayer.ui.theme.AppTheme]
 * (nombre del enum como String en
 * [com.darktubbie.aeroplayer.data.SettingsRepository]), no un
 * Boolean simple, para poder agregar más valores el día que hicieran
 * falta sin migrar el dato guardado.
 */
enum class AeroDesktopMode {
    OFF,
    AUTO
}
