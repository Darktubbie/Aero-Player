package com.darktubbie.aeroplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.media3.common.C
import androidx.media3.common.Player
import com.darktubbie.aeroplayer.AlbumArtCache
import com.darktubbie.aeroplayer.MainActivity
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.data.SettingsRepository
import com.darktubbie.aeroplayer.playback.EXTRA_TRACK_PATH
import com.darktubbie.aeroplayer.playback.PlaybackService
import com.darktubbie.aeroplayer.playback.PlayerCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Los 3 widgets de Aero Player (Fase 6, rediseño nuclear —
 * referencias `widget_01/02/03.jpeg`, tomadas de forma DIRECTA, no
 * como inspiración vaga). Cada [AeroWidgetStyle] es su propia
 * composición visual con su propio [layout] y su propio
 * [providerClass] (así el usuario los ve como widgets distintos en
 * el selector de Android) — no una plantilla reescalada.
 *
 * Solo se exponen los controles que cada referencia realmente tiene:
 * ninguno de los 3 muestra shuffle/repeat, así que ninguno de los 3
 * widgets los tiene — eso NO saca shuffle/repeat de la app, solo de
 * lo que estos widgets exponen (ver Mini Player / Now Playing / el
 * Quick Settings Tile para eso).
 */
enum class AeroWidgetStyle(
    val providerClass: Class<*>,
    val layout: Int
) {

    CLASSIC(
        AeroWidgetClassicProvider::class.java,
        R.layout.aero_widget_classic
    ),

    ORB(
        AeroWidgetOrbProvider::class.java,
        R.layout.aero_widget_orb
    ),

    HANDHELD(
        AeroWidgetHandheldProvider::class.java,
        R.layout.aero_widget_handheld
    )
}

/**
 * Igual que en la Fase 5: cada widget lee y controla el mismo
 * [Player] real que vive dentro de [PlaybackService] — no hay ningún
 * reproductor ni cola propios de los widgets.
 */
object AeroWidget {

    const val ACTION_PLAY_PAUSE =
        "com.darktubbie.aeroplayer.widget.PLAY_PAUSE"

    const val ACTION_NEXT =
        "com.darktubbie.aeroplayer.widget.NEXT"

    const val ACTION_PREVIOUS =
        "com.darktubbie.aeroplayer.widget.PREVIOUS"

    private const val PROGRESS_MAX = 1000

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var artJob: Job? = null

    private var artKey: String? = null

    private var artBitmap: Bitmap? = null

    private class Snapshot(
        val title: String,
        val artist: String,
        val album: String,
        val path: String?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long
    )

    // ---------------------------------------------------------
    // API pública
    // ---------------------------------------------------------

    /** Repinta los 3 estilos con el estado actual. */
    fun refresh(context: Context) {

        AeroWidgetStyle.entries.forEach { style ->
            refreshStyle(context, style)
        }
    }

    fun refreshStyle(context: Context, style: AeroWidgetStyle) {

        val appContext = context.applicationContext

        val ids = widgetIds(appContext, style)

        if (ids.isEmpty()) {
            return
        }

        val snapshot = readSnapshot()

        requestArtIfNeeded(appContext, snapshot)

        val views =
            buildViews(
                appContext,
                style,
                snapshot,
                artBitmap.takeIf { artKey == snapshot?.path }
            )

        AppWidgetManager
            .getInstance(appContext)
            .updateAppWidget(ids, views)
    }

    /**
     * Actualización liviana de progreso para el avance en vivo
     * (Classic y Handheld tienen barra de progreso; Orb no — su
     * pantalla es demasiado chica para una, ver widget_02.jpeg).
     */
    fun tick(context: Context) {

        val appContext = context.applicationContext

        val snapshot = readSnapshot() ?: return

        listOf(
            AeroWidgetStyle.CLASSIC to R.layout.aero_widget_classic,
            AeroWidgetStyle.HANDHELD to R.layout.aero_widget_handheld
        ).forEach { (style, layout) ->

            val ids = widgetIds(appContext, style)

            if (ids.isEmpty()) {
                return@forEach
            }

            val views = RemoteViews(appContext.packageName, layout)

            applyProgress(views, snapshot)

            if (style == AeroWidgetStyle.CLASSIC) {

                views.setTextViewText(
                    R.id.widget_position,
                    formatTime(snapshot.positionMs)
                )
            }

            AppWidgetManager
                .getInstance(appContext)
                .partiallyUpdateAppWidget(ids, views)
        }

        if (widgetIds(appContext, AeroWidgetStyle.ORB).isNotEmpty()) {

            refreshStyle(appContext, AeroWidgetStyle.ORB)
        }
    }

