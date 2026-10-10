# Narsing Music

A native Android local music player with an original monochrome-and-red identity, a clean Compose interface, and reliable system-integrated playback.

## Features
- Kotlin + Jetpack Compose UI with dark monochrome surfaces and red accent
- Custom adaptive launcher icon (vinyl record + music note)
- Local audio discovery through Android MediaStore
- Search songs, artists, and albums
- Playback powered by AndroidX Media3 ExoPlayer
- Background playback through a dedicated MediaSessionService
- Android media notification, lock-screen, headset, and Bluetooth controls
- Audio-focus handling and pause-on-headphone-disconnect behavior
- Local-first privacy: no account and no upload of music
- GitHub Actions debug APK build

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

## Build
Open the project in Android Studio, sync Gradle, and run the `app` configuration. Or use GitHub Actions (`Android APK`) to build a debug APK.

## Background playback
1. Start a song in Narsing Music.
2. Swipe Home or lock the phone; playback is handled by the media playback service rather than the screen.
3. Use the media controls in the notification or lock screen to pause, resume, or skip.
4. If your phone manufacturer aggressively restricts background apps, set Narsing Music battery usage to **Unrestricted** in Android Settings. Android can still stop playback if the user force-stops the app, the device runs out of power, or the system revokes permissions.

The app declares Android's media-playback foreground-service permissions and a wake-lock permission. Playback continuity should be tested on a physical device, including screen-off and battery-saver scenarios, because OEM power management differs.

## Privacy
The app reads audio metadata from the device's media collection and does not upload music or require an account.

## Roadmap
1. Room-backed playlists and favorites.
2. Lyrics import and embedded lyrics.
3. Sleep timer, equalizer integration, vinyl animation.
4. Release signing and release APK/AAB workflow.

Branding: Narsing Music. Original artwork; no third-party brand marks or proprietary assets.
