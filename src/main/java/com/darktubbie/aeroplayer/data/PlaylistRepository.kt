package com.darktubbie.aeroplayer.data

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia y operaciones de playlists locales (Fase 3, 0.4.x).
 *
 * Mismo enfoque que LibraryRepository: JSON dentro del archivo
 * SharedPreferences "aero_player" ya existente, sin Room — el
 * proyecto no lo usaba antes de esta fase y el volumen de datos
 * (unas pocas playlists con listas de fingerprints) no lo justifica.
 *
 * Todas las funciones de modificación reciben la lista actual y
 * devuelven la lista actualizada ya persistida, siguiendo el mismo
 * patrón que FolderRepository — quien las llama (MainViewModel)
 * sigue siendo dueño del estado en memoria.
 */
class PlaylistRepository(
    context: Context
) {

    private val preferences by lazy {
        context.getSharedPreferences(
            "aero_player",
            Context.MODE_PRIVATE
        )
    }

    fun loadPlaylists(): List<Playlist> {

        val raw =
            preferences.getString(
                "playlists",
                null
            ) ?: return emptyList()

        return try {

            val jsonArray =
                JSONArray(raw)

            val result =
                mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {

                val json =
                    jsonArray.getJSONObject(i)

                val keysJson =
                    json.getJSONArray("trackKeys")

                val keys =
                    mutableListOf<String>()

                for (k in 0 until keysJson.length()) {
                    keys.add(keysJson.getString(k))
                }

                result.add(
                    Playlist(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        trackKeys = keys
                    )
                )
            }

            result

        } catch (_: Exception) {

            emptyList()
        }
    }

    private fun savePlaylists(
        playlists: List<Playlist>
    ) {

        val jsonArray = JSONArray()

        playlists.forEach { playlist ->

            val json = JSONObject()

            json.put("id", playlist.id)
            json.put("name", playlist.name)

            val keysJson = JSONArray()

            playlist.trackKeys.forEach {
                keysJson.put(it)
            }

            json.put("trackKeys", keysJson)

            jsonArray.put(json)
        }

        preferences.edit()
            .putString(
                "playlists",
                jsonArray.toString()
            )
            .apply()
    }

    fun createPlaylist(
        name: String,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val trimmedName =
            name.trim()

        if (trimmedName.isEmpty()) {
            return currentPlaylists
        }

        val updated =
            currentPlaylists + Playlist(
                id = UUID.randomUUID().toString(),
                name = trimmedName
            )

        savePlaylists(updated)

        return updated
    }

    fun renamePlaylist(
        playlistId: String,
        newName: String,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val trimmedName =
            newName.trim()

        if (trimmedName.isEmpty()) {
            return currentPlaylists
        }

        val updated =
            currentPlaylists.map {
                if (it.id == playlistId) {
                    it.copy(name = trimmedName)
                } else {
                    it
                }
            }

        savePlaylists(updated)

        return updated
    }

    fun deletePlaylist(
        playlistId: String,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val updated =
            currentPlaylists.filterNot {
                it.id == playlistId
            }

        savePlaylists(updated)

        return updated
    }

    /**
     * Agrega [trackKey] al final de la playlist si todavía no
     * estaba — evita duplicados accidentales de la misma canción
     * dentro de una misma playlist, tal como pide el brief.
     */
    fun addTrack(
        playlistId: String,
        trackKey: String,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val updated =
            currentPlaylists.map { playlist ->

                if (
                    playlist.id == playlistId &&
                    !playlist.trackKeys.contains(trackKey)
                ) {

                    playlist.copy(
                        trackKeys =
                            playlist.trackKeys + trackKey
                    )

                } else {

                    playlist
                }
            }

        savePlaylists(updated)

        return updated
    }

    fun removeTrack(
        playlistId: String,
        trackKey: String,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val updated =
            currentPlaylists.map { playlist ->

                if (playlist.id == playlistId) {

                    playlist.copy(
                        trackKeys =
                            playlist.trackKeys.filterNot {
                                it == trackKey
                            }
                    )

                } else {

                    playlist
                }
            }

        savePlaylists(updated)

        return updated
    }

    /**
     * Reemplaza el orden completo de fingerprints de una playlist.
     * Usado para mover una canción una posición hacia arriba/abajo
     * (ver PlaylistDetailScreen): quien llama arma la nueva lista
     * ya reordenada y esta función solo la persiste.
     */
    fun reorderTracks(
        playlistId: String,
        newOrder: List<String>,
        currentPlaylists: List<Playlist>
    ): List<Playlist> {

        val updated =
            currentPlaylists.map { playlist ->

                if (playlist.id == playlistId) {
                    playlist.copy(trackKeys = newOrder)
                } else {
                    playlist
                }
            }

        savePlaylists(updated)

        return updated
    }
}
