package com.darktubbie.aeroplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Caché de portadas de Aero Player.
 *
 * Arquitectura:
 *
 * RAM
 *  ↓
 * disco
 *  ↓
 * marcador .none
 *  ↓
 * MediaMetadataRetriever
 *
 * Las portadas se identifican por artista + álbum para que
 * varias canciones del mismo álbum compartan una sola imagen.
 */
object AlbumArtCache {

    private const val TARGET_SIZE_SMALL = 192

    /**
     * Tamaño más grande, solo para la portada del reproductor
     * completo (Now Playing), que se muestra a 260dp — 192px se
     * veía notoriamente borroso ahí, aunque para las miniaturas
     * pequeñas (lista, MiniPlayer) 192px sigue siendo de sobra.
     */
    private const val TARGET_SIZE_LARGE = 480

    private const val MEMORY_CACHE_SIZE = 40

    /**
     * Caché en RAM aparte para las portadas grandes: son más
     * pesadas que las miniaturas, así que se les da una capacidad
     * menor a propósito (no hace falta guardar 40 portadas grandes
     * en RAM — normalmente solo importa la de la canción actual).
     */
    private const val LARGE_MEMORY_CACHE_SIZE = 6

    private val memoryCache =
        LruCache<String, Bitmap>(
            MEMORY_CACHE_SIZE
        )

    private val largeMemoryCache =
        LruCache<String, Bitmap>(
            LARGE_MEMORY_CACHE_SIZE
        )

    /*
     * Cada álbum tiene su propio Mutex.
     *
     * Si cinco canciones del mismo álbum piden la portada
     * simultáneamente, solamente una la procesa.
     */
    private val keyLocks =
        ConcurrentHashMap<String, Mutex>()

    /*
     * Límite global de extracción de artwork.
     *
     * Evita que un scroll rápido abra demasiados archivos
     * mediante MediaMetadataRetriever al mismo tiempo.
     */
    private val extractionSemaphore =
        Semaphore(2)

    /**
     * Obtiene una portada.
     *
     * @param context Contexto de la aplicación.
     * @param path Ruta física del archivo de audio.
     * @param artist Artista de la canción.
     * @param album Álbum de la canción.
     * @param highRes si es true, decodifica/cachea una versión más
     * grande ([TARGET_SIZE_LARGE]), pensada para la portada grande
     * de Now Playing. Cada tamaño tiene su propia entrada en disco
     * y su propia caché en RAM — pedir la miniatura pequeña de una
     * canción no afecta ni reemplaza su versión grande, y viceversa.
     */
    suspend fun get(
        context: Context,
        path: String,
        artist: String,
        album: String,
        highRes: Boolean = false
    ): Bitmap? {

        if (path.isBlank()) {
            return null
        }

        val targetSize =
            if (highRes) TARGET_SIZE_LARGE else TARGET_SIZE_SMALL

        val activeMemoryCache =
            if (highRes) largeMemoryCache else memoryCache

        /*
         * createKey() calcula un SHA-256 (MessageDigest +
         * formateo hexadecimal), que es trabajo de CPU no
         * trivial. Se ejecuta en Dispatchers.Default para
         * no bloquear el hilo principal, ya que get() se
         * invoca desde un LaunchedEffect (Main-immediate)
         * en cada composición de AlbumArt, incluso cuando
         * el resultado ya está en la caché de RAM.
         */
        val baseKey =
            withContext(
                Dispatchers.Default
            ) {

                createKey(
                    path,
                    artist,
                    album
                )
            }

        val key =
            if (highRes) "$baseKey-large" else baseKey

        /*
         * Primero RAM.
         */
        activeMemoryCache.get(key)?.let {
            return it
        }

        val cacheDirectory =
            getCacheDirectory(context)

        val imageFile =
            File(
                cacheDirectory,
                "$key.jpg"
            )

        val noneFile =
            File(
                cacheDirectory,

                // El marcador "sin portada" es el mismo para ambos
                // tamaños: si el archivo no tiene artwork embebido,
                // eso no cambia según el tamaño pedido.
                "$baseKey.none"
            )

        /*
         * Después disco.
         */
        val diskBitmap =
            withContext(
                Dispatchers.IO
            ) {

                if (
                    imageFile.exists()
                ) {

                    decodeBitmap(
                        imageFile
                    )

                } else {

                    null
                }
            }

        if (diskBitmap != null) {

            activeMemoryCache.put(
                key,
                diskBitmap
            )

            return diskBitmap
        }

        /*
         * Si sabemos que este álbum no tiene portada,
         * no volvemos a abrir el archivo.
         */
        if (
            noneFile.exists()
        ) {
            return null
        }

        /*
         * Bloqueamos únicamente este álbum.
         *
         * Esto permite que distintos álbumes se procesen
         * independientemente, pero evita duplicar trabajo
         * para el mismo álbum.
         */
        val mutex =
            keyLocks.getOrPut(
                key
            ) {
                Mutex()
            }

        return mutex.withLock {

            /*
             * Otra corrutina pudo haber terminado el trabajo
             * mientras esperábamos el Mutex.
             */
            activeMemoryCache.get(key)?.let {
                return@withLock it
            }

            /*
             * Comprobamos nuevamente el disco.
             */
            val existingBitmap =
                withContext(
                    Dispatchers.IO
                ) {

                    if (
                        imageFile.exists()
                    ) {

                        decodeBitmap(
                            imageFile
                        )

                    } else {

                        null
                    }
                }

            if (
                existingBitmap != null
            ) {

                activeMemoryCache.put(
                    key,
                    existingBitmap
                )

                return@withLock existingBitmap
            }

            /*
             * Si otra corrutina ya dejó el marcador,
             * tampoco procesamos el archivo.
             */
            if (
                noneFile.exists()
            ) {
                return@withLock null
            }

            /*
             * La extracción pesada queda limitada por
             * el Semaphore global.
             */
            val bitmap =
                extractionSemaphore.withPermit {

                    withContext(
                        Dispatchers.IO
                    ) {

                        extractArtwork(
                            path,
                            targetSize
                        )
                    }
                }

            if (bitmap != null) {

                /*
                 * RAM.
                 */
                activeMemoryCache.put(
                    key,
                    bitmap
                )

                /*
                 * Disco.
                 */
                withContext(
                    Dispatchers.IO
                ) {

                    saveBitmap(
                        bitmap,
                        imageFile
                    )
                }

                bitmap

            } else {

                /*
                 * Recordamos que este álbum no tiene
                 * artwork para no volver a analizarlo.
                 */
                withContext(
                    Dispatchers.IO
                ) {

                    try {
                        noneFile.createNewFile()
                    } catch (_: Exception) {
                    }
                }

                null
            }
        }
    }

