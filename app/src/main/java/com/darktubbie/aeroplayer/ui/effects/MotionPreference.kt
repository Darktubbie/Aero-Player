package com.darktubbie.aeroplayer.ui.effects

import android.content.Context
import android.provider.Settings

/**
 * Lee la preferencia de accesibilidad "eliminar animaciones" de
 * Android (Ajustes del sistema > Accesibilidad > Escala de
 * duración de animación = 0), para que las animaciones ambientales
 * de Aero Player la respeten automáticamente sin necesidad de que
 * el usuario configure nada dentro de la app (Fase 5 del plan de
 * evolución visual).
 *
 * No requiere ningún permiso especial: ANIMATOR_DURATION_SCALE es
 * una configuración pública de Settings.Global.
 */
fun isSystemReduceMotionEnabled(
    context: Context
): Boolean {

    val scale =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )

    return scale == 0f
}
