package com.darktubbie.aeroplayer.ape

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.darktubbie.aeroplayer.data.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Resultado de intentar guardar un `.ape` (Fase 8 del plan de
 * evolución visual: editor básico de APE).
 */
sealed class ApeSaveResult {
    data class Success(val path: String) : ApeSaveResult()
    data class Error(val message: String) : ApeSaveResult()
}

/**
 * Escribe/reemplaza el `.ape` hermano de una canción.
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
                trySaveDirectly(audioFile, json)

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

            target.writeText(json, Charsets.UTF_8)

            if (
                !target.exists() ||
                target.length() == 0L
            ) {

                return ApeSaveResult.Error(
                    "Se intentó guardar en " +
                    "${target.absolutePath} pero el archivo no " +
                    "quedó ahí después de escribirlo."
                )
            }

            ApeSaveResult.Success(target.absolutePath)

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
                        "No se pudo resolver la carpeta de la canción."
                    )

            val folderRepository =
                FolderRepository(context)

            val matched =
                folderRepository.findContainingFolder(
                    audioFile.absolutePath,
                    selectedFolders
                ) ?: return ApeSaveResult.Error(
                    "No se pudo guardar de forma directa y esta " +
                    "canción tampoco está dentro de ninguna " +
                    "carpeta agregada en Más > Carpetas (ruta: " +
                    "$parentPath). Quita y vuelve a agregar esa " +
                    "carpeta para conceder permiso de escritura, " +
                    "o agrégala si todavía no está."
                )

            var directory: DocumentFile =
                DocumentFile.fromTreeUri(
                    context,
                    matched.treeUri
                ) ?: return ApeSaveResult.Error(
                    "No se pudo acceder a la carpeta seleccionada."
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
                                "No se encontró la subcarpeta " +
                                "\"$segment\" dentro del permiso " +
                                "concedido (raíz: " +
                                "${matched.rootPath}, canción en: " +
                                "$parentPath)."
                            )

                    if (!next.isDirectory) {

                        return ApeSaveResult.Error(
                            "Ruta inválida dentro de la carpeta " +
                            "seleccionada."
                        )
                    }

                    directory = next
                }
            }

            val fileName =
                "${audioFile.nameWithoutExtension}.ape"

            // Insensible a mayúsculas, igual que en la lectura
            // (ApeRepository): si ya existe un .ape para esta
            // canción con cualquier combinación de mayúsculas, se
            // sobrescribe ese en vez de crear uno duplicado.
            val existing =
                directory.listFiles().firstOrNull {

                    it.isFile &&
                    it.name?.equals(
                        fileName,
                        ignoreCase = true
                    ) == true
                }

            val target =
                existing
                    ?: directory.createFile(
                        "application/json",
                        fileName
                    )
                    ?: return ApeSaveResult.Error(
                        "No se pudo crear el archivo .ape."
                    )

            context.contentResolver
                .openOutputStream(target.uri, "wt")
                ?.use { stream ->

                    stream.write(
                        json.toByteArray(Charsets.UTF_8)
                    )
                }
                ?: return ApeSaveResult.Error(
                    "No se pudo abrir el archivo para escribir."
                )

            ApeSaveResult.Success(
                "$parentPath/$fileName (vía carpeta concedida)"
            )

        } catch (e: Exception) {

            ApeSaveResult.Error(
                e.message ?: "Error desconocido al guardar."
            )
        }
    }

    /**
     * Busca un `.ape` ya existente junto a la canción (insensible a
     * mayúsculas, igual que [ApeRepository]) para sobrescribirlo en
     * vez de crear uno duplicado con otra combinación de mayúsculas.
     */
    private fun resolveTargetFile(
        parent: File,
        baseName: String
    ): File {

        val existing =
            parent.listFiles { candidate ->

                candidate.isFile &&
                candidate.extension.equals(
                    "ape",
                    ignoreCase = true
                ) &&
                candidate.nameWithoutExtension.equals(
                    baseName,
                    ignoreCase = true
                )
            }?.firstOrNull()

        return existing
            ?: File(parent, "$baseName.ape")
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
