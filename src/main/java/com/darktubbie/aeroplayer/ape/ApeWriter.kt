package com.darktubbie.aeroplayer.ape

import android.content.Context
import com.darktubbie.aeroplayer.R
import androidx.documentfile.provider.DocumentFile
import com.darktubbie.aeroplayer.data.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Resultado de intentar guardar un archivo de efectos (Fase 8 del
 * plan de evolución visual: editor básico de APE). El archivo en
 * disco siempre queda con extensión `.aero` — ver [ApeRepository].
 */
sealed class ApeSaveResult {
    data class Success(val path: String) : ApeSaveResult()
    data class Error(val message: String) : ApeSaveResult()
}

/**
 * Escribe/reemplaza el archivo de efectos (`.aero`) hermano de una
 * canción, migrando automáticamente un `.ape` anterior si existe.
 *
 * La app no tiene permiso general de escritura sobre el
 * almacenamiento (solo lectura de audio) — por eso esto reutiliza
 * el permiso persistente ya concedido sobre la carpeta SAF que el
 * usuario agregó en Ajustes > Carpetas y que contiene esa canción,
 * en vez de pedir un permiso nuevo. Si la canción no está dentro de
 * ninguna carpeta ya agregada, no se puede guardar (se explica el
 * motivo en el [ApeSaveResult.Error] devuelto).
 *
 * MUY IMPORTANTE (misma regla del brief que en [ApeRepository]):
 * esto solo escribe un archivo de datos — nunca toca Media3.
 */
object ApeWriter {

    suspend fun save(
        context: Context,
        audioPath: String,
        selectedFolders: List<String>,
        events: List<ApeEvent>
    ): ApeSaveResult =

        withContext(Dispatchers.IO) {

            val json = buildJson(events)
            val audioFile = File(audioPath)

            /*
             * Intento 1: escritura directa por ruta, exactamente
             * la misma vía que ya usa la lectura (ApeRepository) —
             * sin ninguna reconstrucción de ruta de por medio, así
             * que si esto funciona, es matemáticamente imposible
             * que el archivo termine en un lugar distinto al de la
             * canción. En dispositivos donde esto no está permitido
             * (bloqueado por almacenamiento con ámbito), lanza una
             * excepción y se sigue con el intento 2.
             */
            val direct =
                trySaveDirectly(context, audioFile, json)

            if (direct != null) {

                if (direct is ApeSaveResult.Success) {
                    ApeRepository.invalidate(audioPath)
                }

                return@withContext direct
            }

            /*
             * Intento 2: DocumentFile sobre el árbol SAF ya
             * concedido (necesario en dispositivos donde el
             * intento 1 no tiene permiso, típicamente tarjetas SD).
             */
            val safResult =
                trySaveViaSaf(
                    context,
                    audioFile,
                    selectedFolders,
                    json
                )

            if (safResult is ApeSaveResult.Success) {
                ApeRepository.invalidate(audioPath)
            }

            safResult
        }

    /**
     * @return el resultado si la escritura directa se pudo
     * intentar (éxito o fallo real ya verificado), o `null` si ni
     * siquiera se pudo intentar (para que [save] siga con SAF).
     */
    private fun trySaveDirectly(
        context: Context,
        audioFile: File,
        json: String
    ): ApeSaveResult? {

        return try {

            val parent =
                audioFile.parentFile
                    ?: return null

            if (!parent.canWrite()) {

                // No hay permiso de escritura directa aquí — no es
                // un error real, es la señal para pasar a SAF.
                return null
            }

            val target =
                resolveTargetFile(
                    parent,
                    audioFile.nameWithoutExtension
                )

            target.target.writeText(json, Charsets.UTF_8)

            if (
                !target.target.exists() ||
                target.target.length() == 0L
            ) {

                return ApeSaveResult.Error(
                    context.getString(
                        R.string.ape_error_write_verify,
                        target.target.absolutePath
                    )
                )
            }

            // El .aero nuevo ya está a salvo en disco — recién
            // ahora es seguro borrar el .ape viejo que se migró.
            target.legacyFileToDelete?.delete()

            ApeSaveResult.Success(target.target.absolutePath)

        } catch (_: Exception) {

            // Sin permiso u otro problema de acceso directo: se
            // intenta con SAF a continuación, no se reporta como
            // error todavía.
            null
        }
    }