    fun handleAction(context: Context, action: String) {

        val existingPlayer = PlaybackService.currentPlayer()

        if (existingPlayer != null && existingPlayer.currentMediaItem != null) {

            when (action) {

                ACTION_PLAY_PAUSE ->
                    PlayerCommands.togglePlayPause(existingPlayer)

                ACTION_NEXT ->
                    existingPlayer.seekToNext()

                ACTION_PREVIOUS ->
                    existingPlayer.seekToPrevious()
            }

            refresh(context)

            return
        }

        // El servicio puede estar simplemente detenido (Android lo
        // mata sin reproducción activa) con una última cola
        // guardada — solo Play arranca el servicio y la retoma.
        if (action == ACTION_PLAY_PAUSE) {

            PlayerCommands.playOrResume(context.applicationContext) {
                refresh(context)
            }

            return
        }

        refresh(context)
    }

    // ---------------------------------------------------------
    // Lectura del estado real
    // ---------------------------------------------------------

    private fun readSnapshot(): Snapshot? {

        val player: Player =
            PlaybackService.currentPlayer()
                ?: return null

        val item =
            player.currentMediaItem
                ?: return null

        val metadata = item.mediaMetadata

        return Snapshot(
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            path = metadata.extras?.getString(EXTRA_TRACK_PATH),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs =
                player.duration
                    .takeIf { it != C.TIME_UNSET }
                    ?.coerceAtLeast(0L)
                    ?: 0L
        )
    }

    private fun widgetIds(
        context: Context,
        style: AeroWidgetStyle
    ): IntArray =
        AppWidgetManager
            .getInstance(context)
            .getAppWidgetIds(
                ComponentName(context, style.providerClass)
            )

    // ---------------------------------------------------------
    // Carátula (solo la usa Handheld — Classic/Orb no muestran
    // carátula, igual que sus referencias)
    // ---------------------------------------------------------

    private fun requestArtIfNeeded(
        appContext: Context,
        snapshot: Snapshot?
    ) {

        val current = snapshot ?: return

        val path = current.path ?: return

        if (path == artKey) {
            return
        }

        artJob?.cancel()

        artJob =
            scope.launch {

                val loaded =
                    AlbumArtCache.get(
                        appContext,
                        path,
                        current.artist,
                        current.album
                    )

                artBitmap = loaded?.let { roundedSquare(it, 20f) }
                artKey = path

                refresh(appContext)
            }
    }

    private fun roundedSquare(source: Bitmap, radiusPx: Float): Bitmap {

        val size = 220

        val output =
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val side = minOf(source.width, source.height)

        val cropped =
            Bitmap.createBitmap(
                source,
                (source.width - side) / 2,
                (source.height - side) / 2,
                side,
                side
            )

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                shader =
                    BitmapShader(
                        Bitmap.createScaledBitmap(cropped, size, size, true),
                        Shader.TileMode.CLAMP,
                        Shader.TileMode.CLAMP
                    )
            }

        Canvas(output).drawRoundRect(
            RectF(0f, 0f, size.toFloat(), size.toFloat()),
            radiusPx,
            radiusPx,
            paint
        )

