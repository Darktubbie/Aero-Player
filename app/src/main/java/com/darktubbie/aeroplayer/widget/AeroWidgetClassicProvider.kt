package com.darktubbie.aeroplayer.widget

/**
 * Receiver del Widget 01 "Classic" (referencia widget_01.jpeg). Cada
 * estilo tiene su propio `AppWidgetProvider` (así el usuario los ve
 * como widgets distintos en el selector), pero todos delegan en
 * [AeroWidget] — no hay tres reproductores, hay tres vidrieras del
 * mismo.
 */
class AeroWidgetClassicProvider :
    BaseAeroWidgetProvider(AeroWidgetStyle.CLASSIC)
