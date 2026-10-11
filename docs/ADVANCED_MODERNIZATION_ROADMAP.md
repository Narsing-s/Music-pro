# Music Pro — Advanced Modernization Roadmap

This roadmap is designed to make Music Pro more capable without replacing the current interface or disrupting working playback. Treat every item as a small, testable change—not a redesign mandate.

## Product principles

- Keep the existing Home, library, full-player, queue, settings, and mini-player flows recognizable.
- Keep shuffle and repeat in context; do not duplicate them as extra Home panels.
- Local playback must continue to work without internet access or an account.
- No bundled copyrighted music, scraping, or claims that online catalogues are available.
- New features must be optional where they change interaction or consume extra resources.
- Do not claim a feature is complete until unit tests/builds pass and the relevant device checks are performed.

## Priority 0 — reliability before new features

| Area | Improvement | Acceptance criteria |
| --- | --- | --- |
| Media permissions | Recover gracefully after permission is denied, revoked, or granted again in Android Settings. | No crash; clear empty/permission state; rescan works after permission is restored. |
| Playback lifecycle | Validate audio focus, calls/interruptions, headset unplug, Bluetooth route changes, screen-off playback, and notification controls. | Playback state stays consistent between app, notification, lock screen, and widgets. |
| Queue integrity | Cover moving/removing the active item, first/last item, single-item queue, and clearing the queue. | No invalid index, duplicate unexpected track, or stale now-playing state. |
| Library edge cases | Cover empty library, unknown artist/album, deleted files, duplicate titles, and unreadable artwork. | Stable list keys; usable fallbacks; missing media never crashes playback. |
| Persistence | Reopen the app and recreate the playback service after changing appearance, playback speed, equalizer, and repeat/shuffle settings. | Settings either persist accurately or are explicitly documented as session-only. |
| Lyrics | Test malformed LRC/TTML, offsets, multiple timestamps, seeking, missing lyrics, and large files. | Parser failures are contained; lyrics remain aligned after seek where supported. |
| Playlists | Test empty, malformed, duplicate, missing-track, Unicode, and newline-containing M3U entries. | Import/export remains safe; no fake entries from embedded newlines; failures are explained. |

## Priority 1 — modern polish with low regression risk

1. **Artwork resilience:** consistent fallback artwork for missing, blocked, or deleted artwork URIs; avoid layout jumps while artwork loads.
2. **Accessible controls:** meaningful content descriptions, minimum touch targets, visible focus states, and readable contrast in dark/light/custom themes.
3. **Responsive player layouts:** verify portrait, landscape, compact screens, large font sizes, and display cutouts. Keep controls visible and the artwork from shrinking into a tiny thumbnail.
4. **Feedback states:** use concise feedback for playlist import/export, metadata saves, unavailable tracks, and destructive actions. Confirm before irreversible clears; provide Cancel where appropriate.
5. **Search and sorting:** preserve query and sort state when navigating back; make empty search results distinct from a permission-denied or empty library.
6. **Widget quality:** verify both widget variants at minimum and maximum sizes, on multiple launchers, after process recreation, and with long Telugu/Unicode titles.
7. **Performance:** profile library scans, artwork loading, long queues, and large playlists on a mid-range device; avoid blocking the main thread.
8. **Theme quality:** test system dark/light mode, custom colors, contrast, status/navigation bars, and dialogs consistently.

## Priority 2 — optional advanced features

These should be implemented only after a feature-specific design and test plan exists.

- **Artwork-focused widget:** a third optional widget provider; keep both existing widget choices unchanged.
- **Queue persistence and recovery:** restore a useful queue after process death only if media URIs are still valid; handle removed files safely.
- **Backup and restore:** explicit user-triggered export/import of app settings, playlists, and app-local metadata. Exclude audio files and secrets by default; preview and validate imports.
- **Listening insights export:** optional local CSV/JSON export with a clear privacy warning and a separate clear-history action.
- **Accessibility and localization audit:** screen-reader navigation, scalable typography, Telugu/English labels, and no clipped translated strings.
- **Optional player gestures:** only as a setting, with existing buttons retained and gestures never hijacking seek or queue interactions.

## Release gate

Before publishing a release:

- [ ] Run unit tests, Android lint, and the debug APK build (`gradle testDebugUnitTest lintDebug assembleDebug` in CI); review the generated lint report.
- [ ] Install the APK on a physical Android device and launch it from a clean install.
- [ ] Test local Telugu songs, devotional tracks, and files with missing or unusual metadata.
- [ ] Test playback while the screen is off and control it from the notification and lock screen.
- [ ] Test shuffle, repeat, queue reorder/remove, playlists, lyrics, and the sleep timer.
- [ ] Rotate the device and test landscape player layout, system bars, and large text.
- [ ] Test both widgets at their smallest supported sizes.
- [ ] Confirm that offline local playback still works with network disabled.
- [ ] Inspect logs for crashes and permission or media-session errors.
- [ ] Verify APK signing/update compatibility before describing a build as an upgrade.

## Explicitly out of scope unless separately requested

- Replacing the current UI wholesale.
- Adding duplicate playback controls to Home.
- Sending listening history to a server or adding telemetry by default.
- Adding online music catalogue claims without a licensed, reliable source.
- Copying another app's code, artwork, branding, or distinctive layout.

## Status

The CI workflow now runs unit tests, Android lint, and the debug APK build, and uploads the lint HTML report when generated. This document remains a prioritized engineering plan, not a claim that every listed improvement has been implemented. Mark items complete only after code review, automated tests, and the relevant device checks.
