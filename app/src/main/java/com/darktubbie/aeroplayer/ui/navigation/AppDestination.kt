package com.darktubbie.aeroplayer.ui.navigation

/**
 * Los 5 destinos principales de la aplicación (Fase 1 del plan de
 * evolución visual).
 *
 * No se introduce Navigation Compose para esto: igual que ya se
 * decidió para las 2 pantallas anteriores (Library/NowPlaying),
 * con 5 destinos fijos y sin deep links ni back stack complejo,
 * un enum + un `when` en MainActivity sigue siendo más simple y no
 * añade una dependencia nueva.
 *
 * MUSICA y ALBUMES reutilizan la misma LibraryScreen ya existente
 * (que ya tiene sus propias pestañas Songs/Albums/Artists): cada
 * uno simplemente le indica a MainViewModel con qué LibraryTab
 * debe abrirse. Esto evita duplicar la pantalla de biblioteca o
 * reescribirla en esta fase.
 */
enum class AppDestination {
    INICIO,
    MUSICA,
    ALBUMES,
    REPRODUCTOR,
    MAS
}
