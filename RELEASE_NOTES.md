# Narsing Music — Release Notes

## Candidate: v0.1.0

### Highlights
- Native Android music player built with Kotlin, Jetpack Compose, and AndroidX Media3.
- Local library views for tracks, albums, playlists, artists, genres, folders, recent plays, and favorites.
- Search, sorting, queue management, shuffle, repeat, and playback controls.
- Background playback with system media notification and lock-screen controls.
- Custom vinyl-inspired app identity and monochrome/red styling.
- Track details dialog includes an explicit Cancel option; dismissing it leaves track data unchanged.
- README updated with project features, build steps, CI APK instructions, and background-playback notes.

### Testing status
- CI builds a **debug APK** using `gradle assembleDebug`.
- Test on a physical Android device before treating this candidate as stable.
- This is not a production-signed APK/AAB. Production signing and store distribution are not included.

### Install a CI build
1. Visit https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml
2. Open a successful run.
3. Download the `narsing-music-debug-apk` artifact.
4. Extract the ZIP and install `app-debug.apk`.
