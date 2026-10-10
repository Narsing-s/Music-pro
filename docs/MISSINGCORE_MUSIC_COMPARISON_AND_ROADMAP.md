# Music Pro — Feature Comparison & Safe Roadmap

This review compares Music Pro with [MissingCore/Music](https://github.com/MissingCore/Music), a separate local Android music player. It is a feature-gap review, not a request to copy its interface or source code.

## Preserve these boundaries

- Keep Music Pro's current Jetpack Compose interface, navigation, playback flow, and existing behavior unchanged.
- Do not copy source code, assets, strings, or UI from MissingCore/Music. Its repository declares the **AGPL-3.0** license; any code reuse would need a deliberate license-compatibility review and required compliance. Prefer independently implemented ideas.
- Add new capabilities only as optional features that fit the existing app, with tests and a real-device check before release.

## Initial feature comparison

| MissingCore/Music feature | Music Pro review | Safe next step |
| --- | --- | --- |
| Local library: tracks, albums, artists, genres, favorites, playlists | Music Pro already has library browsing, favorites, playlists, and genre-related code. | Keep existing behavior; test library consistency and empty states. |
| Background playback and notification controls | Music Pro uses Media3 PlaybackService. | Regression-test screen-off playback, notification controls, and audio interruptions. |
| Queue removal and reordering | Music Pro documents queue management. | Test reorder/remove behavior and ensure the active track remains correct. |
| Equalizer | Music Pro has equalizer controls and recent persistence fixes. | Validate saved curves after service/session recreation and across app restarts. |
| Sleep timer | Timer-related implementation exists in Music Pro source. | Verify end-to-end behavior before considering any redesign; do not duplicate the control. |
| Lyrics (embedded, synchronized, provider-based) | Lyrics-related references exist, but full feature parity is not established by this quick source scan. | Inspect current implementation before adding providers or synchronized lyrics. |
| M3U playlist import/export | M3U-related references exist, but parity and edge-case coverage are not established. | Test import/export, missing files, Unicode paths, and duplicate entries before expanding. |
| Multi-select actions | No clear multi-select implementation was found in the initial source scan. | Candidate feature: optional bulk selection for queue/library actions, only after confirming demand and designing it to fit existing UI. |
| Local metadata editor | No clear dedicated metadata-editor implementation was found in the initial source scan. | Candidate feature: edit app-local display metadata first; never silently rewrite the underlying audio file. |
| Play history, play counts, listening recaps | No clear play-count/recap implementation was found in the initial source scan. | Candidate feature: privacy-friendly local listening history with transparent reset/clear controls. |
| Custom artwork and extensive theme/font customization | Music Pro has appearance options, but full parity was not established. | Defer until current theme and artwork behavior are fully documented; avoid a broad UI redesign. |
| Android Auto | Music Pro already declares a Media3 media-library service and automotive descriptor. | Treat as a compatibility verification task first; test on Android Auto or Desktop Head Unit before adding manifest changes. |
| Additional home-screen widget variants | Music Pro already has a home-screen playback widget. | Test the current widget thoroughly before considering more variants. |

## Recommended order

1. **Reliability first:** test playback service, widget state, equalizer persistence, sleep timer, permission recovery, and queue edge cases.
2. **Playlist portability:** verify M3U import/export and add regression tests for malformed files, missing tracks, and Unicode paths if those flows are supported.
3. **Optional library productivity:** consider multi-select and app-local metadata editing only after confirming the existing UI can accommodate them without clutter.
4. **Listening insights:** consider an opt-in, on-device play history/recap feature; keep data local and provide a clear way to erase it.
5. **Automotive compatibility:** verify the existing Media3 integration before implementing anything new.

## Acceptance criteria for future additions

- No regressions to current screens, controls, queue behavior, playback, or saved settings.
- New features are optional and do not add duplicate buttons or unexplained blank space.
- Automated tests cover business logic and edge cases.
- GitHub Actions passes unit tests and builds the debug APK.
- Real-device checks are recorded separately; CI success alone is not proof of device compatibility.

## Reference

- [MissingCore/Music repository and feature list](https://github.com/MissingCore/Music)
- [MissingCore/Music license](https://github.com/MissingCore/Music/blob/dev/LICENSE)
- [Music Pro repository](https://github.com/Narsing-s/Music-pro)

This document records candidates for investigation. It does not claim every listed feature is absent or fully implemented in Music Pro; each candidate should be verified against actual behavior before code changes are made.
