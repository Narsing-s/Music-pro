<div align="center">

# 🎧 Narsing Music

### Your library. Your sound. Your style.

A modern, local-first Android music player with a clean dark aesthetic, bold red accents, and a dedicated **Quick Mixes** panel.

[![Android APK](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml/badge.svg)](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Media3](https://img.shields.io/badge/Playback-AndroidX%20Media3-34A853)](https://developer.android.com/media/media3)

[**Get the source**](https://github.com/Narsing-s/Music-pro) · [**Builds & APK artifacts**](https://github.com/Narsing-s/Music-pro/actions) · [**Releases**](https://github.com/Narsing-s/Music-pro/releases) · [**Report a bug**](https://github.com/Narsing-s/Music-pro/issues)

</div>

---

## ✨ Made for your music

Narsing Music brings songs stored on your Android device into one focused listening experience. Browse your library, build playlists, discover a mix by track length, and keep listening while you move around the app.

<table>
  <tr>
    <td width="50%">
      <h3>⚡ Quick Mixes</h3>
      A dedicated Home panel with shuffled <b>Quick Listen</b> and <b>Long Session</b> mixes.
    </td>
    <td width="50%">
      <h3>🎵 Your library</h3>
      Browse tracks and collections discovered from local device storage.
    </td>
  </tr>
  <tr>
    <td>
      <h3>🎛️ Playback essentials</h3>
      Play, pause, skip, seek, shuffle, repeat, and manage the queue.
    </td>
    <td>
      <h3>🌙 Keep listening</h3>
      Media playback service with Android media notification and lock-screen controls.
    </td>
  </tr>
  <tr>
    <td>
      <h3>❤️ Personal collections</h3>
      Favorites, playlists, and recently played tracks.
    </td>
    <td>
      <h3>🎨 Your look</h3>
      Dark, light, and custom appearance options with a monochrome and red-accent design.
    </td>
  </tr>
</table>

## 🚀 Quick Mixes

Quick Mixes has its own panel on the Home screen, keeping listening shortcuts visually separate from the other library controls.

| Mix | Track length | What happens |
| --- | --- | --- |
| **Quick Listen** | Under 3 minutes | Shuffles matching tracks for a shorter listen |
| **Long Session** | Over 6 minutes | Shuffles matching tracks for a longer session |

A mix with no matching songs cannot start playback. Selecting a mix uses the app's existing playback flow and does not edit or delete music files.

## 🧭 Explore the library

Depending on the available device media and app navigation, browse Home, Tracks, Albums, Artists, Playlists, Favorites, Recent, and other library views. Search by track title, artist, or album, and use the available sorting and duration filters.

Tap a track to start playback. Use the mini-player to return to the player while browsing. The track-details dialog has a **Cancel** option that closes the dialog without changing the track. Share actions use Android's native Sharesheet; review the text before sending it to another app.

## 🎧 Background playback

1. Start a song in the app.
2. Leave the app or lock the phone.
3. Use the Android media notification or lock-screen controls to manage playback.
4. If your phone restricts background activity, try setting the app's battery usage to **Unrestricted** in Android Settings.

Background behavior depends on Android version and manufacturer settings. Test screen-off playback on your own device. Force-stopping the app, battery exhaustion, revoked permissions, or aggressive power management can interrupt playback.

## 🛠️ Build from source

### Requirements

- Android Studio (recent stable release)
- JDK 17
- Android SDK 35

### Build and run unit-test task

```bash
git clone https://github.com/Narsing-s/Music-pro.git
cd Music-pro
gradle testDebugUnitTest assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also open the project in Android Studio and run the `app` configuration after Gradle sync.

## 📦 Download a CI build

1. Open [Android APK workflow runs](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Choose a completed run with a **successful** status for the latest commit you want to test.
3. In the run's **Artifacts** section, download `narsing-music-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on your Android device. You may need to allow installation from that source in Android settings.
5. The artifact includes `app-debug.apk.sha256`. To verify the APK after extraction, run `sha256sum -c app-debug.apk.sha256` on a computer or compatible shell.

> [!NOTE]
> CI artifacts are debug builds intended for testing, not production-signed releases. Check the workflow result for the specific commit before installing; an older successful run does not confirm that the latest source builds successfully. Debug APKs may not install as an update over a differently signed build.

## 🔐 Privacy-first approach

The app reads audio metadata from your device's media collection and stores app preferences locally. It does not require an account or upload your music. Sharing is initiated by the user through Android's Sharesheet; check the share preview before choosing a destination.

## 🧪 Project status

Narsing Music is under active development. Please validate changes on a physical Android device before relying on them. Production signing and a signed release APK/AAB workflow are separate from the current debug-build workflow.

## 🤝 Feedback & contributions

- **Bug reports:** [Open a bug report](https://github.com/Narsing-s/Music-pro/issues/new?template=bug_report.yml)
- **Feature ideas:** [Request a feature](https://github.com/Narsing-s/Music-pro/issues/new?template=feature_request.yml)
- **Contributing:** Read [CONTRIBUTING.md](https://github.com/Narsing-s/Music-pro/blob/main/CONTRIBUTING.md)
- **Security:** Read [SECURITY.md](https://github.com/Narsing-s/Music-pro/blob/main/SECURITY.md)

When reporting a bug, include Android version, device model, steps to reproduce, and relevant logs with personal data removed. Do not attach private audio files, credentials, or listening history.

---

<div align="center">

**Narsing Music**  
*Made for the music already on your phone.*

[Explore the repository →](https://github.com/Narsing-s/Music-pro)

</div>
