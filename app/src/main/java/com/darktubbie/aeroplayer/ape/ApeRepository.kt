package com.darktubbie.aeroplayer.ape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Resuelve y parsea el archivo de efectos de una canción (Fase 7
 * del plan de evolución visual: Aero Player Effects — "APE" es el
 * nombre del sistema, no de la extensión de archivo).
 *
 * Reglas del formato, tal como las pidió el brief original:
 *
 * - Un archivo de efectos afecta únicamente a la canción con el
 *   mismo nombre base ("My Song.mp3" + "My Song.aero"), sin importar
 *   la extensión de audio.
 * - Mayúsculas/minúsculas se tratan de forma segura (comparación
 *   insensible a mayúsculas tanto del nombre base como de la
 *   extensión).
 * - Canciones sin archivo de efectos usan los efectos ambientales
 *   predeterminados (el generador aleatorio ya existente en
 *   [com.darktubbie.aeroplayer.ui.effects.ForegroundLayer]).
 * - El tiempo de cada evento se guarda en milisegundos.
 *
 * Extensión ".aero" (reemplaza a ".ape"): se prioriza siempre que
 * exista, y si no, se cae a un ".ape" existente para no romper
 * canciones que ya tenían efectos guardados de antes de este
 * cambio — [com.darktubbie.aeroplayer.ape.ApeWriter] es quien migra
 * ese ".ape" a ".aero" la próxima vez que se edite y guarde esa
 * canción. El formato JSON interno no cambió, solo la extensión.
 *
 * MUY IMPORTANTE (regla del brief, no negociable): APE nunca
 * controla Media3. Este repositorio no tiene ninguna referencia al
 * reproductor — solo lee y devuelve datos. Quien consuma estos
 * datos ([com.darktubbie.aeroplayer.ui.effects.ForegroundLayer])
 * únicamente observa la posición de reproducción para decidir qué
 * efecto mostrar, nunca la modifica.
 *
 * Extensibilidad futura (arquitectura, no implementación): el
 * modelo [ApeFile]/[ApeEvent] solo entiende "tiempo absoluto en ms
 * + efecto + duración" hoy. Nada en este archivo asume que ese es
 * el único tipo de evento posible — una fuente de eventos futura
 * (letras sincronizadas, secciones de canción, beats detectados por
 * análisis de audio) simplemente necesitaría producir la misma
 * lista de [ApeEvent] por otro medio; [ForegroundLayer] ya consume
 * "una lista de eventos con tiempo en ms", no "un archivo .aero"
 * directamente.
 */
object ApeRepository {

    /**
     * Extensión vigente del formato. Ver [LEGACY_EXTENSION] para la
     * extensión anterior, todavía soportada solo en lectura.
     */
    const val CURRENT_EXTENSION = "aero"

    /**
     * Extensión usada antes de este cambio de formato — sigue
     * reconociéndose al leer (compatibilidad hacia atrás), pero
     * nunca se crea un archivo nuevo con esta extensión.
     */
    const val LEGACY_EXTENSION = "ape"

    /**
     * Cache en RAM por ruta de audio. `null` como valor significa
     * "ya se comprobó y esta canción no tiene .ape" (para no volver
     * a tocar el sistema de archivos en cada reproducción de la
     * misma canción).
     *
     * IMPORTANTE: `ConcurrentHashMap` no admite valores `null` (lo
     * lanza como `NullPointerException` al hacer `put`) — por eso
     * el valor se guarda envuelto en `Optional`, ya que "esta
     * canción no tiene .ape" (el caso más común) es exactamente el
     * resultado `null` de [resolveAndParse].
     */
    private val cache =
        ConcurrentHashMap<String, Optional<ApeFile>>()

    suspend fun get(
        audioPath: String
    ): ApeFile? {

        if (audioPath.isBlank()) {
            return null
        }

        cache[audioPath]?.let {
            return it.orElse(null)
        }

        val resolved =
            withContext(Dispatchers.IO) {
                resolveAndParse(audioPath)
            }

        cache[audioPath] = Optional.ofNullable(resolved)

        return resolved
    }

    /**
     * Olvida lo que se sabía sobre el `.ape` de esta canción, para
     * que la próxima llamada a [get] vuelva a leer el disco en vez
     * de devolver lo que ya tenía en caché.
     *
     * Necesario porque el editor (Fase 8) puede crear o modificar un
     * `.ape` mientras esa misma canción sigue sonando: sin esto,
     * [com.darktubbie.aeroplayer.ui.effects.ForegroundLayer] seguiría
     * usando la versión vieja (o "sin .ape") hasta que la canción
     * cambiara o la app se reiniciara.
     */
    fun invalidate(
        audioPath: String
    ) {

        cache.remove(audioPath)
    }

    /**
     * Busca el archivo de efectos de [audioPath] entre los
     * candidatos ya listados de su carpeta: primero con la
     * extensión vigente ([CURRENT_EXTENSION]), y si no aparece, con
     * la extensión anterior ([LEGACY_EXTENSION]) para no dejar de
     * reconocer efectos guardados antes de este cambio de formato.
     *
     * Compartida entre lectura ([resolveAndParse]) y escritura
     * ([com.darktubbie.aeroplayer.ape.ApeWriter]), para que ambas
     * apliquen exactamente el mismo criterio de "cuál es el archivo
     * de esta canción".
     */
    fun findEffectsCandidate(
        files: List<File>,
        baseName: String
    ): File? {

        fun matching(extension: String) =
            files.firstOrNull { candidate ->

                candidate.isFile &&
                candidate.extension.equals(
                    extension,
                    ignoreCase = true
                ) &&
                candidate.nameWithoutExtension.equals(
                    baseName,
                    ignoreCase = true
                )
            }

        return matching(CURRENT_EXTENSION)
            ?: matching(LEGACY_EXTENSION)
    }

    private fun resolveAndParse(
        audioPath: String
    ): ApeFile? {

        return try {

            val audioFile = File(audioPath)

            val parent =
                audioFile.parentFile
                    ?: return null

            val baseName =
                audioFile.nameWithoutExtension

            val effectsFile =
                findEffectsCandidate(
                    parent.listFiles()?.toList() ?: emptyList(),
                    baseName
                ) ?: return null

            parseJson(
                effectsFile.readText(Charsets.UTF_8)
            )

        } catch (_: Exception) {

            // Directorio inaccesible, permiso denegado, archivo
            // desaparecido entre el listFiles() y la lectura, etc.
            // Cualquiera de estos casos equivale a "sin .ape", nunca
            // a un crash.
            null
        }
    }

    private fun parseJson(
        text: String
    ): ApeFile? {

        return try {

            val json = JSONObject(text)

            val version =
                json.optInt("version", 1)

            val effectsArray: JSONArray =
                json.optJSONArray("effects")
                    ?: JSONArray()

            val effects =
                mutableListOf<ApeEvent>()

            for (i in 0 until effectsArray.length()) {

                val eventObject =
                    effectsArray.optJSONObject(i)
                        ?: continue

                val time =
                    eventObject.optLong("time", -1L)

                val effect =
                    eventObject.optString("effect", "")

                val duration =
                    eventObject.optLong("duration", 0L)

                if (time >= 0L && effect.isNotBlank()) {

                    effects.add(
                        ApeEvent(
                            time = time,
                            effect = effect,
                            duration = duration
                        )
                    )
                }
            }

            ApeFile(
                version = version,
                effects = effects
            )

        } catch (_: Exception) {

            null
        }
    }
}
