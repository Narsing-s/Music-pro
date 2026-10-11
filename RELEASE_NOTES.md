# Music Pro — Release Notes

## Current development build: v0.1.1

Music Pro is an offline-first Android music player built with Kotlin, Jetpack Compose, and AndroidX Media3. This is a development release; validate the APK on a physical device before relying on it as a stable release.

### Features in the repository

- Browse local tracks, albums, artists, genres, playlists, favorites, and recent listening.
- Search and organize a local music collection, with queue controls, shuffle, repeat, and Quick Mixes.
- Background audio playback through Media3 with system media-session and lock-screen controls.
- Local lyrics import/display with LRC and supported TTML parsing.
- Import and export M3U playlists, preserving Unicode names and sanitizing metadata line breaks.
- App-local track and album metadata overrides without modifying original audio files.
- Local listening insights and play-count summaries.
- Dark, light, and custom appearance options.
- Two home-screen playback widget layouts with playback, previous/next, shuffle, and repeat controls.
- Equalizer, playback-speed controls, and sleep timer options in the existing app experience.

Feature availability and behavior can vary by Android version and device. Do not interpret this feature list as proof of physical-device validation.

### Build and quality checks

The Android APK GitHub Actions workflow is configured to run:

- `gradle testDebugUnitTest`
- `gradle lintDebug`
- `gradle assembleDebug`

When generated, the Android lint HTML report is uploaded as a workflow artifact. A successful run also generates a SHA-256 checksum for the debug APK and uploads both files.

### Download a CI debug APK

1. Visit [Android APK workflow runs](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Open the latest successful run.
3. Find the **Artifacts** section and download `narsing-music-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on your Android device.

The CI artifact is a debug build for testing, not a production-signed release. Installing it over an existing version may fail if the signing keys differ. For published builds, check the [Releases page](https://github.com/Narsing-s/Music-pro/releases).

### Before calling a build stable

- Test Telugu songs, devotional music, and files with incomplete metadata.
- Test background playback, audio interruptions, notification controls, and screen-off playback.
- Test rotation, large font sizes, accessibility descriptions, and both widgets at compact sizes.
- Test playlist import/export, lyrics, queue editing, shuffle/repeat, and sleep-timer cancellation.
- Verify offline playback with network disabled.
- Review unit-test and lint results and inspect device logs for crashes.

See the [Advanced Modernization Roadmap](docs/ADVANCED_MODERNIZATION_ROADMAP.md) and [Phone Testing Checklist](docs/PHONE_TESTING_AND_DEMO_CHECKLIST.md) for the planned validation process.
