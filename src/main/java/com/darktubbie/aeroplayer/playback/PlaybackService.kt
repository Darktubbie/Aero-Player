package com.darktubbie.aeroplayer.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.darktubbie.aeroplayer.data.SettingsRepository

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

    private lateinit var settingsRepository: SettingsRepository

    /*
     * Fase 1 (0.4.x): pausar automáticamente al perder la salida de
     * audio actual (p. ej. se desconectan los audífonos/altavoz
     * Bluetooth mientras suena música).
     *
     * Se usa ACTION_AUDIO_BECOMING_NOISY en vez de un sistema propio
     * de detección de Bluetooth: es el mecanismo oficial de Android
     * para exactamente este escenario (cualquier salida de audio
     * "privada" que se pierde y haría que el sonido saltara al
     * altavoz del teléfono sin avisar), cubre tanto Bluetooth como
     * audífonos con cable, y no depende de sondear el estado de
     * Bluetooth por nuestra cuenta.
     */
    private val becomingNoisyReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action ==
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                    settingsRepository
                        .isPauseOnBluetoothDisconnectEnabled()
                ) {

                    mediaSession
                        ?.player
                        ?.pause()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        settingsRepository =
            SettingsRepository(this)

        val player =
            ExoPlayer.Builder(this)
                .build()

        mediaSession =
            MediaSession.Builder(
                this,
                player
            ).build()

        val filter =
            IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                becomingNoisyReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                becomingNoisyReceiver,
                filter
            )
        }
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

        try {

            unregisterReceiver(
                becomingNoisyReceiver
            )

        } catch (_: IllegalArgumentException) {

            // Ya estaba sin registrar (p. ej. onCreate nunca
            // llegó a completarse) — no hay nada que deshacer.
        }

        mediaSession?.run {

            player.release()

            release()

            mediaSession = null
        }

        super.onDestroy()
    }
}
