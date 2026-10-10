# Narsing Music

<p align="center">
  <strong>A clean, modern music player for your Android library.</strong><br>
  Built with Kotlin, Jetpack Compose, and AndroidX Media3.
</p>

<p align="center">
  <a href="https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml">Android APK builds</a> ·
  <a href="https://github.com/Narsing-s/Music-pro/releases">Releases</a> ·
  <a href="https://github.com/Narsing-s/Music-pro/issues">Report an issue</a>
</p>

Narsing Music brings your on-device songs into a focused listening experience, with a monochrome interface, red accents, quick library navigation, and playback designed to continue when you leave the app.

## At a glance

| Feature | What it does |
| --- | --- |
| 🎵 **Your music library** | Browse audio discovered on your Android device |
| ⚡ **Quick Mixes panel** | Start a shuffled **Quick Listen** mix (under 3 minutes) or **Long Session** mix (over 6 minutes) |
| 🔎 **Find your tracks** | Search songs, artists, and albums; sort and filter the library |
| ❤️ **Make it yours** | Favorites, playlists, recent tracks, metadata overrides, and artwork |
| 🎧 **Playback controls** | Play/pause, next/previous, seek, shuffle, repeat, and queue |
| 🌙 **Background listening** | Media playback service with notification and lock-screen controls |
| 🎨 **Personal styling** | Dark, light, and custom appearance options |
| 🔒 **Local-first** | No account required; your music stays on your device |

## Home screen

The Home screen includes library shortcuts, collection statistics, and a **dedicated Quick Mixes panel**. The panel groups listening options separately from the rest of the Home content:

- **Quick Listen** — plays a shuffled selection of tracks shorter than 3 minutes.
- **Long Session** — plays a shuffled selection of tracks longer than 6 minutes.

A mix with no matching tracks remains unavailable to play. These shortcuts use the existing playback flow and do not alter your music files.

## Library and playback

Browse the available **Home, Tracks, Albums, Artists, Playlists, Favorites, and Recent** views, along with other library navigation supported by the app. Search by track title, artist, or album, and use the available sorting and duration filters.

Tap a track to start listening and use the mini-player to return to playback while browsing. The track-details dialog includes a **Cancel** option to close it without changing the track.

## Background playback

1. Start a song.
2. Leave the app or lock your phone.
3. Use Android's media notification or lock-screen controls to manage playback.
4. If your device restricts background apps, try setting the app's battery usage to **Unrestricted** in Android Settings.

Playback behavior can vary by Android version and device manufacturer. Test screen-off playback on your own device; force-stopping the app, battery exhaustion, revoked permissions, or aggressive power management can interrupt playback.

## Build from source

### Requirements

- Android Studio (recent stable release)
- JDK 17
- Android SDK 35

### Build a debug APK

1. Clone this repository:
   ```bash
   git clone https://github.com/Narsing-s/Music-pro.git
   cd Music-pro
   ```
2. Open the project in Android Studio and allow Gradle to sync.
3. Build the app:
   ```bash
   gradle assembleDebug
   ```
4. Find the APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

## Get a CI build

1. Open [Android APK workflow runs](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Select a completed run with a **successful** status.
3. Under **Artifacts**, download `narsing-music-debug-apk` if that artifact is available for the run.
4. Extract the downloaded ZIP and install `app-debug.apk` on your Android device. You may need to allow installation from that source in Android settings.

> **Note:** CI builds are debug builds for testing, not production-signed releases. Check the workflow run before using its artifact; a successful older run does not guarantee the latest source has built successfully.

## Privacy

Narsing Music reads audio metadata from your device's media collection and stores app preferences locally. The app does not require an account or upload your music.

## Project status

This project is under active development. Validate changes on a physical Android device before relying on them. Production signing and a signed release APK/AAB workflow are separate from the current debug-build workflow.

## Contributing and feedback

Found a bug or have an improvement in mind? [Open an issue](https://github.com/Narsing-s/Music-pro/issues). Please include your Android version, device model, steps to reproduce, and relevant build logs.

---

<p align="center">
  <strong>Narsing Music</strong><br>
  Your library. Your sound. Your style.
</p>
