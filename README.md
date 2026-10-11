<div align="center">

# 🎧 Music Pro

### Your library. Your sound. Your style.

A modern Android music player focused on **music stored on your device** — with a clean dark-first interface, practical playback controls, playlists, and personal listening tools.

<p>
  <a href="https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml"><img src="https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml/badge.svg" alt="Android APK workflow"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Playback-AndroidX%20Media3-34A853" alt="AndroidX Media3"></a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android">
</p>

[**Download a CI build**](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml) · [**Releases**](https://github.com/Narsing-s/Music-pro/releases) · [**Source code**](https://github.com/Narsing-s/Music-pro) · [**Report a bug**](https://github.com/Narsing-s/Music-pro/issues)

</div>

---

## ✨ Designed for your music

Music Pro brings the songs already on your phone into one focused listening experience. Browse your collection, manage your queue, customize the look, and keep your music playing while you explore the app.

| Experience | Highlights |
| --- | --- |
| 🎵 **Your local library** | Browse available tracks, albums, artists, genres, favorites, and playlists on your device. Telugu songs and devotional music are supported when their audio files are stored locally. |
| 🎛️ **Player controls** | Play, pause, seek, skip, shuffle, repeat, and manage the queue from the full player. |
| ⚡ **Quick Mixes** | Start a shuffled mix for shorter tracks or longer listening sessions. |
| ❤️ **Favorites & Insights** | Keep favorites and playlists; view local play counts and listening-time recaps. |
| 🎨 **Personal appearance** | Choose Dark, Light, or Custom appearance options. |
| 🎤 **Lyrics support** | Import local lyrics and display supported timed LRC or TTML lyrics, including standard LRC offset metadata. |
| 📂 **Playlist portability** | Import and export M3U playlists, with Unicode names such as Telugu preserved. |
| 🔒 **Background playback** | Use Android media notifications, lock-screen controls, and supported home-screen widgets. |
| 🧩 **Compact mini-player** | Hide the mini-player with **×** without stopping the currently playing song. |

## 📴 Offline-first, local music

Music Pro is a **local music player, not an online streaming catalogue**.

- Plays audio files available on your Android device.
- No account, music catalogue API key, or internet connection is required for local playback.
- Online music discovery and cloud streaming features have been removed.
- Your library and listening insights are intended for on-device use.
- Lyrics lookup may use an online service when that feature is used; this is separate from music playback.

> **Looking for a song that is not listed?** Make sure the audio file is downloaded to your phone and that Music Pro has the required media permission. Music Pro does not fetch songs from YouTube or a cloud catalogue.

## 🏠 A cleaner Home screen

The Home screen prioritizes search, Quick Access, and Quick Mixes rather than oversized promotional controls. Quick Access offers shortcuts to the Library, Favorites, and Player. Shuffle remains in the full player, next to Repeat, where it is easier to understand in context.

The mini-player can be dismissed independently of playback. It becomes available again when you start another track.

## 🎚️ Playback at a glance

| Control / feature | Behavior |
| --- | --- |
| **Shuffle** | Toggle shuffled playback; the highlighted state indicates when it is enabled. |
| **Repeat** | Use the repeat mode offered by the player. |
| **Queue** | View and manage queued tracks using the available reorder and remove actions. |
| **Quick Listen** | Create a shuffled mix from tracks shorter than 3 minutes. |
| **Long Session** | Create a shuffled mix from tracks longer than 6 minutes. |
| **Mini-player ×** | Hide the compact panel without stopping the song. |
| **Home-screen widgets** | Add the available resizable or compact widget from your Android launcher's Widgets picker. Supported controls include previous, play/pause, next, shuffle, and repeat. |

Quick Mixes require matching tracks in your library. They use the existing playback flow and do not edit or delete your audio files.

## 🧭 Browse and organize

Explore Home, Tracks, Albums, Playlists, Artists, Genres, Favorites, Insights, and other available library views. Search by track, artist, or album and use the sorting or filtering options available in your build.

- **Albums:** available All / Albums / EPs / Singles filters can narrow the list.
- **Album details:** app-local display overrides can set album name, artist, year, genres, and EP status without modifying the original audio files.
- **Insights:** play counts are recorded after 30 seconds of active playback. Recaps include all-time, this-month, and this-year views. Optimize can remove insight entries for tracks no longer in the current library; insight history can be cleared separately.
- **M3U playlists:** import and export playlists. Exports preserve Unicode names and Android media URIs while sanitizing metadata that could otherwise create accidental extra playlist entries.

## 📱 Background playback

1. Start a song in Music Pro.
2. Leave the app or lock your phone.
3. Use the Android media notification, lock-screen controls, or a supported widget.
4. If playback is interrupted, review the app's battery and background-activity settings.

Background behavior varies by Android version, device manufacturer, permissions, and battery-management settings. Force-stopping the app, battery exhaustion, revoked permissions, or aggressive power restrictions can interrupt playback. Verify screen-off behavior on your own device.

## 📥 Download the Android APK

1. Open [**Android APK workflow runs**](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Select a run for the commit you want to test.
3. Confirm that the run completed successfully.
4. Open the run and find the **Artifacts** section.
5. Download **`narsing-music-debug-apk`** and extract the ZIP.
6. Install **`app-debug.apk`** on your Android device. Android may ask you to allow installation from that source.

The artifact also includes **`app-debug.apk.sha256`** for checksum verification on a compatible computer or shell.

> [!IMPORTANT]
> GitHub Actions artifacts are debug builds intended for testing, not production-signed releases. A successful CI run verifies the configured build and test tasks; it does not replace real-device testing. A debug APK signed with a different key may not install as an update over an existing copy.

For published release packages, check the [**Releases page**](https://github.com/Narsing-s/Music-pro/releases).

## 🛠️ Build from source

### Requirements

- Android Studio (recent stable version)
- JDK 17
- Android SDK configured for the project

### Clone and build

```bash
git clone https://github.com/Narsing-s/Music-pro.git
cd Music-pro
gradle testDebugUnitTest assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Alternatively, open the project in Android Studio, sync Gradle, and run the `app` configuration.

## 🔐 Privacy & permissions

Music Pro reads audio metadata from your device's media collection and stores app preferences locally. No account is required for local playback. Sharing is initiated by you through Android's Sharesheet; review the destination and content before sending.

Grant only the Android permissions needed for the features you use. Do not include private audio, personal listening history, or sensitive device details in public bug reports.

## 🧭 Advanced modernization plan

Want to know what is still worth improving? See the [**Advanced Modernization Roadmap**](docs/ADVANCED_MODERNIZATION_ROADMAP.md) for prioritized reliability checks, responsive UI/accessibility polish, optional advanced features, and a release checklist. It is intentionally designed to preserve the current screens and working playback controls.

## 🧪 Project status

Music Pro is actively developed. The [Android APK workflow](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml) runs the configured unit-test task and builds a debug APK. Media permissions, playback, background behavior, widgets, and screen layouts should still be checked on a physical device after changes. Production signing and a signed release APK/AAB are separate from the debug build.

## 🤝 Feedback & contributions

- 🐛 **Bug reports:** [Open an issue](https://github.com/Narsing-s/Music-pro/issues)
- 💻 **Source code:** [Browse the repository](https://github.com/Narsing-s/Music-pro)
- 🧰 **Build logs and APK artifacts:** [Android APK workflow](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml)
- 📦 **Published builds:** [Releases](https://github.com/Narsing-s/Music-pro/releases)

When reporting a bug, include your Android version, device model, clear reproduction steps, and relevant logs with personal information removed.

---

<div align="center">

**MUSIC PRO**  
*Less clutter. More music.*

[**Explore Music Pro →**](https://github.com/Narsing-s/Music-pro)

</div>
