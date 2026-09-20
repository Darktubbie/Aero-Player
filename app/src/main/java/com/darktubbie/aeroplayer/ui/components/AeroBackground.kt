package com.darktubbie.aeroplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darktubbie.aeroplayer.ui.effects.BackgroundLayer
import com.darktubbie.aeroplayer.ui.effects.ForegroundLayer
import com.darktubbie.aeroplayer.ui.effects.MidgroundLayer

/**
 * Contenedor del sistema visual de Aero Player (Fase 3 del plan de
 * evolución visual), organizado en la jerarquía de capas pedida en
 * el brief:
 *
 * 1. [BackgroundLayer] — fondo ambiental.
 * 2. [MidgroundLayer] — elementos ambientales/midground.
 * 3. [content] — interfaz de cristal (lo que cada pantalla dibuja).
 * 4. [ForegroundLayer] — elementos ocasionales de foreground.
 *
 * El resultado visual es idéntico al de antes (mismo degradado,
 * mismas burbujas, en las mismas posiciones): esta fase solo
 * introduce la arquitectura de capas para que las Fases 4-6 puedan
 * reemplazar/animar cada una por separado sin tocar las demás ni
 * la interfaz de cristal existente.
 */
@Composable
fun AeroBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxSize()
    ) {

        BackgroundLayer(
            modifier =
                Modifier.matchParentSize()
        )

        MidgroundLayer(
            modifier =
                Modifier.matchParentSize()
        )

        content()

        ForegroundLayer()
    }
}
