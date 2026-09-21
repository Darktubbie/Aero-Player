package com.darktubbie.aeroplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Una entrada de historial (Fase 4, 0.4.x): qué canción sonó y
 * cuándo. Se identifica por [trackKey] (el mismo fingerprint que
 * usan las playlists — título+artista+álbum+duración), no por path,
 * por la misma razón: sobrevive a que el archivo se mueva de
 * carpeta.
 */
data class HistoryEntry(
    val trackKey: String,
    val playedAtMs: Long
)

/**
 * Persistencia del historial de reproducción (Fase 4, 0.4.x).
 *
 * Mismo enfoque que Favoritos/Playlists: JSON en el SharedPreferences
 * "aero_player" ya existente.
 *
 * [MAX_ENTRIES] evita el crecimiento ilimitado que pide el brief:
 * 300 entradas son semanas de escucha normal antes de empezar a
 * descartar las más viejas, y a esa escala el archivo de
 * SharedPreferences sigue siendo trivialmente chico. Se recorta en
 * cada guardado, no solo al leer, para que el archivo en disco
 * nunca crezca más allá de ese límite.
 */
class HistoryRepository(
    context: Context
) {

    companion object {
        private const val MAX_ENTRIES = 300
    }

    private val preferences by lazy {
        context.getSharedPreferences(
            "aero_player",
            Context.MODE_PRIVATE
        )
    }

    /**
     * Más reciente primero.
     */
    fun loadHistory(): List<HistoryEntry> {

        val raw =
            preferences.getString(
                "playback_history",
                null
            ) ?: return emptyList()

        return try {

            val jsonArray =
                JSONArray(raw)

            val result =
                mutableListOf<HistoryEntry>()

            for (i in 0 until jsonArray.length()) {

                val json =
                    jsonArray.getJSONObject(i)

                result.add(
                    HistoryEntry(
                        trackKey = json.getString("trackKey"),
                        playedAtMs = json.getLong("playedAtMs")
                    )
                )
            }

            result

        } catch (_: Exception) {

            emptyList()
        }
    }

    private fun saveHistory(
        entries: List<HistoryEntry>
    ) {

        val jsonArray = JSONArray()

        entries.take(MAX_ENTRIES).forEach { entry ->

            val json = JSONObject()

            json.put("trackKey", entry.trackKey)
            json.put("playedAtMs", entry.playedAtMs)

            jsonArray.put(json)
        }

        preferences.edit()
            .putString(
                "playback_history",
                jsonArray.toString()
            )
            .apply()
    }

    /**
     * Agrega una entrada al principio (más reciente primero) y
     * recorta a [MAX_ENTRIES]. No deduplica entradas del mismo
     * [trackKey]: escuchar la misma canción varias veces en días
     * distintos son eventos de historial legítimamente distintos,
     * a diferencia de Favoritos/Playlists donde la canción es un
     * miembro único de una colección.
     */
    fun addEntry(
        trackKey: String,
        playedAtMs: Long,
        currentHistory: List<HistoryEntry>
    ): List<HistoryEntry> {

        val updated =
            (
                listOf(
                    HistoryEntry(trackKey, playedAtMs)
                ) + currentHistory
            ).take(MAX_ENTRIES)

        saveHistory(updated)

        return updated
    }

    fun clearHistory(): List<HistoryEntry> {

        saveHistory(emptyList())

        return emptyList()
    }
}
