package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.darktubbie.aeroplayer.R

/**
 * Capa 1 de la jerarquía visual (Fase 4 del plan de evolución
 * visual): fondo estático.
 *
 * Dibuja `res/drawable/aero_background.png` a pantalla completa
 * (recortado con [ContentScale.Crop] para llenar cualquier
 * proporción de pantalla sin deformarse). El degradado que usaba
 * esta capa en la Fase 3 queda reemplazado por esta imagen real.
 *
 * El archivo incluido ahora mismo es solo un placeholder
 * claramente identificado (rayas + texto indicando qué reemplazar)
 * para poder compilar y probar esta fase — el recurso final lo
 * proporciona el usuario reemplazando directamente ese mismo
 * archivo (`app/src/main/res/drawable/aero_background.png`), sin
 * necesidad de tocar este código.
 */
@Composable
fun BackgroundLayer(
    modifier: Modifier = Modifier
) {

    Image(
        painter =
            painterResource(
                id = R.drawable.aero_background
            ),

        contentDescription = null,

        contentScale =
            ContentScale.Crop,

        modifier = modifier
    )
}
