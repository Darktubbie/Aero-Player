package com.darktubbie.aeroplayer.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.darktubbie.aeroplayer.data.SettingsRepository
import com.darktubbie.aeroplayer.widget.AeroWidget
import com.darktubbie.aeroplayer.widget.AeroWidgetSync
import androidx.media3.common.Player
import java.lang.ref.WeakReference

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

    private var widgetSync: AeroWidgetSync? = null

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

        restoreLastQueue(player)

        // Fase 5 (0.6.0): el widget lee y controla este mismo
        // Player — no existe ningún reproductor paralelo.
        playerRef = WeakReference(player)

        widgetSync =
            AeroWidgetSync(this, player).also {
                it.attach()
            }

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

    /**
     * Reanudación en frío (Fase 6, 0.6.0): cuando el sistema mató
     * el proceso por no haber reproducción activa
     * ([onTaskRemoved]), el próximo arranque de este servicio (por
     * un toque en el Aero Widget o el Quick Settings Tile) no tiene
     * ningún [Player] vivo del que partir. Se restaura acá la
     * última cola guardada — EN PAUSA (`playWhenReady` nunca se
     * toca, queda en su default `false`): que el servicio exista de
     * nuevo no significa que el usuario pidió reproducir, solo deja
     * lista la cola para que el próximo toque de play la continúe
     * en vez de mandar a elegir de nuevo desde la app.
     */
    private fun restoreLastQueue(player: ExoPlayer) {

        val (tracks, index, positionMs) =
            settingsRepository.loadLastQueue()

        if (tracks.isEmpty()) {
            return
        }

        val mediaItems =
            tracks.map { track ->

                MediaItem.Builder()
                    .setMediaId(track.uri)
                    .setUri(track.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setExtras(
                                Bundle().apply {
                                    putString(
                                        EXTRA_TRACK_PATH,
                                        track.path
                                    )
                                }
                            )
                            .build()
                    )
                    .build()
            }

        player.setMediaItems(
            mediaItems,
            index,
            positionMs
        )

        player.prepare()
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

        widgetSync?.release()
        widgetSync = null
        playerRef = null

        // Sin servicio no hay reproducción: el widget vuelve a su
        // estado de reposo en vez de quedarse con la última canción.
        AeroWidget.refresh(this)

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

    companion object {

        @Volatile
        private var playerRef: WeakReference<Player>? = null

        /**
         * Player real del servicio, o null si el servicio no está
         * vivo. Solo debe usarse desde el hilo principal (el mismo
         * hilo de aplicación con el que ExoPlayer fue creado).
         */
        fun currentPlayer(): Player? =
            playerRef?.get()
    }
}
