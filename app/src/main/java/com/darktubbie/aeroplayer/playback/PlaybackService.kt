package com.darktubbie.aeroplayer.playback

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio Media3 que aloja el [ExoPlayer] real y la [MediaSession]
 * asociada.
 *
 * Ser una MediaSessionService (en vez de manejar el ExoPlayer
 * directamente desde la Activity/ViewModel) es lo que permite:
 *
 * - Que la reproducción siga sonando al navegar por la app o al
 *   ponerla en segundo plano.
 * - Preparar de forma nativa los controles multimedia del sistema
 *   y, más adelante, la notificación de reproducción — Media3 los
 *   genera automáticamente a partir de esta sesión, sin código
 *   adicional nuestro.
 *
 * No conoce nada de AeroPlayer (ni AudioTrack, ni repositorios):
 * solo expone la sesión para que PlayerRepository se conecte a
 * ella mediante un MediaController.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player =
            ExoPlayer.Builder(this)
                .build()

        mediaSession =
            MediaSession.Builder(
                this,
                player
            ).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    /**
     * Si el usuario quita Aero Player de la lista de apps
     * recientes mientras NO hay reproducción activa, no hay
     * motivo para que el servicio (con su ExoPlayer y su
     * MediaSession) siga vivo en memoria — sin esto se quedaría
     * ahí indefinidamente hasta que el sistema decida matarlo por
     * presión de memoria. Si SÍ está sonando, se deja vivo a
     * propósito: es justo lo que permite que la música siga
     * sonando en segundo plano.
     */
    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        val player =
            mediaSession?.player

        if (
            player == null ||
            !player.playWhenReady
        ) {

            stopSelf()
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {

        mediaSession?.run {

            player.release()

            release()

            mediaSession = null
        }

        super.onDestroy()
    }
}
