package com.darktubbie.aeroplayer.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.media3.common.Player

/**
 * Mantiene el Aero Widget al día mientras vive [com.darktubbie.aeroplayer.playback.PlaybackService]
 * (Fase 5, 0.6.0): repinta el widget completo en cada evento
 * discreto del Player (cambio de pista, play/pausa, shuffle, repeat,
 * seek…) y, solo mientras suena algo, avanza el progreso una vez por
 * segundo con una actualización liviana.
 *
 * Cuidado de batería: sin ningún widget colocado no hace nada, y con
 * la pantalla apagada no se envían actualizaciones de progreso (nadie
 * las está viendo); al encender la pantalla el siguiente segundo ya
 * las retoma.
 */
class AeroWidgetSync(
    private val context: Context,
    private val player: Player
) : Player.Listener {

    private val handler = Handler(Looper.getMainLooper())

    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE)
            as PowerManager

    private val ticker =
        object : Runnable {

            override fun run() {

                if (!player.isPlaying) {
                    return
                }

                if (powerManager.isInteractive) {
                    AeroWidget.tick(context)
                }

                handler.postDelayed(this, TICK_MS)
            }
        }

    fun attach() {

        player.addListener(this)

        AeroWidget.refresh(context)

        restartTicker()
    }

    fun release() {

        player.removeListener(this)

        handler.removeCallbacks(ticker)
    }

    override fun onEvents(
        player: Player,
        events: Player.Events
    ) {

        if (
            events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_TIMELINE_CHANGED
            )
        ) {

            AeroWidget.refresh(context)

            restartTicker()
        }
    }

    private fun restartTicker() {

        handler.removeCallbacks(ticker)

        if (player.isPlaying) {

            handler.postDelayed(ticker, TICK_MS)
        }
    }

    private companion object {

        const val TICK_MS = 1000L
    }
}
