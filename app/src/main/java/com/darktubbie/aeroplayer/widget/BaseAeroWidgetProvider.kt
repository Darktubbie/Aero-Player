package com.darktubbie.aeroplayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Base compartida por los 3 receivers de widget (Fase 6, rediseño
 * nuclear): cada uno solo dice CUÁL [AeroWidgetStyle] es — todo el
 * trabajo real (leer el Player, armar el RemoteViews, ejecutar los
 * comandos) vive en [AeroWidget], una sola vez.
 */
abstract class BaseAeroWidgetProvider(
    private val style: AeroWidgetStyle
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        AeroWidget.refreshStyle(context, style)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {

        AeroWidget.refreshStyle(context, style)
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        super.onReceive(context, intent)

        val action = intent.action ?: return

        when (action) {

            AeroWidget.ACTION_PLAY_PAUSE,
            AeroWidget.ACTION_NEXT,
            AeroWidget.ACTION_PREVIOUS ->
                AeroWidget.handleAction(context, action)
        }
    }
}
