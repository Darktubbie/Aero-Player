# Aero Player

PWA de música local con estética Frutiger Aero + chrome clásico de Windows XP.

## Personalizar fondo y música de escritorio

Ya no hace falta tocar el código: en el escritorio hay un ícono
**"Personalizar"** que abre una ventana con dos botones — *Elegir imagen*
(fondo) y *Elegir audio* (música ambiental) — más un botón para restablecer
los valores originales. Lo que elijas se guarda en el dispositivo (IndexedDB)
y se aplica de inmediato, incluso después de cerrar y volver a abrir la app.

Este proyecto ya incluye una pista de música ambiental
(`assets/desktop-ambient.mp3`) y un fondo (`assets/desktop-wallpaper.jpg`) por
defecto, que suenan/se muestran hasta que elijas los tuyos propios desde
"Personalizar". Si prefieres cambiar los archivos por defecto directamente
(en vez de usar el selector dentro de la app), también puedes reemplazar esos
dos archivos en `assets/` antes de desplegar el proyecto.

## Instalación

Sube la carpeta a cualquier hosting con HTTPS (GitHub Pages, Netlify, Vercel)
y ábrela desde Chrome en Android → menú (⋮) → "Instalar app".
