🌊 Aero Player

«A lightweight, local-first music player for Android with a Frutiger Aero soul.»

Aero Player is an open-source Android music player focused on making local music feel fresh, simple, and fun again.

Built from the ground up as a native Android application, Aero Player combines a modern audio engine with a visual style inspired by the Frutiger Aero era, classic Windows Aero interfaces, and the colorful digital aesthetics of the late 2000s and early 2010s.

«🚧 Aero Player is under active development.
The current version is 0.6.0 — "The Future We Remember Update". The first stable release (1.0) is not out yet.»

---

🌊 Vision

Aero Player is designed around a simple idea:

Your music should belong to you.

The core experience is local-first. Your music library, playlists, settings, and playback experience work directly on your device without requiring an account or an always-online connection.

Online features may be added later as optional extensions rather than becoming a requirement for the basic player.

---

✨ Features

🎵 Music Library

- Music from your device's library and from folders you choose (MediaStore + Storage Access Framework)
- Song, album, and artist views
- Search and extended sorting
- Album artwork and metadata, with its own artwork cache
- Album Showcase: a dedicated album view with a large sleeve, a translucent crystal vinyl with liquid inside that you can drag around, album info and tracklist, and Play / Shuffle actions

▶️ Playback

- Background playback built on Android Media3
- Notification and lock-screen controls
- Play / pause / previous / next and seek
- Shuffle and repeat
- Playback queue: view it from Now Playing, reorder it by dragging, and remove songs
- Sleep timer
- Automatic pause when Bluetooth audio disconnects
- Resume your last queue after the app has been closed
- Quick Settings tile for playback

📚 Personal Library

- Playlists, with song reordering
- Favorites
- Playback history
- Aero Picks: local recommendations based on what you play (no AI, no account)
- Aero Home with Continue Listening, Recently Played, Favorites, and Playlists

🎨 Aero Interface

- Frutiger Aero-inspired visual identity with glass and translucent surfaces
- Light Aero and Aero Dark themes
- Spanish and English
- Animated ambient backgrounds (bubbles, fish, jellyfish, clouds, leaves) with adjustable intensity, which respect the system's "remove animations" setting
- Album art as environment: the colors of the current cover tint the lighting and glows around the interface
- Aero touch effects: water-drop ripples, sparkles, and small bubbles when you tap, plus a soft press-and-bounce on playback controls
- Custom `.aero` effects, with a basic editor
- Aero Desktop mode for landscape and portrait layouts on larger screens
- Aero Rest Mode: after a while without touching the screen, a dim screen with a quiet logo
- Three home-screen widgets: Classic, Orb, and Handheld

🌐 Optional Online Features

Aero Player may eventually support optional online music discovery through independent providers.

The local player will remain usable without these services.

---

🚧 Still Planned

- Playlist import / export
- Local backup and restore

---

🛠️ Technology

Aero Player is a native Android application using:

Technology| Purpose
Kotlin| Application language
Jetpack Compose| User interface
Android Media3| Audio playback and media session
MediaStore + Storage Access Framework| Music library and user-selected folders

Aero Player does not use Room: the library is built from MediaStore and cached on the device.

The project is intentionally built as a native application rather than as a WebView wrapper.

---

🏗️ Version History

0.1 — Foundation

- [x] Android project structure
- [x] Initial Frutiger Aero interface
- [x] Navigation

0.2 — Local Library

- [x] Folder selection
- [x] Music scanning
- [x] Metadata extraction
- [x] Library browsing

0.3 — Player

- [x] Media3 integration
- [x] Background playback
- [x] Notification controls
- [x] Now Playing screen
- [x] Queue management

0.4 — Personal Library

- [x] Playlists
- [x] Favorites
- [x] Search
- [x] Sorting
- [x] Playback history
- [x] Sleep timer
- [x] Real settings screen

0.5 — Aero

- [x] Visual polish and typography
- [x] Animations and dynamic backgrounds
- [x] Album artwork integration
- [x] Theme customization (Light Aero / Aero Dark)
- [x] Spanish and English
- [x] Aero Home
- [x] `.aero` custom effects

0.6 — The Future We Remember Update

- [x] Aero Desktop (horizontal and vertical)
- [x] Home-screen widgets
- [x] Quick Settings tile
- [x] Aero Rest Mode
- [x] Album Showcase
- [x] Playback Queue view
- [x] Album art as environment
- [x] Touch microinteractions

Toward 1.0 — First Stable Release

- [ ] Stable local playback
- [ ] Reliable library management
- [ ] Playlist import / export
- [ ] Backup / restore
- [ ] Final Aero Player visual identity

---

🌱 Design Philosophy

Aero Player takes inspiration from an era when software interfaces were allowed to be colorful, expressive, glossy, and playful.

The goal isn't to recreate an old operating system.

Instead, Aero Player takes the spirit of that era and combines it with modern Android design and usability.

Glass. Water. Sky. Light. Music.

🌊 🫧 ☁️ 🎵

---

🔒 Privacy

Aero Player is designed with a local-first approach.

The basic music-player experience does not require:

- An account
- Cloud storage
- A subscription
- A mandatory internet connection

The app does not request the internet permission. Online functionality, if introduced, will remain separate from the core local playback experience.

---

📦 Building

Requirements: Android 8.0 (API 26) or newer to run the app. To build it you need a JDK, the Android SDK (compileSdk 37), and Gradle.

Open the project in Android Studio, or build it from the command line with Gradle (`assembleDebug` for a debug build, `assembleRelease` for a release build).

---

🤝 Contributing

Aero Player is an open-source project.

Issues, bug reports, suggestions, and pull requests are welcome as the project develops.

Before contributing major changes, please check the existing issues and project roadmap.

---

📸 Screenshots

«Screenshots will be added as the interface develops.»

---

📄 License

License information will be added when the initial project structure is established.

---

<p align="center">
  <strong>🌊 Aero Player</strong><br>
  <i>Let your music breathe.</i>
</p>
