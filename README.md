# Narsing Music

A native Android local music player starter inspired by minimalist monochrome industrial design. Fresh implementation; not a fork of MissingCore Music.

## Features
- Kotlin + Jetpack Compose UI
- Local audio discovery via Android MediaStore
- Search songs, artists, and albums
- Playback using AndroidX Media3 ExoPlayer
- Background playback via MediaSessionService
- Media notification / lock-screen controls
- Dark monochrome UI with red accent
- GitHub Actions debug APK build

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

## Build
Open in Android Studio, sync Gradle, then run the `app` configuration. Or use GitHub Actions (`Android APK`) to build a debug APK.

## Privacy
The app reads audio metadata from the device's media collection and does not upload music or require an account.

## Roadmap
1. Room-backed playlists and favorites.
2. Lyrics import and embedded lyrics.
3. Sleep timer, equalizer integration, vinyl animation.
4. Release signing and release APK/AAB workflow.

Branding: Narsing Music. Original monochrome UI; avoids Nothing trademarks and proprietary assets.
