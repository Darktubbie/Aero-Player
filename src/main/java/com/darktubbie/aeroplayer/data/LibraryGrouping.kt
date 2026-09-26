package com.darktubbie.aeroplayer.data

/**
 * Agrupa una biblioteca ya escaneada por álbum/artista.
 *
 * Son funciones puras a propósito (sin estado, sin I/O): no
 * viven en LibraryRepository porque ese repositorio es sobre
 * persistencia y MediaStore, no sobre transformar datos que ya
 * están en memoria. Separarlas aquí las hace triviales de
 * razonar y de testear de forma aislada.
 */
object LibraryGrouping {

    fun groupByAlbum(
        tracks: List<AudioTrack>
    ): List<Album> {

        return tracks
            .groupBy { track ->
                track.album to track.artist
            }
            .map { (key, albumTracks) ->

                Album(
                    name = key.first,
                    artist = key.second,
                    tracks = albumTracks
                )
            }
            .sortedBy {
                it.name.lowercase()
            }
    }

    fun groupByArtist(
        tracks: List<AudioTrack>
    ): List<Artist> {

        return tracks
            .groupBy {
                it.artist
            }
            .map { (artistName, artistTracks) ->

                val albumCount =
                    artistTracks
                        .map {
                            it.album
                        }
                        .distinct()
                        .size

                Artist(
                    name = artistName,
                    tracks = artistTracks,
                    albumCount = albumCount
                )
            }
            .sortedBy {
                it.name.lowercase()
            }
    }
}
