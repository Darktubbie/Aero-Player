package com.darktubbie.aeroplayer.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Recursos personalizados por efecto (Fase 9 del plan de evolución
 * visual): Effect → Asset → Built-in o Custom.
 *
 * Es una preferencia GLOBAL, no por canción — "la misma
 * personalización debería poder reutilizarse en diferentes
 * canciones", tal como pidió el brief. El `.ape` sigue sin saber
 * nada de esto: solo describe qué efecto y cuándo (ver
 * [com.darktubbie.aeroplayer.ape.ApeEvent]), nunca qué imagen se usa
 * para dibujarlo.
 *
 * Las claves de efecto son las mismas cadenas que ya usa el formato
 * `.ape` ("bubble", "fish", "jellyfish", "cloud"), no los nombres
 * del enum de Kotlin.
 *
 * El recurso elegido se COPIA a almacenamiento interno propio de la
 * app (no se guarda solo la URI elegida) para que "permanezca
 * localmente en el dispositivo" tal como pidió el brief, y para que
 * siga funcionando aunque el usuario borre/mueva el archivo
 * original o el permiso temporal sobre esa URI expire.
 *
 * Solo admite imágenes estáticas por ahora (PNG/JPG/WEBP) —
 * reproducir GIF animado requeriría un decodificador de frames que
 * el brief pide explícitamente no implementar todavía ("no
 * implementes un editor completo de efectos personalizados").
 */
class EffectAssetRepository(
    private val context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val assetsDir: File by lazy {

        File(
            context.filesDir,
            "effect_assets"
        ).apply {
            mkdirs()
        }
    }

    /**
     * Ruta local del recurso personalizado de este efecto, o `null`
     * si debe usarse el ícono/silueta integrada.
     */
    fun getAssetPath(
        effectType: String
    ): String? {

        val path =
            prefs.getString(
                key(effectType),
                null
            ) ?: return null

        return if (File(path).exists()) path else null
    }

    /**
     * Copia [sourceUri] a almacenamiento interno como el recurso
     * personalizado de [effectType], reemplazando el anterior si
     * había uno.
     *
     * @return true si se guardó correctamente.
     */
    fun setAsset(
        effectType: String,
        sourceUri: Uri
    ): Boolean {

        return try {

            val target =
                File(assetsDir, effectType)

            context.contentResolver
                .openInputStream(sourceUri)
                ?.use { input ->

                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                ?: return false

            prefs.edit()
                .putString(
                    key(effectType),
                    target.absolutePath
                )
                .apply()

            true

        } catch (_: Exception) {

            false
        }
    }

    /**
     * Vuelve a usar el ícono/silueta integrada para este efecto.
     */
    fun clearAsset(
        effectType: String
    ) {

        prefs.getString(key(effectType), null)?.let {
            File(it).delete()
        }

        prefs.edit()
            .remove(key(effectType))
            .apply()
    }

    private fun key(
        effectType: String
    ) = "asset_$effectType"

    companion object {

        private const val PREFS_NAME =
            "aero_player_effect_assets"
    }
}