    private suspend fun trySaveViaSaf(
        context: Context,
        audioFile: File,
        selectedFolders: List<String>,
        json: String
    ): ApeSaveResult {

        return try {

            val parentPath =
                audioFile.parentFile
                    ?.absolutePath
                    ?.replace('\\', '/')
                    ?.trimEnd('/')
                    ?: return ApeSaveResult.Error(
                        context.getString(R.string.ape_error_resolve_folder)
                    )

            val folderRepository =
                FolderRepository(context)

            val matched =
                folderRepository.findContainingFolder(
                    audioFile.absolutePath,
                    selectedFolders
                ) ?: return ApeSaveResult.Error(
                    context.getString(
                        R.string.ape_error_no_matching_folder,
                        parentPath
                    )
                )

            var directory: DocumentFile =
                DocumentFile.fromTreeUri(
                    context,
                    matched.treeUri
                ) ?: return ApeSaveResult.Error(
                    context.getString(R.string.ape_error_access_folder)
                )

            val relativePath =
                parentPath
                    .removePrefix(matched.rootPath)
                    .trim('/')

            if (relativePath.isNotEmpty()) {

                for (segment in relativePath.split('/')) {

                    val next =
                        directory.findFile(segment)
                            ?: return ApeSaveResult.Error(
                                context.getString(
                                    R.string.ape_error_subfolder_not_found,
                                    segment,
                                    matched.rootPath,
                                    parentPath
                                )
                            )

                    if (!next.isDirectory) {

                        return ApeSaveResult.Error(
                            context.getString(R.string.ape_error_invalid_path)
                        )
                    }

                    directory = next
                }
            }

            val currentName =
                "${audioFile.nameWithoutExtension}." +
                ApeRepository.CURRENT_EXTENSION

            val legacyName =
                "${audioFile.nameWithoutExtension}." +
                ApeRepository.LEGACY_EXTENSION

            val siblingFiles =
                directory.listFiles().toList()

            // Insensible a mayúsculas, igual que en la lectura
            // (ApeRepository): si ya existe un .aero para esta
            // canción con cualquier combinación de mayúsculas, se
            // sobrescribe ese en vez de crear uno duplicado.
            val existingCurrent =
                siblingFiles.firstOrNull {

                    it.isFile &&
                    it.name?.equals(
                        currentName,
                        ignoreCase = true
                    ) == true
                }

            // Si no hay .aero pero sí un .ape (formato anterior),
            // se migra: se crea el .aero nuevo y, recién después de
            // escribirlo con éxito, se borra este .ape.
            val legacyToDelete =
                if (existingCurrent == null) {

                    siblingFiles.firstOrNull {

                        it.isFile &&
                        it.name?.equals(
                            legacyName,
                            ignoreCase = true
                        ) == true
                    }

                } else {

                    null
                }

            val target =
                existingCurrent
                    ?: directory.createFile(
                        "application/json",
                        currentName
                    )
                    ?: return ApeSaveResult.Error(
                        context.getString(R.string.ape_error_create_file)
                    )

            context.contentResolver
                .openOutputStream(target.uri, "wt")
                ?.use { stream ->

                    stream.write(
                        json.toByteArray(Charsets.UTF_8)
                    )
                }
                ?: return ApeSaveResult.Error(
                    context.getString(R.string.ape_error_open_stream)
                )

            // El .aero nuevo ya está a salvo — recién ahora es
            // seguro borrar el .ape viejo que se migró.
            legacyToDelete?.delete()

            ApeSaveResult.Success(
                context.getString(
                    R.string.ape_saved_via_folder,
                    parentPath,
                    currentName
                )
            )

        } catch (e: Exception) {

            ApeSaveResult.Error(
                e.message ?: context.getString(R.string.ape_error_unknown)
            )
        }
    }

    /**
     * Busca un archivo de efectos ya existente junto a la canción
     * (con cualquiera de las dos extensiones, ver
     * [ApeRepository.findEffectsCandidate]) para decidir dónde
     * guardar:
     *
     * - Si ya hay un `.aero`, se sobrescribe ese mismo archivo.
     * - Si solo hay un `.ape` (formato anterior), se migra: el
     *   nuevo contenido se escribe en un `.aero` con el mismo
     *   nombre base, y el `.ape` viejo se devuelve en
     *   [Resolution.legacyFileToDelete] para que quien llama lo
     *   borre recién después de confirmar que el `.aero` nuevo se
     *   escribió bien — nunca se borra el original antes de tener
     *   el reemplazo a salvo.
     * - Si no hay ninguno, se crea un `.aero` nuevo.
     */
    private data class Resolution(
        val target: File,
        val legacyFileToDelete: File?
    )

    private fun resolveTargetFile(
        parent: File,
        baseName: String
    ): Resolution {

        val existing =
            ApeRepository.findEffectsCandidate(
                parent.listFiles()?.toList() ?: emptyList(),
                baseName
            )

        return when {

            existing == null ->
                Resolution(
                    target =
                        File(
                            parent,
                            "$baseName.${ApeRepository.CURRENT_EXTENSION}"
                        ),
                    legacyFileToDelete = null
                )

            existing.extension.equals(
                ApeRepository.CURRENT_EXTENSION,
                ignoreCase = true
            ) ->
                Resolution(
                    target = existing,
                    legacyFileToDelete = null
                )

            else ->
                // Extensión legacy (.ape): migrar.
                Resolution(
                    target =
                        File(
                            parent,
                            "$baseName.${ApeRepository.CURRENT_EXTENSION}"
                        ),
                    legacyFileToDelete = existing
                )
        }
    }

    private fun buildJson(
        events: List<ApeEvent>
    ): String {

        val root = JSONObject()

        root.put("version", 1)

        val array = JSONArray()

        for (event in events.sortedBy { it.time }) {

            val eventObject = JSONObject()

            eventObject.put("time", event.time)
            eventObject.put("effect", event.effect)
            eventObject.put("duration", event.duration)

            array.put(eventObject)
        }

        root.put("effects", array)

        return root.toString(2)
    }
}
