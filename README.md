<div align="center">

# 🎧 MUSIC PRO

### Your library. Your sound. Your style.

**A modern, offline-first Android music player** built for the music already on your phone.

[![Android APK](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml/badge.svg)](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Media3](https://img.shields.io/badge/Playback-AndroidX%20Media3-34A853)](https://developer.android.com/media/media3)

[**Browse source**](https://github.com/Narsing-s/Music-pro) · [**Download a CI build**](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml) · [**Releases**](https://github.com/Narsing-s/Music-pro/releases) · [**Report an issue**](https://github.com/Narsing-s/Music-pro/issues)

</div>

---

## ✨ A cleaner way to listen

Music Pro brings your on-device audio into a focused listening experience with a dark-first interface, vivid red accents, and controls where you expect them. Browse your collection, manage the queue, and keep playback going while exploring the app.

<table>
  <tr>
    <td width="50%">
      <h3>🎲 Shuffle in the player</h3>
      Use the clearly labeled **SHUFFLE / SHUFFLE ON** control beside Repeat in the full player, instead of a large Home-screen promo.
    </td>
    <td width="50%">
      <h3>🎚️ Full playback control</h3>
      Play, pause, skip, seek, repeat, shuffle, and manage the play queue.
    </td>
  </tr>
  <tr>
    <td>
      <h3>🎵 On-device library</h3>
      Browse tracks, albums, artists, and available music collections from your device.
    </td>
    <td>
      <h3>🧩 Dismissible mini-player</h3>
      Close the compact player with **×** while browsing. Dismissing the panel does not stop the current song; selecting another track brings it back.
    </td>
  </tr>
  <tr>
    <td>
      <h3>❤️ Personal listening</h3>
      Keep favorites and playlists, use recent-track and queue features, and open the new **Insights** destination for private on-device play counts and listening time. A play is counted after 30 seconds of active playback; insight history can be cleared independently.
    </td>
    <td>
      <h3>🎨 Your style</h3>
      Switch between Dark, Light, and Custom appearance options.
    </td>
  </tr>
  <tr>
    <td>
      <h3>⚡ Quick Mixes</h3>
      Start a shuffled mix for shorter listening or longer sessions from the Home screen.
    </td>
    <td>
      <h3>🔒 Background playback</h3>
      Use Android media notifications and lock-screen controls where supported.
    </td>
  </tr>
  <tr>
    <td>
      <h3>🎤 Synchronized lyrics</h3>
      Import local lyrics and use timed LRC or TTML lyrics where the file format is supported, including standard LRC offset metadata.
    </td>
    <td>
      <h3>📂 Playlist portability</h3>
      Import and export M3U playlists to move a playlist between compatible music apps. Export filenames preserve Unicode names (including Telugu) while removing filesystem-reserved characters.
    </td>
  </tr>
</table>

## 🏠 Home, without the clutter

The Home screen prioritizes search, Quick Access, and Quick Mixes. The large **Surprise Me** promotional button and the redundant track-statistics strip have been removed to reduce visual clutter. Shuffle remains available in the player, where it belongs.

Quick Access provides shortcuts to the Library, Favorites, and Player. The player mini-panel can be dismissed independently of playback, so you can reclaim screen space without interrupting your music.

## 🎛️ Playback & Quick Mixes

| Feature | What it does |
| --- | --- |
| **Shuffle** | Tap the labeled Shuffle control in the full player; its highlighted state shows when shuffle is enabled |
| **Repeat** | Use the available repeat mode in the player |
| **Queue** | View and manage queued tracks, including available reorder and remove actions |
| **Quick Listen** | Create a shuffled mix from tracks shorter than 3 minutes |
| **Long Session** | Create a shuffled mix from tracks longer than 6 minutes |
| **Mini-player ×** | Hide the compact player without stopping the current song |

A Quick Mix needs matching tracks in your library to start. Mix selection uses the existing playback flow and does not edit or delete audio files.

## 🧭 Explore your collection

Use the app's navigation to explore Home, Tracks, Albums, Playlists, Artists, Genres, Favorites, Insights, and other available library views. Search by track, artist, or album and use the sorting or filtering options available in your build. In Albums, use the edit control to set app-local album name, artists, year, genres, and EP status; these display overrides never modify the original audio files.

Tap a song to play it. Open the full player for playback controls. While browsing, use the mini-player to return to the current track; tap **×** if you want to hide that panel. Starting a different track makes the mini-player available again.

## 📱 Background playback

1. Start a song in Music Pro.
2. Leave the app or lock your phone.
3. Use the Android media notification or lock-screen controls when available.
4. If playback is interrupted, review the app's battery/background-activity settings on your device.

Background behavior depends on Android version, device manufacturer, permissions, and power-management settings. Force-stopping the app, battery exhaustion, revoked permissions, or aggressive battery restrictions can interrupt playback. Verify screen-off playback on your own device.

## 📥 Get the Android APK

1. Open [Android APK workflow runs](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Select the run for the commit you want to test and confirm its status is **successful**.
3. Open the run's **Artifacts** section and download `narsing-music-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on your Android device. Android may ask you to allow installation from that source.
5. The artifact includes `app-debug.apk.sha256` for checksum verification on a compatible computer or shell.

> [!IMPORTANT]
> CI artifacts are debug builds intended for testing, not production-signed releases. A successful build confirms compilation and the configured test tasks; it does not replace real-device testing. A debug APK may not install as an update over a version signed with a different key.

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

You can also open the project in Android Studio, sync Gradle, and run the `app` configuration.

## 🎛️ Player controls & home-screen widget

The full Now Playing screen includes **Shuffle** and **Repeat** controls beside playback options. Music Pro now provides two independently addable home-screen widget layouts: the original resizable widget and a compact, high-contrast variant. Both provide previous, play/pause, next, shuffle, and repeat actions; active shuffle/repeat states are highlighted and synchronized with the playback service. Add either widget from your Android launcher's Widgets picker. Existing in-app screens and playback flows remain unchanged.

## 🧩 Playlist reliability

M3U exports preserve Unicode song and playlist names, keep Android media URIs intact, and sanitize multiline track metadata so titles or artist names cannot accidentally create extra playlist entries. The export flow and existing screen layout remain unchanged.

## 🔐 Privacy

Music Pro is designed for local listening: it reads audio metadata from your device's media collection and stores app preferences locally. No account is required to use the local music library. Sharing is initiated by you through Android's Sharesheet; review the destination and preview before sending anything.

Grant only the Android permissions needed for the features you use. Avoid including private audio, personal listening history, or sensitive device details in public bug reports.

## 🧪 Project status

Music Pro is actively developed. The GitHub Actions workflow builds a debug APK and runs the configured unit-test task. Playback, media permissions, device-specific background behavior, and visual layout should still be verified on a physical device before relying on a change. Production signing and a signed release APK/AAB are separate from the debug build.

## 🤝 Feedback & contributions

- **Bug reports:** [Open an issue](https://github.com/Narsing-s/Music-pro/issues)
- **Source and changes:** [Browse the repository](https://github.com/Narsing-s/Music-pro)
- **Build status and APK artifacts:** [Android APK workflow](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml)

When reporting a bug, include your Android version, device model, steps to reproduce, and relevant logs with personal information removed.

---

<div align="center">

**MUSIC PRO**  
*Less clutter. More music.*

[Explore the repository →](https://github.com/Narsing-s/Music-pro)

</div>
