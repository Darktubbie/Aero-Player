package com.darktubbie.aeroplayer.data

import android.content.Context

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
}
