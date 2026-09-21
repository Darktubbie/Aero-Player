package com.darktubbie.aeroplayer.data

/**
 * Una playlist local (Fase 3, 0.4.x).
 *
 * [trackKeys] son fingerprints (ver [trackFingerprint]), no rutas
 * de archivo ni URIs de MediaStore — el orden de la lista es el
 * orden de la playlist, así que reordenar es simplemente reordenar
 * esta lista.
 */
data class Playlist(
    val id: String,
    val name: String,
    val trackKeys: List<String> = emptyList()
)

/**
 * Identificador robusto de una canción para playlists (Fase 3,
 * 0.4.x).
 *
 * A diferencia de Favoritos (Fase 2), que identifica por
 * [AudioTrack.path] porque ahí alcanzaba con algo estable entre
 * reescaneos, el brief pide explícitamente que las playlists NO
 * dependan de una ruta absoluta que se rompa fácilmente (mover el
 * archivo a otra subcarpeta ya escaneada, reorganizar carpetas,
 * etc. cambiaría el path pero no la canción).
 *
 * En vez de eso se arma un fingerprint con la metadata de la propia
 * canción (título + artista + álbum + duración): sobrevive a que el
 * archivo se mueva de carpeta, y solo se "rompe" si el usuario edita
 * los tags de la canción — una situación mucho más rara y, para el
 * alcance de este proyecto sin base de datos ni IDs persistentes
 * propios, un costo aceptable a cambio de no depender del path.
 *
 * Se usa `\u0000` como separador porque no puede aparecer en texto
 * normal de metadata, a diferencia de separadores más obvios como
 * "-" o "|".
 */
fun trackFingerprint(
    track: AudioTrack
): String {

    return listOf(
        track.title,
        track.artist,
        track.album,
        track.duration.toString()
    ).joinToString("\u0000")
}
