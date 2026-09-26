package com.darktubbie.aeroplayer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract

/**
 * Gestiona las carpetas de música seleccionadas mediante SAF:
 * persistencia de la selección y resolución de un tree URI a una
 * ruta de archivo real, usada después para filtrar los resultados
 * de MediaStore por carpeta.
 *
 * Extraído de MainActivity sin cambiar comportamiento (Fase 2 del
 * plan). El repositorio no guarda el estado en memoria: recibe la
 * lista actual y devuelve la lista actualizada, para que quien lo
 * usa (hoy MainActivity, más adelante un ViewModel) siga siendo
 * dueño del estado.
 */
class FolderRepository(
    private val context: Context
) {

    private val preferences by lazy {
        context.getSharedPreferences(
            "aero_player",
            Context.MODE_PRIVATE
        )
    }

    /*
     * ---------------------------------------------------------
     * CARPETAS: PERSISTENCIA
     * ---------------------------------------------------------
     */

    fun loadFolders(): List<String> {

        return preferences
            .getStringSet(
                "music_folders",
                emptySet()
            )
            ?.toList()
            ?: emptyList()
    }

    private fun saveFolders(
        folders: List<String>
    ) {

        preferences.edit()
            .putStringSet(
                "music_folders",
                folders.toSet()
            )
            .apply()
    }

    /**
     * Toma permiso persistente sobre el árbol elegido y lo añade
     * a la selección si no estaba ya.
     *
     * Devuelve la lista actualizada de carpetas.
     */
    fun addFolder(
        uri: Uri,
        currentFolders: List<String>
    ): List<String> {

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,

                // Fase 8: además de lectura (para escanear música),
                // ahora también se persiste el permiso de escritura,
                // necesario para que el editor de .ape pueda crear/
                // sobrescribir archivos dentro de esta carpeta.
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        if (
            currentFolders.contains(
                uri.toString()
            )
        ) {
            return currentFolders
        }

        val updated =
            currentFolders + uri.toString()

        saveFolders(updated)

        return updated
    }

    /**
     * Quita una carpeta de la selección y persiste el cambio.
     *
     * Devuelve la lista actualizada de carpetas.
     */
    fun removeFolder(
        folder: String,
        currentFolders: List<String>
    ): List<String> {

        val updated =
            currentFolders.filterNot {
                it == folder
            }

        saveFolders(updated)

        return updated
    }

    /*
     * ---------------------------------------------------------
     * RESOLUCIÓN DE TREE URI -> RUTA DE ARCHIVO
     * ---------------------------------------------------------
     */

    /**
     * Busca, entre las carpetas SAF ya seleccionadas, la que
     * contiene el archivo dado (Fase 8: guardar un `.ape` junto a
     * su canción reutilizando el permiso persistente ya concedido
     * sobre esa carpeta, en vez de pedir uno nuevo).
     *
     * Si varias carpetas seleccionadas contuvieran el archivo (una
     * anidada dentro de otra), devuelve la más específica.
     */
    fun findContainingFolder(
        filePath: String,
        folders: List<String>
    ): MatchedFolder? {

        val normalizedFile =
            normalizePath(filePath)

        return folders
            .mapNotNull { folderUriString ->

                val uri =
                    Uri.parse(folderUriString)

                val rootPath =
                    treeUriToPath(uri)
                        ?.let { normalizePath(it) }
                        ?: return@mapNotNull null

                val contains =
                    normalizedFile == rootPath ||
                    normalizedFile.startsWith("$rootPath/")

                if (contains) {
                    MatchedFolder(uri, rootPath)
                } else {
                    null
                }
            }
            .maxByOrNull {
                it.rootPath.length
            }
    }

    /**
     * Convierte cada tree URI seleccionado en una ruta de archivo
     * normalizada, para poder filtrar después los resultados de
     * MediaStore por carpeta.
     */
    fun resolveFolderPaths(
        folders: List<String>
    ): List<String> {

        return folders
            .mapNotNull {
                treeUriToPath(
                    Uri.parse(it)
                )
            }
            .map {
                normalizePath(it)
            }
    }

    private fun treeUriToPath(
        uri: Uri
    ): String? {

        return try {

            val documentId =
                DocumentsContract
                    .getTreeDocumentId(uri)
                    ?: return null

            val separator =
                documentId.indexOf(':')

            if (separator == -1) {
                return null
            }

            val volume =
                documentId.substring(
                    0,
                    separator
                )

            val relativePath =
                documentId.substring(
                    separator + 1
                )

            if (
                volume.equals(
                    "primary",
                    true
                )
            ) {

                val root =
                    Environment
                        .getExternalStorageDirectory()
                        .absolutePath

                if (
                    relativePath.isBlank()
                ) {

                    root

                } else {

                    "$root/$relativePath"
                }

            } else {

                getStorageVolumePath(
                    volume
                )?.let { root ->

                    if (
                        relativePath.isBlank()
                    ) {

                        root

                    } else {

                        "$root/$relativePath"
                    }
                }
            }

        } catch (_: Exception) {

            null
        }
    }

    private fun getStorageVolumePath(
        uuid: String
    ): String? {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N
        ) {
            return null
        }

        val storageManager =
            context.getSystemService(
                Context.STORAGE_SERVICE
            ) as StorageManager

        for (
            volume in
            storageManager.storageVolumes
        ) {

            if (
                volume.uuid.equals(
                    uuid,
                    ignoreCase = true
                )
            ) {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R
                ) {

                    return volume
                        .directory
                        ?.absolutePath
                }
            }
        }

        return null
    }

    private fun normalizePath(
        path: String
    ): String {

        return path
            .replace(
                '\\',
                '/'
            )
            .trimEnd('/')
    }
}

/**
 * Una carpeta SAF ya seleccionada que contiene un archivo dado, con
 * su tree URI (para pedir permiso/crear archivos ahí) y su ruta
 * raíz ya resuelta (para calcular la subcarpeta relativa). Ver
 * [FolderRepository.findContainingFolder].
 */
data class MatchedFolder(
    val treeUri: Uri,
    val rootPath: String
)
