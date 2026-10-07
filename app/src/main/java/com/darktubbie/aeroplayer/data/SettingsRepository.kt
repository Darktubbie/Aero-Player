package com.darktubbie.aeroplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferencias globales de reproducción que no encajan dentro de
 * FolderRepository ni LibraryRepository (Fase 1, 0.4.x).
 *
 * Comparte el mismo archivo SharedPreferences ("aero_player") que
 * los demás repositorios: es solo una separación de responsabilidad
 * en el código, no un almacenamiento distinto.
 *
 * Nace con una sola preferencia (pausar al desconectar Bluetooth),
 * pensada para crecer cuando llegue la Fase 5 (Settings real) sin
 * tener que reorganizar nada.
 */
class SettingsRepository(
    context: Context
) {

    private val preferences by lazy {
        context.getSharedPreferences(
            "aero_player",
            Context.MODE_PRIVATE
        )
    }

    /**
     * Por defecto activado: es el comportamiento que la mayoría de
     * la gente espera (que la música no siga sonando por el
     * altavoz del teléfono si se estaba escuchando por Bluetooth).
     */
    fun isPauseOnBluetoothDisconnectEnabled(): Boolean {

        return preferences.getBoolean(
            "pause_on_bluetooth_disconnect",
            true
        )
    }

    fun setPauseOnBluetoothDisconnectEnabled(
        enabled: Boolean
    ) {

        preferences.edit()
            .putBoolean(
                "pause_on_bluetooth_disconnect",
                enabled
            )
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * INTENSIDAD DE EFECTOS AMBIENTALES (Fase 5, 0.4.x)
     * ---------------------------------------------------------
     */

    fun loadAmbientIntensityName(): String? {

        return preferences.getString(
            "ambient_intensity",
            null
        )
    }

    fun saveAmbientIntensityName(
        name: String
    ) {

        preferences.edit()
            .putString("ambient_intensity", name)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * TEMA (Aero claro / Dark Aero) — post-0.4.0
     * ---------------------------------------------------------
     */

    fun loadThemeName(): String? {

        return preferences.getString(
            "app_theme",
            null
        )
    }

    fun saveThemeName(
        name: String
    ) {

        preferences.edit()
            .putString("app_theme", name)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * IDIOMA (Fase 2, 0.5.0)
     * ---------------------------------------------------------
     *
     * Guarda "es" o "en". Nulo significa "todavía no elegido" —
     * en ese caso [com.darktubbie.aeroplayer.MainActivity] usa
     * español como default explícito (no el locale del sistema),
     * para que el idioma de la app sea siempre determinístico y no
     * dependa de en qué idioma tenga el usuario el teléfono.
     */

    fun loadLanguageCode(): String? {

        return preferences.getString(
            "app_language",
            null
        )
    }

    fun saveLanguageCode(
        code: String
    ) {

        preferences.edit()
            .putString("app_language", code)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * SHUFFLE / REPEAT PERSISTENTES (Fase 5, 0.4.x)
     * ---------------------------------------------------------
     *
     * Antes de esta fase, shuffle/repeat solo vivían en el
     * ExoPlayer de PlaybackService: sobrevivían mientras el
     * servicio seguía vivo, pero se perdían en un reinicio en frío
     * de la app (ver PlayerRepository.pendingShuffleRestore, que es
     * quien realmente los restaura al conectar).
     */

    fun isShuffleEnabled(): Boolean {

        return preferences.getBoolean(
            "shuffle_enabled",
            false
        )
    }

    fun setShuffleEnabled(
        enabled: Boolean
    ) {

        preferences.edit()
            .putBoolean("shuffle_enabled", enabled)
            .apply()
    }

    /**
     * Uno de los `Player.REPEAT_MODE_*` de Media3 (0 = OFF por
     * defecto, el mismo valor que trae un ExoPlayer recién creado).
     */
    fun getRepeatMode(): Int {

        return preferences.getInt(
            "repeat_mode",
            0
        )
    }

    fun setRepeatMode(
        mode: Int
    ) {

        preferences.edit()
            .putInt("repeat_mode", mode)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * MODO AERO DESKTOP (Fase 1, 0.6.0)
     * ---------------------------------------------------------
     *
     * Nulo significa "nunca elegido" → MainViewModel lo interpreta
     * como AeroDesktopMode.OFF, el mismo comportamiento que la app
     * ya tenía antes de esta fase (ver AeroDesktopMode.OFF).
     */

    fun loadAeroDesktopModeName(): String? {

        return preferences.getString(
            "aero_desktop_mode",
            null
        )
    }

    fun saveAeroDesktopModeName(
        name: String
    ) {

        preferences.edit()
            .putString("aero_desktop_mode", name)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * AERO REST MODE (Fase 7, 0.6.0)
     * ---------------------------------------------------------
     *
     * Opcional y apagado por defecto (igual que AeroDesktopMode):
     * actualizar a 0.6.0 no debe cambiar el comportamiento de nadie
     * sin que lo pida.
     */

    fun isRestModeEnabled(): Boolean {

        return preferences.getBoolean(
            "rest_mode_enabled",
            false
        )
    }

    fun setRestModeEnabled(
        enabled: Boolean
    ) {

        preferences.edit()
            .putBoolean("rest_mode_enabled", enabled)
            .apply()
    }

    /*
     * ---------------------------------------------------------
     * ÚLTIMA COLA (Fase 6, 0.6.0 — reanudación en frío)
     * ---------------------------------------------------------
     *
     * El Aero Widget y el Quick Settings Tile pueden tocarse con
     * PlaybackService completamente detenido (el sistema mató el
     * proceso al no haber reproducción activa — ver
     * `PlaybackService.onTaskRemoved`). En ese momento no existe
     * ningún `Player` al que preguntarle "qué estabas escuchando",
     * así que la última cola se persiste acá cada vez que cambia,
     * y `PlaybackService.onCreate()` la restaura (en pausa, sin
     * arrancar sola) para que haya algo que reanudar con un solo
     * toque en vez de mandar a la app a elegir de nuevo.
     *
     * Se guardan solo los 5 campos que hacen falta para reconstruir
     * los `MediaItem` (uri/title/artist/album/path) — no un
     * `AudioTrack` completo — en un JSON simple con `org.json`, ya
     * incluido en el SDK de Android: no hace falta agregar ninguna
     * dependencia nueva para esto.
     */

    fun saveLastQueue(
        tracks: List<AudioTrack>,
        index: Int
    ) {

        val array = JSONArray()

        tracks.forEach { track ->

            array.put(
                JSONObject().apply {
                    put("uri", track.uri)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("path", track.path)
                }
            )
        }

        preferences.edit()
            .putString("last_queue", array.toString())
            .putInt("last_queue_index", index)
            .apply()
    }

    /**
     * Solo actualiza el índice (p. ej. al saltar de canción): no
     * reescribe toda la cola guardada.
     */
    fun saveLastQueueIndex(
        index: Int
    ) {

        preferences.edit()
            .putInt("last_queue_index", index)
            .apply()
    }

    fun saveLastPositionMs(
        positionMs: Long
    ) {

        preferences.edit()
            .putLong("last_position_ms", positionMs)
            .apply()
    }

    fun loadLastQueue(): Triple<List<AudioTrack>, Int, Long> {

        val raw =
            preferences.getString("last_queue", null)
                ?: return Triple(emptyList(), 0, 0L)

        val tracks =
            runCatching {

                val array = JSONArray(raw)

                (0 until array.length()).map { i ->

                    val item = array.getJSONObject(i)

                    AudioTrack(
                        uri = item.getString("uri"),
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        album = item.getString("album"),
                        duration = 0L,
                        path = item.getString("path"),
                        albumArtPath = null
                    )
                }

            }.getOrDefault(emptyList())

        val index =
            preferences.getInt("last_queue_index", 0)
                .coerceIn(0, (tracks.size - 1).coerceAtLeast(0))

        val positionMs =
            preferences.getLong("last_position_ms", 0L)

        return Triple(tracks, index, positionMs)
    }
}
