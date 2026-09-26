package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.runtime.compositionLocalOf

/**
 * Contexto de reproducción que necesita el sistema de efectos
 * ambientales (Fases 5, 6 y 7): si hay música sonando, qué pista es
 * (para resolver su `.ape`) y cómo leer la posición actual.
 *
 * Se expone como CompositionLocal en vez de como parámetro de cada
 * pantalla porque [AeroBackground] (y con él, [MidgroundLayer] y
 * [ForegroundLayer]) se usa en Inicio, Música, Álbumes, Más y
 * Reproductor — pasar `isPlaying`/la pista actual a cada una de esas
 * pantallas solo para reenviarlo a las capas de efectos habría sido
 * un cambio de firma mucho más invasivo que esto. Se provee una sola
 * vez en `MainActivity`.
 *
 * [getPositionMs] es una función síncrona (no un `State` de
 * Compose) a propósito: el sistema de efectos la sondea
 * periódicamente desde una corrutina propia (ver
 * [ForegroundLayer]) en vez de observarla como estado de Compose,
 * para no forzar una recomposición de toda la app en cada tick de
 * posición.
 *
 * [apeVersion] existe solo para forzar una relectura del `.ape`
 * actual: el editor (Fase 8) puede crear o modificar el `.ape` de
 * la canción que sigue sonando, y sin esta señal
 * [ForegroundLayer] no tendría forma de enterarse hasta que
 * cambiara de canción (su resolución del `.ape` está atada a
 * [trackPath], que no cambia solo porque el archivo en disco haya
 * cambiado). `MainActivity` lo incrementa cada vez que el editor
 * guarda con éxito.
 *
 * [trackArtist]/[trackAlbum] (Fase 5 de la Experiencia de Artwork,
 * 0.5.0): agregados para que [MidgroundLayer] pueda resolver la
 * portada de la canción actual vía [com.darktubbie.aeroplayer.AlbumArtCache]
 * (que identifica portadas por artista+álbum, no por [trackPath]
 * solo) y tintar sutilmente el fondo ambiental con su color
 * dominante. Cadena vacía por defecto, igual de inofensivo que
 * [trackPath] en `null`.
 */
data class AmbientPlaybackInfo(
    val isPlaying: Boolean,
    val trackPath: String?,
    val getPositionMs: () -> Long,
    val apeVersion: Int = 0,
    val trackArtist: String = "",
    val trackAlbum: String = ""
)

val LocalAmbientPlayback =
    compositionLocalOf {

        AmbientPlaybackInfo(
            isPlaying = false,
            trackPath = null,
            getPositionMs = { 0L }
        )
    }
