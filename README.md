# Narsing Music

A native Android local music player built with Kotlin and Jetpack Compose. Narsing Music focuses on a clean monochrome interface with a red accent, quick access to your local library, and playback that can continue while the app is in the background.

[**Browse the source code**](https://github.com/Narsing-s/Music-pro) · [**Download the latest debug APK**](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml)

## Highlights

- **Local music library** — discover audio stored on your Android device through MediaStore.
- **Library views** — browse Home, Tracks, Albums, Playlists, Artists, Genres, Folders, Recent, and Favorites.
- **Search and sorting** — find music and sort by title, artist, duration, or recently added.
- **Playback controls** — play, pause, skip, shuffle, repeat, seek, and manage the queue.
- **Background playback** — Media3 playback service with system media notification and lock-screen controls.
- **Audio-device support** — audio focus handling and pause-on-headphone-disconnect behavior.
- **Track tools** — favorites, playlists, track details, locally saved metadata overrides, and custom artwork.
- **Original identity** — custom vinyl-inspired launcher icon and monochrome/red visual design.
- **Local-first privacy** — no account required; the app does not upload your music.
- **Automated Android builds** — GitHub Actions builds a debug APK on pushes to the main branches.

## Screens and library navigation

Use the library navigation to switch between collections. The Tracks view shows the number of visible songs and the total number of songs detected on the device. Tap a song to open the Now Playing screen; use the mini-player to return to playback while browsing the library.

Track details can be dismissed with **Cancel**. Choosing Cancel only closes the details dialog; it does not delete or modify the track.

## Requirements

- Android Studio (a recent stable release)
- JDK 17
- Android SDK 35

## Build from source

1. Clone this repository.
2. Open it in Android Studio and allow Gradle to sync.
3. Run the `app` configuration, or build a debug APK from the project root:

   ```bash
   gradle assembleDebug
   ```

4. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Download a CI build

1. Open [GitHub Actions — Android APK](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Select a successful workflow run.
3. Scroll to **Artifacts** and download `narsing-music-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on your Android device. Android may ask you to allow installation from that source.

Debug builds are for testing and are not a substitute for a production-signed release build.

## Background playback notes

1. Start a song in Narsing Music.
2. Swipe Home or lock the phone. Playback is handled by the media playback service, not by the visible screen.
3. Use the media notification or lock-screen controls to pause, resume, or skip.
4. If the phone aggressively restricts background apps, set Narsing Music battery usage to **Unrestricted** in Android Settings.

Playback continuity should be tested on a physical device, including screen-off and battery-saver scenarios. Android or manufacturer power management may stop playback if the app is force-stopped, the device runs out of power, or permissions are revoked.

## Privacy

The app reads audio metadata from the device's media collection and stores app preferences locally. It does not require an account or upload your music.

## Project status

This repository is under active development. Please test changes on a physical Android device before relying on them. The CI workflow currently produces a **debug APK**; production signing and a signed release APK/AAB workflow are separate follow-up work.

## Contributing

Bug reports and focused improvements are welcome. Please include your Android version, device model, steps to reproduce, and relevant build logs when reporting a problem.

## Branding

**Narsing Music** — original vinyl-inspired artwork and visual styling; no third-party brand marks or proprietary assets.
