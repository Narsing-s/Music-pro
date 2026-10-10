# Narsing Music — Phone Test & Demo Checklist

Use this checklist before sharing a build publicly. A successful CI build confirms compilation and packaging, not every real-device behavior.

## 1. Get the latest build

1. Open [Android APK workflow runs](https://github.com/Narsing-s/Music-pro/actions/workflows/android-apk.yml).
2. Open the latest run for the commit you want to test.
3. Confirm the run says **Success**.
4. In Artifacts, download `narsing-music-debug-apk` and extract the APK.
5. Install it on your own Android phone. If Android warns that this is an APK from outside Play Store, only continue if you trust the source and understand this is a test build. Do not ask others to disable security protections.

## 2. Test app sharing

- [ ] Open Narsing Music.
- [ ] Tap the Share icon in the top toolbar.
- [ ] Confirm Android's native share chooser appears.
- [ ] Choose a destination such as Messages or WhatsApp, but review the message before sending.
- [ ] Confirm the message includes the app description and GitHub URL.
- [ ] Cancel the share chooser and confirm the player remains usable.

## 3. Test Now Playing sharing

- [ ] Open a track and enter Now Playing.
- [ ] Tap **SHARE** beneath the track details.
- [ ] Confirm the preview contains the title, artist, Narsing Music attribution, and GitHub URL.
- [ ] Confirm no local file path, file URI, or audio attachment is included.
- [ ] Cancel sharing and confirm playback continues.

## 4. Test core playback after the change

- [ ] Play, pause, next, and previous still work.
- [ ] Shuffle and repeat still work.
- [ ] Lock the screen and confirm playback behavior.
- [ ] Return to the app after sharing and confirm the selected track is still correct.
- [ ] Test with an empty library if possible.
- [ ] Test a long title and an unknown artist.

Record the phone model, Android version, result, and any issue. A test is not passed until it has been tried on a real device.

## 5. Record a 15-second vertical demo

Record the actual phone screen, not a mockup. Keep notifications and private information hidden.

**Shot list**
- **0–2 seconds:** Open the app. On-screen text: “Your local music, a cleaner player.”
- **2–6 seconds:** Show the music library and tap a track.
- **6–10 seconds:** Show the Now Playing screen and a playback control.
- **10–13 seconds:** Tap SHARE and show the native share chooser. Do not send a message on camera.
- **13–15 seconds:** Show the repository link and on-screen text: “Help test Narsing Music.”

Use captions so the clip makes sense without sound. Do not claim a feature works unless it passed the checklist above.

## 6. Copy-ready caption

🎧 I'm building Narsing Music — a modern Android music player for music stored on your phone.

I'm looking for a few Android users to try the current test build and give honest feedback on the interface, playback, and sharing experience.

Explore the project: https://github.com/Narsing-s/Music-pro

Built with Kotlin, Jetpack Compose, and AndroidX Media3.

#Android #Kotlin #AndroidDevelopment #MusicPlayer #OpenSource

*Label the download as a test build and include install instructions when sharing the APK.*

## 7. Collect useful feedback

Ask testers these three questions:
1. Could you install and open the app without help?
2. Could you find a song and use the main playback controls?
3. What is the one thing you would change first?

Do not ask for positive ratings. Fix repeated issues before expanding the audience.
