package com.darktubbie.aeroplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.darktubbie.aeroplayer.data.AudioTrack
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Envuelve un [MediaController] conectado a [PlaybackService] y
 * expone el estado de reproducción como Compose [State], para que
 * MainViewModel no tenga que hablar directamente con la API de
 * Media3.
 *
 * La cola de reproducción es "toda la lista visible, empezando en
 * la canción pulsada": Media3 ya trae su propio concepto de
 * playlist (setMediaItems + seekToNext/seekToPrevious +
 * shuffleModeEnabled + repeatMode), así que no hace falta un
 * QueueManager propio — reconstruirlo por fuera duplicaría lo que
 * Media3 ya resuelve bien. [playQueue] evita reconstruir la lista
 * de MediaItem cuando la cola pulsada ya es la que está cargada
 * (Fase 8 del plan).
 */
class PlayerRepository(
    private val context: Context
) {

    private var controller: MediaController? = null

    private var controllerFuture:
        ListenableFuture<MediaController>? = null

    private val _currentTrackUri =
        mutableStateOf<String?>(null)

    val currentTrackUri: State<String?>
        get() = _currentTrackUri

    private val _isPlaying =
        mutableStateOf(false)

    val isPlaying: State<Boolean>
        get() = _isPlaying

    private val _positionMs =
        mutableStateOf(0L)

    val positionMs: State<Long>
        get() = _positionMs

    private val _durationMs =
        mutableStateOf(0L)

    val durationMs: State<Long>
        get() = _durationMs

    private val _shuffleEnabled =
        mutableStateOf(false)

    val shuffleEnabled: State<Boolean>
        get() = _shuffleEnabled

    private val _repeatMode =
        mutableStateOf(Player.REPEAT_MODE_OFF)

    val repeatMode: State<Int>
        get() = _repeatMode

    private val playerListener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {

                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {

                _currentTrackUri.value =
                    mediaItem?.mediaId

                // La duración de la nueva pista aún
                // puede no estar lista aquí; se
                // actualiza también en cada
                // refreshPosition() mientras se ve
                // Now Playing.
                refreshPosition()
            }
        }

    /**
     * Conecta con [PlaybackService]. Es seguro llamarla varias
     * veces: si ya hay un controller activo o una conexión en
     * curso, no hace nada.
     */
    fun connect() {

        if (
            controller != null ||
            controllerFuture != null
        ) {
            return
        }

        val sessionToken =
            SessionToken(
                context,
                ComponentName(
                    context,
                    PlaybackService::class.java
                )
            )

        val future =
            MediaController.Builder(
                context,
                sessionToken
            ).buildAsync()

        controllerFuture = future

        future.addListener(
            {

                /*
                 * Si release() ya se llamó mientras este future
                 * seguía pendiente (por ejemplo, el ViewModel se
                 * destruyó justo después de pedir la conexión),
                 * controllerFuture ya se puso a null desde
                 * release() y MediaController.releaseFuture ya se
                 * encargó de cancelar/liberar esto. No hay que
                 * completar la conexión a estas alturas.
                 */
                if (controllerFuture !== future) {
                    return@addListener
                }

                controller =
                    future.get()

                controller?.addListener(
                    playerListener
                )

                /*
                 * Si el servicio ya tenía una reproducción en
                 * curso (por ejemplo, la Activity se recreó pero
                 * PlaybackService siguió vivo), el controller
                 * recién conectado ya refleja ese estado real.
                 * Sin esto, currentTrackUri/isPlaying/shuffle/
                 * repeat se quedarían en sus valores iniciales
                 * hasta el próximo evento del Player, mostrando
                 * un estado incorrecto mientras tanto.
                 */
                syncStateFromController()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun syncStateFromController() {

        val controller =
            controller
                ?: return

        _currentTrackUri.value =
            controller.currentMediaItem
                ?.mediaId

        _isPlaying.value =
            controller.isPlaying

        _shuffleEnabled.value =
            controller.shuffleModeEnabled

        _repeatMode.value =
            controller.repeatMode

        refreshPosition()
    }

    private var queuedTrackUris: List<String>? = null

    /**
     * Reproduce [tracks] como cola, empezando en [startIndex].
     * Si [startIndex] ya es la canción actual, simplemente
     * continúa/alterna play-pause en vez de reiniciarla.
     *
     * Si [tracks] es exactamente la misma cola que ya está
     * cargada en el controller (mismo orden, mismas pistas — el
     * caso normal de tocar otra canción de la lista que ya se
     * estaba reproduciendo), no se reconstruye la lista de
     * MediaItem: eso evitaría trabajo de CPU innecesario en el
     * hilo principal con bibliotecas grandes, y además resetearía
     * el orden interno de shuffle de Media3 sin necesidad.
     */
    fun playQueue(
        tracks: List<AudioTrack>,
        startIndex: Int
    ) {

        val controller =
            controller
                ?: return

        if (startIndex !in tracks.indices) {
            return
        }

        val tappedTrack =
            tracks[startIndex]

        if (
            tappedTrack.uri ==
            _currentTrackUri.value
        ) {

            togglePlayPause()

            return
        }

        val trackUris =
            tracks.map {
                it.uri
            }

        if (trackUris == queuedTrackUris) {

            controller.seekTo(
                startIndex,
                0L
            )

            controller.play()

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
                            .build()
                    )
                    .build()
            }

        controller.setMediaItems(
            mediaItems,
            startIndex,
            0L
        )

        controller.prepare()

        controller.play()

        queuedTrackUris = trackUris
    }

    fun togglePlayPause() {

        val controller =
            controller
                ?: return

        if (controller.isPlaying) {

            controller.pause()

        } else {

            controller.play()
        }
    }

    fun skipNext() {
        controller?.seekToNext()
    }

    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    fun seekTo(
        positionMs: Long
    ) {

        controller?.seekTo(positionMs)

        _positionMs.value = positionMs
    }

    /**
     * Vuelve a leer posición y duración del controller.
     *
     * Media3 no empuja actualizaciones continuas de posición
     * mientras suena una pista (solo notifica eventos discretos
     * como cambio de pista o de estado), así que quien quiera una
     * barra de progreso en vivo (Now Playing) debe llamar a esto
     * periódicamente mientras esa pantalla esté visible — no lo
     * hacemos aquí dentro para no gastar batería sondeando cuando
     * nadie está mirando el progreso.
     */
    fun refreshPosition() {

        val controller =
            controller
                ?: return

        _positionMs.value =
            controller.currentPosition
                .coerceAtLeast(0L)

        _durationMs.value =
            controller.duration
                .coerceAtLeast(0L)
    }

    /**
     * Lee la posición actual directamente del controller, sin
     * pasar por [_positionMs] ni depender de que alguna pantalla
     * esté llamando a [refreshPosition] periódicamente.
     *
     * Pensada para el sistema de Aero Player Effects (Fase 7): un
     * `.ape` necesita sondear la posición para saber cuándo disparar
     * un efecto, incluso si el usuario no está viendo la pantalla
     * de Now Playing en ese momento. Es una lectura puntual, no un
     * observable — quien la use decide su propia cadencia de
     * sondeo.
     */
    fun currentPositionMs(): Long =
        controller
            ?.currentPosition
            ?.coerceAtLeast(0L)
            ?: 0L

    fun toggleShuffle() {

        val controller =
            controller
                ?: return

        val newValue =
            !controller.shuffleModeEnabled

        controller.shuffleModeEnabled =
            newValue

        _shuffleEnabled.value = newValue
    }

    /**
     * Alterna entre apagado -> repetir todo -> repetir una,
     * volviendo a apagado.
     */
    fun cycleRepeatMode() {

        val controller =
            controller
                ?: return

        val nextMode =
            when (controller.repeatMode) {

                Player.REPEAT_MODE_OFF ->
                    Player.REPEAT_MODE_ALL

                Player.REPEAT_MODE_ALL ->
                    Player.REPEAT_MODE_ONE

                else ->
                    Player.REPEAT_MODE_OFF
            }

        controller.repeatMode = nextMode

        _repeatMode.value = nextMode
    }

    /**
     * Libera la conexión con PlaybackService. No libera el
     * ExoPlayer/MediaSession en sí (eso es responsabilidad de
     * PlaybackService.onDestroy): esto solo cierra la conexión
     * del cliente.
     *
     * Se usa MediaController.releaseFuture en vez de
     * controller?.release() porque cubre correctamente el caso
     * en que release() se llama mientras connect() todavía está
     * conectando: cancela el future pendiente si aún no resolvió,
     * o libera el controller si ya se conectó. Sin esto, una
     * conexión que termina de completarse después de release()
     * quedaría viva para siempre (ver el comentario en connect()).
     */
    fun release() {

        controller?.removeListener(
            playerListener
        )

        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }

        controller = null

        controllerFuture = null
    }
}
