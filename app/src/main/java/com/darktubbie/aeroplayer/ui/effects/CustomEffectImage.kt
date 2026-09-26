package com.darktubbie.aeroplayer.ui.effects

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caché en RAM de los recursos personalizados por efecto (Fase 9).
 * Como máximo hay 4 (uno por tipo de efecto), así que a diferencia
 * de [com.darktubbie.aeroplayer.AlbumArtCache] no hace falta un LRU
 * con límite: se decodifica una vez por ruta y se reutiliza
 * mientras la app siga viva. Si el usuario reemplaza un recurso, la
 * ruta cambia (ver [com.darktubbie.aeroplayer.data.EffectAssetRepository]
 * — el nombre del archivo interno es fijo por tipo de efecto, pero
 * su contenido cambia), así que esta caché puede quedar con un
 * bitmap viejo asociado a esa ruta; es un caso raro (cambiar el
 * recurso personalizado mientras hay efectos activos en pantalla) y
 * se resuelve solo la próxima vez que se reinicie la app.
 */
private object CustomEffectBitmapCache {

    private val cache =
        ConcurrentHashMap<String, Bitmap>()

    suspend fun get(
        path: String
    ): Bitmap? {

        cache[path]?.let {
            return it
        }

        val bitmap =
            withContext(Dispatchers.IO) {

                try {
                    BitmapFactory.decodeFile(path)
                } catch (_: Exception) {
                    null
                }
            }

        if (bitmap != null) {
            cache[path] = bitmap
        }

        return bitmap
    }
}

/**
 * Dibuja el recurso personalizado de un efecto (Fase 9), sustituto
 * visual del emoji/silueta integrada para ese tipo mientras exista
 * uno (ver [com.darktubbie.aeroplayer.data.EffectAssetRepository]).
 */
@Composable
fun CustomEffectImage(
    path: String,
    modifier: Modifier = Modifier
) {

    var bitmap by
        remember(path) {
            mutableStateOf<Bitmap?>(null)
        }

    LaunchedEffect(path) {

        bitmap =
            CustomEffectBitmapCache.get(path)
    }

    bitmap?.let { loadedBitmap ->

        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,

            modifier =
                modifier.size(56.dp)
        )
    }
}

/**
 * Elige entre el recurso personalizado de este efecto (si el
 * usuario configuró uno — Fase 9) o el ícono/silueta integrada
 * ([builtIn]).
 */
@Composable
fun CustomOrBuiltInEffect(
    assetRepository: com.darktubbie.aeroplayer.data.EffectAssetRepository,
    type: AmbientEventType,
    modifier: Modifier,
    builtIn: @Composable () -> Unit
) {

    val assetPath =
        remember(type) {
            assetRepository.getAssetPath(
                ambientEventTypeKey(type)
            )
        }

    if (assetPath != null) {

        CustomEffectImage(
            path = assetPath,
            modifier = modifier
        )

    } else {

        builtIn()
    }
}