    /**
     * Extrae la portada embebida del archivo de audio.
     */
    private fun extractArtwork(
        path: String,
        targetSize: Int
    ): Bitmap? {

        val retriever =
            MediaMetadataRetriever()

        return try {

            retriever.setDataSource(
                path
            )

            val artwork =
                retriever.embeddedPicture
                    ?: return null

            /*
             * Primero obtenemos las dimensiones.
             * Todavía no construimos el Bitmap final.
             */
            val bounds =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

            BitmapFactory.decodeByteArray(
                artwork,
                0,
                artwork.size,
                bounds
            )

            if (
                bounds.outWidth <= 0 ||
                bounds.outHeight <= 0
            ) {
                return null
            }

            var sampleSize =
                1

            while (
                bounds.outWidth / sampleSize > targetSize ||
                bounds.outHeight / sampleSize > targetSize
            ) {

                sampleSize *= 2
            }

            val options =
                BitmapFactory.Options().apply {

                    inSampleSize =
                        sampleSize

                    /*
                     * La portada de la lista no necesita
                     * precisión de 32 bits.
                     */
                    inPreferredConfig =
                        Bitmap.Config.RGB_565
                }

            BitmapFactory.decodeByteArray(
                artwork,
                0,
                artwork.size,
                options
            )

        } catch (_: Exception) {

            null

        } finally {

            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Decodifica una portada ya almacenada en disco.
     */
    private fun decodeBitmap(
        file: File
    ): Bitmap? {

        return try {

            BitmapFactory.decodeFile(
                file.absolutePath
            )

        } catch (_: Exception) {

            null
        }
    }

    /**
     * Guarda una versión comprimida de la portada.
     */
    private fun saveBitmap(
        bitmap: Bitmap,
        file: File
    ) {

        try {

            file.parentFile?.mkdirs()

            FileOutputStream(
                file
            ).use { output ->

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    output
                )
            }

        } catch (_: Exception) {
        }
    }

    /**
     * Directorio privado de artwork.
     *
     * No usamos almacenamiento externo.
     */
    private fun getCacheDirectory(
        context: Context
    ): File {

        val directory =
            File(
                context.filesDir,
                "album_art"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory
    }

    /**
     * Crea una clave estable para el álbum.
     *
     * Normalmente:
     *
     * artist + album
     *
     * Si los metadatos no son útiles, usamos la ruta
     * para evitar que canciones desconocidas compartan
     * accidentalmente una portada.
     */
    private fun createKey(
        path: String,
        artist: String,
        album: String
    ): String {

        val cleanArtist =
            artist
                .trim()
                .lowercase()

        val cleanAlbum =
            album
                .trim()
                .lowercase()

        val invalidArtist =
            cleanArtist.isBlank() ||
            cleanArtist == "unknown artist" ||
            cleanArtist == "<unknown>"

        val invalidAlbum =
            cleanAlbum.isBlank() ||
            cleanAlbum == "unknown album" ||
            cleanAlbum == "<unknown>"

        val rawKey =
            if (
                !invalidArtist &&
                !invalidAlbum
            ) {

                "$cleanArtist|$cleanAlbum"

            } else {

                "track|$path"
            }

        return sha256(
            rawKey
        )
    }

    /**
     * SHA-256 para convertir la clave del álbum
     * en un nombre de archivo seguro.
     */
    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val bytes =
            digest.digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )

        return buildString {

            for (
                byte in bytes
            ) {

                append(
                    "%02x".format(
                        byte
                    )
                )
            }
        }
    }
}