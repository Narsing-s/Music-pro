# Music Pro — Feature Comparison & Safe Roadmap

This review compares Music Pro with [MissingCore/Music](https://github.com/MissingCore/Music), a separate local Android music player. The goal is to borrow general feature ideas—not its source code, branding, assets, or UI—and keep Music Pro's existing interface and behavior intact.

## Non-negotiable boundaries

- Preserve Music Pro's current Jetpack Compose screens, navigation, layout, controls, and playback behavior.
- Do not copy source code, assets, strings, or UI from MissingCore/Music. Its repository declares the **AGPL-3.0** license, so code reuse requires deliberate license-compatibility review.
- Add capabilities behind existing flows or as optional enhancements; do not create duplicate controls, unexplained blank space, or new permissions without a clear need.
- Require unit tests and a passing Android build for code changes. Real-device checks remain separate.

## Source-level feature comparison

| Area | Music Pro status found in source | Remaining opportunity |
| --- | --- | --- |
| Local library, favorites, playlists, genres | Present in current app code. | Keep testing permission recovery, unknown metadata, empty libraries, and duplicate tracks. |
| Background playback and media notification | Media3 `PlaybackService` and media-library service are declared. | Verify screen-off playback, audio focus, notification actions, and reconnect behavior on physical devices. |
| Queue management | Queue inspection, play, remove, and move callbacks are present. | Add regression coverage for removing/moving the active and final queue item. |
| Shuffle and repeat | Present; Shuffle is available in the full player. | Keep the full-player placement and saved state; do not re-add the Home-screen promo. |
| Equalizer and playback speed | Five-band settings and playback-speed persistence are present. | Validate saved values after process/service recreation and confirm the displayed dB matches applied values. |
| Sleep timer | Implemented in the existing UI using a cancellable Compose effect. | Test cancellation, changing the timer, leaving/re-entering screens, and playback stopping only when the active timer expires. |
| Metadata editor | Present; app-local title, artist, and album overrides are stored in preferences. | Keep edits app-local and clearly avoid implying the underlying audio file was rewritten. |
| Artwork and appearance | Custom artwork and theme/font preferences are present. | Test missing/unreadable artwork URIs and reset behavior; avoid a broad redesign. |
| Lyrics | Local lyrics editor/import and LRC timestamp parsing are present. This update adds TTML parsing support with tests. | Validate provider/network failures, malformed files, and synchronized seeking on a device. |
| M3U playlists | Import/export flows are present. | Test malformed files, duplicate/missing tracks, Unicode titles/paths, empty playlists, and round-trip export/import. |
| Multi-select | Selection state exists in the current screen code. | Audit every bulk action for selection cleanup, cancellation safety, and accurate feedback. |
| Recent listening | A local recent-track list is persisted (up to 30 IDs). | A richer listening recap/play-count view could be an optional future feature, with clear local-data erase controls. |
| Android Auto | Media3 media-library service and automotive descriptor are declared. | Verify discoverability and playback in Android Auto / Desktop Head Unit; don't change the manifest unless testing identifies a real gap. |
| Home-screen widget | A playback widget and receiver are present. | Verify state refresh, multiple widget instances, and behavior after app/process restart. |

## Changes made from this comparison

- Added a dedicated timed-lyrics parser that supports existing LRC timestamps and common TTML timed paragraphs.
- Added regression tests for multi-timestamp LRC lines, TTML paragraphs with nested spans, seconds-based timing, and malformed/untimed input.
- Kept the existing UI and lyric editing flow unchanged. TTML support is a parser capability; final behavior still needs a real-device check with actual lyric files.

## Recommended next steps

1. **Reliability first:** finish phone tests for playback, queue edge cases, widget state, equalizer persistence, timer cancellation, permission recovery, and headphone/Bluetooth interruptions.
2. **Playlist portability:** harden M3U import/export with tests for malformed input, Unicode, missing media, and duplicates.
3. **Listening insights:** if added later, make play history opt-in or clearly disclosed, local-only, and easy to clear; don't add network tracking.
4. **Automotive verification:** test the existing Media3 integration before changing service declarations.
5. **Accessibility and resilience:** verify touch targets, content descriptions, long-title handling, reduced-animation behavior, and empty/error states without changing the visual design.

## Acceptance criteria

- No regressions to current screens, controls, playback, queue behavior, or saved settings.
- No copied code/assets/UI from the reference project.
- New parsing or business logic has automated tests.
- GitHub Actions passes unit tests and builds the debug APK.
- Real-device results are recorded separately; CI success alone is not proof of Android Auto, audio-focus, or OEM-specific behavior.

## References

- [MissingCore/Music repository and feature list](https://github.com/MissingCore/Music)
- [MissingCore/Music license](https://github.com/MissingCore/Music/blob/dev/LICENSE)
- [Music Pro repository](https://github.com/Narsing-s/Music-pro)
