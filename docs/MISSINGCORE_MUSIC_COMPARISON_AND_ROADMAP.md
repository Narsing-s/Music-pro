# Music Pro — Feature Comparison & Safe Roadmap

This review compares Music Pro with [MissingCore/Music](https://github.com/MissingCore/Music), a separate local Android music player. The goal is to learn from general feature ideas—not reuse its source code, branding, assets, strings, or UI—and keep Music Pro's existing interface and behavior intact.

## Non-negotiable boundaries

- Preserve Music Pro's current Jetpack Compose screens, navigation, layout, controls, and playback behavior.
- Do not copy source code, assets, strings, or UI from MissingCore/Music. Its repository declares the AGPL-3.0 license; code reuse requires a deliberate license-compatibility review.
- Add capabilities behind existing flows or as optional enhancements; do not create duplicate controls, unexplained blank space, or new permissions without a clear need.
- Require unit tests and a passing Android build for code changes. Real-device checks remain separate.

## Source-level feature comparison

| Area | Music Pro status found in source | Remaining opportunity |
| --- | --- | --- |
| Local library, favorites, playlists, genres | Present in current app code. | Test permission recovery, unknown metadata, empty libraries, and duplicate tracks. |
| Background playback and media notification | Media3 PlaybackService and media-library service are declared. | Verify screen-off playback, audio focus, notification actions, and reconnect behavior on physical devices. |
| Queue management | Queue inspection, play, remove, and move callbacks are present. | Add regression coverage for removing/moving the active and final queue item. |
| Shuffle and repeat | Present; Shuffle is available in the full player. | Keep the full-player placement and saved state; do not re-add a Home-screen duplicate. |
| Equalizer and playback speed | Five-band settings and playback-speed persistence are present. | Validate saved values after process/service recreation and confirm displayed values match applied values. |
| Sleep timer | Implemented in the existing UI using a cancellable Compose effect. | Test cancellation, changing the timer, leaving/re-entering screens, and playback stopping only when the active timer expires. |
| Metadata editor | App-local title, artist, and album overrides are stored in preferences. | Keep edits app-local and don't imply the underlying audio file was rewritten. |
| Artwork and appearance | Custom artwork and theme/font preferences are present. | Test missing/unreadable artwork URIs and reset behavior; avoid a broad redesign. |
| Lyrics | Local lyrics editor/import plus LRC and TTML timed parsing. LRC offset metadata is supported and tested. | Validate real lyric files, provider/network failures, malformed XML, and synchronized seeking on a device. |
| M3U playlists | Import/export flows are present; export filenames now preserve Unicode and remove filesystem-reserved characters. | Test malformed files, duplicate/missing tracks, Unicode titles/paths, empty playlists, and round-trip export/import. |
| Multi-select | Selection state exists in the current screen code. | Audit every bulk action for selection cleanup, cancellation safety, and accurate feedback. |
| Recent listening | A local recent-track list is persisted (up to 30 IDs). | A richer listening recap/play-count view could be a future optional feature, with clear local-data erase controls. |
| Android Auto | Media3 media-library service and automotive descriptor are declared. | Verify discoverability and playback in Android Auto / Desktop Head Unit; don't change declarations without evidence of a real gap. |
| Home-screen widget | Resizable playback widget and receiver are present. It now exposes previous, play/pause, next, shuffle, and repeat actions, with active shuffle/repeat state reflected in the widget. | Verify on physical launchers, state refresh after process/service recreation, and very narrow widget sizes. |

## Changes made from this comparison

- Added a dedicated timed-lyrics parser that supports existing LRC timestamps, multiple timestamps on one line, common TTML timed paragraphs, nested span text, and LRC [offset:...] metadata in milliseconds.
- Added regression tests for LRC timestamps/offsets, TTML paragraphs with nested spans, seconds-based timing, and malformed/untimed input.
- Hardened M3U export filename handling so Unicode playlist names are preserved and reserved filename characters are removed; added unit tests. Existing export flow and UI remain unchanged.
- Added a dedicated M3U serializer and regression tests so CR/LF characters inside track titles or artists cannot inject fake playlist entries; preserves Unicode and Android media URIs, and clamps negative durations to zero. The existing export UI and workflow remain unchanged.
- Kept the existing UI and lyric editing flow unchanged. Parsing support still needs real-device validation with actual lyric files.
- Added Shuffle and Repeat actions to the existing resizable home-screen widget. Widget state is refreshed from Media3 playback state, and active modes are visually highlighted; no new controls were added to other app screens.

## Recommended next steps

1. **Reliability first:** test playback, queue edge cases, widget state, equalizer persistence, timer cancellation, permission recovery, and headphone/Bluetooth interruptions.
2. **Playlist portability:** the export serializer now handles multiline metadata safely; next, test M3U import/export with malformed input, missing media, duplicates, and round-trip compatibility.
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
