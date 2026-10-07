package com.darktubbie.aeroplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

/**
 * Clave del `MediaMetadata.extras` donde [PlayerRepository.playQueue]
 * guarda la ruta del archivo de audio (Fase 5, 0.6.0), que es lo que
 * necesita [com.darktubbie.aeroplayer.AlbumArtCache] para la carátula.
 */
const val EXTRA_TRACK_PATH = "aero_track_path"

/**
 * Comandos de reproducción compartidos (Fase 5, 0.6.0 — Aero Widget).
 *
 * [PlayerRepository] opera sobre un `MediaController` (que es un
 * [Player]) y el widget opera directamente sobre el [Player] real
 * que vive dentro de [PlaybackService]. Como ambos son un [Player],
 * las reglas que no son un simple llamado (alternar play/pausa, el
 * orden del ciclo de repeat) viven acá una sola vez para que el
 * widget y la app nunca se comporten distinto — no hay un segundo
 * sistema de reproducción.
 */
object PlayerCommands {

    fun togglePlayPause(player: Player) {

        if (player.isPlaying) {

            player.pause()

        } else {

            player.play()
        }
    }

    /**
     * Ciclo: apagado -> repetir todo -> repetir una -> apagado.
     */
    fun nextRepeatMode(current: Int): Int =
        when (current) {

            Player.REPEAT_MODE_OFF ->
                Player.REPEAT_MODE_ALL

            Player.REPEAT_MODE_ALL ->
                Player.REPEAT_MODE_ONE

            else ->
                Player.REPEAT_MODE_OFF
        }

    /**
     * Reanudación en frío (Fase 6, 0.6.0): el Aero Widget y el
     * Quick Settings Tile pueden tocarse con [PlaybackService]
     * completamente detenido (el sistema lo mató al no haber
     * reproducción activa). [PlaybackService.currentPlayer] da null
     * en ese caso porque no hay ningún [Player] vivo — no porque no
     * haya nada para reproducir.
     *
     * Esta función arranca el servicio conectando un
     * [MediaController] normal (el mismo mecanismo que ya usa
     * [PlayerRepository] desde la app) — no crea un segundo
     * reproductor. Al arrancar, [PlaybackService.onCreate] ya deja
     * la última cola cargada en pausa, así que solo hace falta
     * pedir play(). El controller se libera enseguida: es un canal
     * de control, no el reproductor — soltarlo no detiene nada.
     *
     * Si ya hay un [Player] vivo (el caso normal, con la app
     * abierta o recién cerrada), [onReady] recibe ESE Player
     * directamente y no se crea ningún controller.
     */
    fun playOrResume(
        context: Context,
        onReady: (Player) -> Unit
    ) {

        PlaybackService.currentPlayer()?.let { alreadyRunning ->
            onReady(alreadyRunning)
            return
        }

        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

        val future =
            MediaController.Builder(context, sessionToken)
                .buildAsync()

        future.addListener(
            {
                runCatching { future.get() }
                    .getOrNull()
                    ?.let { controller ->

                        controller.play()

                        onReady(controller)

                        MediaController.releaseFuture(future)
                    }
            },
            MoreExecutors.directExecutor()
        )
    }
}