        return output
    }

    // ---------------------------------------------------------
    // RemoteViews
    // ---------------------------------------------------------

    private fun buildViews(
        appContext: Context,
        style: AeroWidgetStyle,
        snapshot: Snapshot?,
        art: Bitmap?
    ): RemoteViews {

        val dark =
            SettingsRepository(appContext)
                .loadThemeName() == "DARK_AERO"

        return when (style) {
            AeroWidgetStyle.CLASSIC ->
                buildClassicViews(appContext, snapshot, dark)

            AeroWidgetStyle.ORB ->
                buildOrbViews(appContext, snapshot, dark)

            AeroWidgetStyle.HANDHELD ->
                buildHandheldViews(appContext, snapshot, art, dark)
        }
    }

    private fun buildClassicViews(
        appContext: Context,
        snapshot: Snapshot?,
        dark: Boolean
    ): RemoteViews {

        val views =
            RemoteViews(appContext.packageName, R.layout.aero_widget_classic)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (dark) R.drawable.aero_widget_classic_body_dark
            else R.drawable.aero_widget_classic_body
        )

        views.setTextViewText(
            R.id.widget_state_label,
            if (snapshot?.isPlaying == true) "▶ PLAYING" else "❙❙ PAUSED"
        )

        views.setTextViewText(
            R.id.widget_position,
            formatTime(snapshot?.positionMs ?: 0L)
        )

        views.setTextViewText(
            R.id.widget_title,
            snapshot?.let {
                listOf(it.artist, it.title)
                    .filter { part -> part.isNotBlank() }
                    .joinToString(" - ")
                    .ifBlank { null }
            } ?: appContext.getString(R.string.widget_idle_title)
        )

        applyProgress(
            views,
            snapshot ?: Snapshot("", "", "", null, false, 0L, 0L)
        )

        views.setImageViewResource(
            R.id.widget_play,
            if (snapshot?.isPlaying == true) R.drawable.ic_widget_pause
            else R.drawable.ic_widget_play
        )

        bindTransport(appContext, views, snapshot, AeroWidgetStyle.CLASSIC)

        return views
    }

    private fun buildOrbViews(
        appContext: Context,
        snapshot: Snapshot?,
        dark: Boolean
    ): RemoteViews {

        val views =
            RemoteViews(appContext.packageName, R.layout.aero_widget_orb)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (dark) R.drawable.aero_widget_orb_chassis_dark
            else R.drawable.aero_widget_orb_chassis
        )

        views.setTextViewText(
            R.id.widget_title,
            snapshot?.title?.ifBlank { null }
                ?: appContext.getString(R.string.widget_idle_title)
        )

        views.setTextViewText(
            R.id.widget_position,
            "TIME " + formatTime(snapshot?.positionMs ?: 0L)
        )

        views.setImageViewResource(
            R.id.widget_play,
            if (snapshot?.isPlaying == true) R.drawable.ic_widget_pause
            else R.drawable.ic_widget_play
        )

        bindTransport(appContext, views, snapshot, AeroWidgetStyle.ORB)

        return views
    }

    private fun buildHandheldViews(
        appContext: Context,
        snapshot: Snapshot?,
        art: Bitmap?,
        dark: Boolean
    ): RemoteViews {

        val views =
            RemoteViews(appContext.packageName, R.layout.aero_widget_handheld)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (dark) R.drawable.aero_widget_handheld_scene_dark
            else R.drawable.aero_widget_handheld_scene
        )

        views.setTextViewText(
            R.id.widget_title,
            snapshot?.title?.ifBlank { null }
                ?: appContext.getString(R.string.widget_idle_title)
        )

        views.setTextViewText(
            R.id.widget_subtitle,
            snapshot?.artist?.ifBlank { null }
                ?: appContext.getString(R.string.widget_idle_hint)
        )

        if (art != null) {
            views.setImageViewBitmap(R.id.widget_art, art)
        } else {
            views.setImageViewResource(
                R.id.widget_art,
                R.drawable.aero_widget_art_placeholder
            )
        }

        applyProgress(
            views,
            snapshot ?: Snapshot("", "", "", null, false, 0L, 0L)
        )

        views.setImageViewResource(
            R.id.widget_play,
            if (snapshot?.isPlaying == true) R.drawable.ic_widget_pause
            else R.drawable.ic_widget_play
        )

        val openApp = openAppIntent(appContext)
        views.setOnClickPendingIntent(R.id.widget_art, openApp)

        bindTransport(appContext, views, snapshot, AeroWidgetStyle.HANDHELD)

        return views
    }

    /** Play/Anterior/Siguiente — los únicos controles que exponen
     *  las 3 referencias. En reposo, Play arranca (playOrResume) y
     *  anterior/siguiente abren la app (no tienen sentido sin cola
     *  activa). */
    private fun bindTransport(
        appContext: Context,
        views: RemoteViews,
        snapshot: Snapshot?,
        style: AeroWidgetStyle
    ) {

        val openApp = openAppIntent(appContext)

        val baseCode = style.ordinal * 10

        bindAction(
            appContext,
            views,
            R.id.widget_play,
            ACTION_PLAY_PAUSE,
            baseCode + 3,
            style
        )

        if (snapshot != null) {

            bindAction(
                appContext,
                views,
                R.id.widget_previous,
                ACTION_PREVIOUS,
                baseCode + 2,
                style
            )
            bindAction(
                appContext,
                views,
                R.id.widget_next,
                ACTION_NEXT,
                baseCode + 4,
                style
            )

        } else {

            views.setOnClickPendingIntent(R.id.widget_previous, openApp)
            views.setOnClickPendingIntent(R.id.widget_next, openApp)
        }
    }

    private fun applyProgress(views: RemoteViews, snapshot: Snapshot) {

        val progress =
            if (snapshot.durationMs > 0L) {
                ((snapshot.positionMs * PROGRESS_MAX) / snapshot.durationMs)
                    .toInt()
                    .coerceIn(0, PROGRESS_MAX)
            } else {
                0
            }

        views.setProgressBar(R.id.widget_progress, PROGRESS_MAX, progress, false)
    }

    private fun bindAction(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        action: String,
        requestCode: Int,
        style: AeroWidgetStyle
    ) {

        // Los 3 receivers hacen exactamente lo mismo con la acción
        // (ver BaseAeroWidgetProvider.onReceive), así que cualquiera
        // sirve como destino explícito — un intent explícito no
        // necesita que el receiver declare un <intent-filter> para
        // esta acción.
        val intent =
            Intent(context, style.providerClass)
                .setAction(action)

        views.setOnClickPendingIntent(
            viewId,
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                ),
            PendingIntent.FLAG_IMMUTABLE or
                PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun formatTime(ms: Long): String {

        val totalSeconds = (ms / 1000).coerceAtLeast(0L)

        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
