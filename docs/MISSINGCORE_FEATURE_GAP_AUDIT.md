# Music Pro — MissingCore Feature Gap Audit

Reviewed against the public [MissingCore/Music feature list](https://github.com/MissingCore/Music) and its recent release notes on 2026-10-10. This is a compatibility audit, not a request to copy the other project's source, branding, artwork, or layout.

## Guardrails

- Preserve the current Music Pro screens, navigation, colors, playback controls, queue behavior, and settings.
- Prefer additions behind existing screens or as optional settings; no new home-screen promotion panels.
- Do not add network lyric providers, telemetry, or permissions merely to match another player.
- MissingCore/Music declares AGPL-3.0. Do not transplant its code or assets without a deliberate license review.

## What Music Pro already has

| Capability | Current Music Pro evidence | Decision |
| --- | --- | --- |
| Local tracks, albums, artists, genres, playlists, favorites | Existing MediaStore repository and Compose library screens | Keep; prioritize edge-case tests |
| Background playback and notification controls | Media3 PlaybackService and media session | Keep; verify on real devices |
| Shuffle and repeat | Full player controls; now also available in the home widget | No duplicate controls in Home |
| Queue management | Existing queue view and move/remove actions | Add regression tests for edge cases |
| Local metadata overrides | Track title, artist, and album overrides stored in app preferences | Keep app-local; don't claim to rewrite audio files |
| Custom appearance | Dark/light/custom theme options and font preferences | Preserve current appearance |
| Lyrics | Local lyric editor/import, LRC offsets, TTML/LRC parsing and unit tests | Validate with real files and seeking |
| M3U portability | Import/export, Unicode-safe filenames, metadata newline sanitization | Test round trips and malformed files |
| Equalizer, playback speed, sleep timer | Existing controls/settings | Verify persistence, cancellation, and service recreation |
| Android Auto foundation | Media3 media-library service and automotive descriptor | Verify on compatible hardware before changing service declarations |
| Home-screen controls | Existing resizable widget with previous, play/pause, next, shuffle, repeat | Verify narrow sizes and refresh behavior |

## Features present in MissingCore that are not confirmed as equivalent in Music Pro

These are **candidate gaps**, not instructions to change the current UI. Do them only as opt-in additions with tests and no regression to existing behavior.

1. **Multiple widget layouts and appearance options.** Music Pro currently has one resizable widget. A compact 2×1 and an artwork-focused layout could be added as separate launcher choices without changing in-app screens. Keep the current widget available.
2. **Listening recaps and play-count insights.** Add only as a new, clearly labeled optional destination, with local-only storage and a clear-history action. Do not silently start tracking extra history.
3. **Album-level metadata editing.** Music Pro currently overrides metadata per track. Album name/artist/year/genre overrides and EP/single grouping would need a data model and migration plan; do not implement by mutating original files.
4. **Optional player gestures and artwork styles.** Swipe next/previous, tap artwork to toggle playback, and vinyl/waveform presentation are interaction/design choices. They must be opt-in and must not replace existing buttons or seek behavior.
5. **Media format and metadata edge cases.** Broaden format/metadata coverage only after checking Android's MediaStore and Media3 support for the actual formats. Never advertise theoretical support as verified support.

## Safe implementation order

1. **Now:** keep the completed widget shuffle/repeat controls, M3U hardening, and lyric parsing; run CI and record real-device checks separately.
2. **Next, isolated:** add a second widget layout/provider using the same Media3 session, leaving the existing widget untouched.
3. **After that:** add automated play-history/recap logic before any new UI; make collection and clearing behavior explicit.
4. **Later:** design album-level metadata overrides with tests and migration coverage before exposing editor actions.
5. **Only by opt-in:** gesture and artwork presentation preferences.

## Verification checklist

- [ ] GitHub Actions passes unit tests and debug APK build.
- [ ] Existing Now Playing, Home, navigation, queue, and preferences behave exactly as before.
- [ ] Widget actions work while app is backgrounded; shuffle/repeat state refreshes after service recreation.
- [ ] Lyrics and M3U round trips tested with real files, Unicode, malformed input, missing tracks, and empty playlists.
- [ ] Audio focus, Bluetooth/headphone changes, screen-off playback, and Android Auto checked on hardware where available.
- [ ] No new permissions, network dependencies, analytics, or copied code/assets without a clear reason.

## References

- [MissingCore/Music README and feature list](https://github.com/MissingCore/Music)
- [MissingCore/Music recent release notes](https://github.com/MissingCore/Music/releases)
- [Music Pro source](https://github.com/Narsing-s/Music-pro)
