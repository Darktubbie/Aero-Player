package com.darktubbie.aeroplayer.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Consulta MediaStore para encontrar música dentro de las carpetas
 * seleccionadas, y persiste/recupera la biblioteca resultante.
 *
 * Extraído de MainActivity sin cambiar comportamiento (Fase 2 del
 * plan): mismas claves de SharedPreferences, mismo formato de JSON,
 * misma consulta a MediaStore.
 */
class LibraryRepository(
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
     * BIBLIOTECA PERSISTENTE
     * ---------------------------------------------------------
     */

    fun saveTracks(
        tracks: List<AudioTrack>
    ) {

        try {

            val jsonArray = JSONArray()

            tracks.forEach { track ->

                val json =
                    JSONObject().apply {

                        put(
                            "uri",
                            track.uri
                        )

                        put(
                            "title",
                            track.title
                        )

                        put(
                            "artist",
                            track.artist
                        )

                        put(
                            "album",
                            track.album
                        )

                        put(
                            "duration",
                            track.duration
                        )

                        put(
                            "path",
                            track.path
                        )

                        put(
                            "albumArtPath",
                            track.albumArtPath ?: ""
                        )

                        put(
                            "dateModifiedMs",
                            track.dateModifiedMs
                        )
                    }

                jsonArray.put(json)
            }

            preferences.edit()
                .putString(
                    "music_library",
                    jsonArray.toString()
                )
                .apply()

        } catch (_: Exception) {
        }
    }

    fun loadTracks(): List<AudioTrack> {

        return try {

            val jsonString =
                preferences.getString(
                    "music_library",
                    null
                )
                    ?: return emptyList()

            val jsonArray =
                JSONArray(jsonString)

            val loadedTracks =
                mutableListOf<AudioTrack>()

            for (i in 0 until jsonArray.length()) {

                val json =
                    jsonArray.getJSONObject(i)

                loadedTracks.add(
                    AudioTrack(

                        uri =
                            json.optString(
                                "uri"
                            ),

                        title =
                            json.optString(
                                "title",
                                "Unknown title"
                            ),

                        artist =
                            json.optString(
                                "artist",
                                "Unknown artist"
                            ),

                        album =
                            json.optString(
                                "album",
                                "Unknown album"
                            ),

                        duration =
                            json.optLong(
                                "duration",
                                0L
                            ),

                        path =
                            json.optString(
                                "path"
                            ),

                        albumArtPath =
                            json.optString(
                                "albumArtPath"
                            )
                                .takeIf {
                                    it.isNotBlank()
                                },

                        // ausente en bibliotecas guardadas antes
                        // de la Fase 1 (0.4.x) -> 0L por defecto.
                        dateModifiedMs =
                            json.optLong(
                                "dateModifiedMs",
                                0L
                            )
                    )
                )
            }

            loadedTracks

        } catch (_: Exception) {

            emptyList()
        }
    }

    /*
     * ---------------------------------------------------------
     * PREFERENCIA DE ORDENAMIENTO (Fase 1, 0.4.x)
     * ---------------------------------------------------------
     */

    /**
     * Guarda el nombre del enum [com.darktubbie.aeroplayer.ui.library.SortOrder]
     * elegido, para recordarlo entre reinicios de la app.
     */
    fun saveSortOrderName(name: String) {

        preferences.edit()
            .putString("sort_order", name)
            .apply()
    }

    /**
     * Devuelve el nombre guardado del orden preferido, o null si
     * nunca se guardó ninguno (primera vez / instalación previa a
     * esta fase).
     */
    fun loadSortOrderName(): String? {

        return preferences.getString(
            "sort_order",
            null
        )
    }

    /*
     * ---------------------------------------------------------
     * ESCANEO MEDIASTORE
     * ---------------------------------------------------------
     */

    /**
     * Consulta MediaStore y devuelve solo las pistas cuya ruta cae
     * dentro de alguna de las carpetas seleccionadas.
     *
     * @param folderPaths rutas de archivo ya resueltas (ver
     * FolderRepository.resolveFolderPaths), no tree URIs.
     */
    fun queryMediaStore(
        folderPaths: List<String>
    ): List<AudioTrack> {

        val result =
            mutableListOf<AudioTrack>()

        if (folderPaths.isEmpty()) {
            return emptyList()
        }

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection =
            arrayOf(

                MediaStore.Audio.Media._ID,

                MediaStore.Audio.Media.TITLE,

                MediaStore.Audio.Media.ARTIST,

                MediaStore.Audio.Media.ALBUM,

                MediaStore.Audio.Media.DURATION,

                MediaStore.Audio.Media.DATA,

                // Fase 1 (0.4.x): base para el orden "Más
                // recientes primero". MediaStore la guarda en
                // segundos, no en milisegundos.
                MediaStore.Audio.Media.DATE_MODIFIED
            )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(

            collection,

            projection,

            selection,

            null,

            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

            val titleIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

            val artistIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

            val albumIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                )

            val durationIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                )

            val pathIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DATA
                )

            val dateModifiedIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DATE_MODIFIED
                )

            while (cursor.moveToNext()) {

                val path =
                    cursor.getString(
                        pathIndex
                    )
                        ?: continue

                val normalizedPath =
                    normalizePath(path)

                if (
                    !isInsideSelectedFolder(
                        normalizedPath,
                        folderPaths
                    )
                ) {
                    continue
                }

                val id =
                    cursor.getLong(
                        idIndex
                    )

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                val title =
                    cursor.getString(
                        titleIndex
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown title"

                val artist =
                    cursor.getString(
                        artistIndex
                    )
                        ?.takeIf {
                            it.isNotBlank() &&
                            it != "<unknown>"
                        }
                        ?: "Unknown artist"

                val album =
                    cursor.getString(
                        albumIndex
                    )
                        ?.takeIf {
                            it.isNotBlank() &&
                            it != "<unknown>"
                        }
                        ?: "Unknown album"

                val duration =
                    cursor.getLong(
                        durationIndex
                    )

                // DATE_MODIFIED viene en segundos desde epoch;
                // se normaliza a milisegundos para que sea
                // comparable con el resto de timestamps de la app.
                val dateModifiedMs =
                    cursor.getLong(
                        dateModifiedIndex
                    ) * 1000L

                result.add(
                    AudioTrack(

                        uri =
                            uri.toString(),

                        title =
                            title,

                        artist =
                            artist,

                        album =
                            album,

                        duration =
                            duration,

                        path =
                            path,

                        /*
                         * La portada se obtiene de forma
                         * completamente independiente del escaneo.
                         * AlbumArtCache se encarga de todo.
                         */
                        albumArtPath =
                            null,

                        dateModifiedMs =
                            dateModifiedMs
                    )
                )
            }
        }

        return result
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

    private fun isInsideSelectedFolder(
        filePath: String,
        folders: List<String>
    ): Boolean {

        for (
            folder in folders
        ) {

            val normalizedFolder =
                folder.trimEnd('/')

            if (
                filePath ==
                normalizedFolder ||
                filePath.startsWith(
                    "$normalizedFolder/"
                )
            ) {

                return true
            }
        }

        return false
    }
}
