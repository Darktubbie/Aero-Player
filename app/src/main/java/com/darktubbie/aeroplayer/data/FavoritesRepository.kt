package com.darktubbie.aeroplayer.data

import android.content.Context

/**
 * Persistencia de canciones favoritas (Fase 2, 0.4.x).
 *
 * Igual que FolderRepository/SettingsRepository, comparte el mismo
 * archivo SharedPreferences ("aero_player") — no se agregó Room ni
 * ningún almacenamiento nuevo, el proyecto no lo usaba antes de esta
 * fase y un Set<String> es más que suficiente para esto.
 *
 * Se identifica cada canción por [AudioTrack.path] (ruta real de
 * archivo), no por [AudioTrack.uri]: el URI viene de MediaStore y su
 * _ID puede llegar a cambiar si el archivo se elimina y se vuelve a
 * indexar, mientras que la ruta identifica al mismo archivo físico
 * de forma estable entre escaneos. Como efecto secundario, esto
 * también resuelve solo "canciones duplicadas" mencionado en el
 * brief: dos AudioTrack con el mismo path son, por definición, el
 * mismo archivo.
 */
class FavoritesRepository(
    context: Context
) {

    private val preferences by lazy {
        context.getSharedPreferences(
            "aero_player",
            Context.MODE_PRIVATE
        )
    }

    fun loadFavoritePaths(): Set<String> {

        return preferences
            .getStringSet(
                "favorite_paths",
                emptySet()
            )
            ?: emptySet()
    }

    private fun saveFavoritePaths(
        paths: Set<String>
    ) {

        preferences.edit()
            .putStringSet(
                "favorite_paths",
                paths
            )
            .apply()
    }

    /**
     * Alterna el estado de favorito de [path] y devuelve el
     * conjunto actualizado, ya persistido.
     */
    fun toggleFavorite(
        path: String,
        currentFavorites: Set<String>
    ): Set<String> {

        val updated =
            if (currentFavorites.contains(path)) {
                currentFavorites - path
            } else {
                currentFavorites + path
            }

        saveFavoritePaths(updated)

        return updated
    }
}
