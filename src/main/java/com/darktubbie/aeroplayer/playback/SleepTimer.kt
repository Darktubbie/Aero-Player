package com.darktubbie.aeroplayer.playback

/**
 * Modalidad activa del Sleep Timer (Fase 1, 0.4.x).
 *
 * No se persiste entre reinicios de la app a propósito: es una
 * acción puntual del usuario para esta sesión de escucha, no una
 * preferencia duradera.
 */
sealed class SleepTimerMode {

    /** Se pausa cuando [totalMs] termina de transcurrir. */
    data class ByTime(
        val totalMs: Long
    ) : SleepTimerMode()

    /** Se pausa después de que terminen [totalSongs] canciones. */
    data class BySongs(
        val totalSongs: Int
    ) : SleepTimerMode()
}

/**
 * Estado observable del Sleep Timer, expuesto por [MainViewModel][
 * com.darktubbie.aeroplayer.MainViewModel] a la UI.
 *
 * [mode] null significa "sin temporizador activo". Vive en el
 * ViewModel (no en un Composable) para sobrevivir a la navegación
 * entre pantallas y a las recomposiciones, tal como pide el brief.
 */
data class SleepTimerState(
    val mode: SleepTimerMode? = null,
    val remainingMs: Long = 0L,
    val remainingSongs: Int = 0
) {

    val isActive: Boolean
        get() = mode != null
}
