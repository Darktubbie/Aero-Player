package com.darktubbie.aeroplayer.widget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.common.Player
import com.darktubbie.aeroplayer.MainActivity
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.playback.PlaybackService
import com.darktubbie.aeroplayer.playback.PlayerCommands

/**
 * Quick Settings Tile de Aero Player (Fase 6, 0.6.0).
 *
 * A propósito NO es un Mini Player: es un toggle simple, igual al
 * de Musicolet — primer toque reanuda/inicia, segundo toque pausa y
 * el tile pasa a inactivo. No expone carátula, progreso ni
 * siguiente/anterior; eso ya lo cubren el Mini Player, Now Playing y
 * el Aero Widget.
 *
 * Sin reproductor propio: lee y controla el mismo [Player] real de
 * [PlaybackService] que usan la app y el Aero Widget, con
 * [PlayerCommands] — no hay ninguna cola ni estado paralelo.
 *
 * [onStartListening]/[onStopListening] delimitan la única ventana en
 * la que Android permite tocar [getQsTile]: mientras el panel de
 * Quick Settings tiene este tile visible. Fuera de esa ventana no
 * hay [Tile] al que escribirle, así que el listener del [Player]
 * solo vive mientras el panel está abierto — no hace falta (ni se
 * puede) mantener el tile al día en segundo plano.
 */
class AeroQuickSettingsTileService : TileService() {

    private var listenedPlayer: Player? = null

    private val playerListener =
        object : Player.Listener {

            override fun onEvents(
                player: Player,
                events: Player.Events
            ) {

                if (
                    events.containsAny(
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_PLAYBACK_STATE_CHANGED
                    )
                ) {

                    updateTile()
                }
            }
        }

    override fun onStartListening() {
        super.onStartListening()

        updateTile()

        PlaybackService.currentPlayer()?.let { player ->

            player.addListener(playerListener)

            listenedPlayer = player
        }
    }

    override fun onStopListening() {

        listenedPlayer?.removeListener(playerListener)
        listenedPlayer = null

        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val existingPlayer = PlaybackService.currentPlayer()

        when {

            existingPlayer != null &&
                existingPlayer.currentMediaItem != null -> {

                PlayerCommands.togglePlayPause(existingPlayer)

                updateTile()
            }

            existingPlayer == null -> {

                // Revisión Fase 6: esto antes mandaba directo a la
                // app apenas PlaybackService no estaba vivo — que es
                // el caso más común, porque Android lo mata en
                // cuanto no hay reproducción activa. playOrResume lo
                // arranca y retoma la última cola guardada en vez de
                // mandar a elegir de nuevo desde la app.
                PlayerCommands.playOrResume(applicationContext) {
                    updateTile()
                }
            }

            else -> {

                // Servicio vivo pero sin ninguna cola jamás guardada
                // (primer uso real, nunca se reprodujo nada): acá sí
                // no hay nada que reanudar.
                openApp()
            }
        }
    }

    private fun updateTile() {

        val tile = qsTile ?: return

        val player = PlaybackService.currentPlayer()

        val isPlaying =
            player?.isPlaying == true &&
                player.currentMediaItem != null

        tile.state =
            if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.label = getString(R.string.tile_label)

        tile.subtitle =
            if (isPlaying) {
                player?.currentMediaItem
                    ?.mediaMetadata
                    ?.title
                    ?.toString()
                    ?.ifBlank { null }
                    ?: getString(R.string.tile_subtitle_playing)
            } else {
                getString(R.string.tile_subtitle_idle)
            }

        tile.updateTile()
    }

    private fun openApp() {

        val intent =
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

            startActivityAndCollapse(pendingIntent)

        } else {

            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
