# Contributing to Narsing Music

Thanks for helping improve Narsing Music.

## Before making a change

- Check existing issues and open pull requests to avoid duplicate work.
- Keep changes focused and preserve existing playback behavior unless the change specifically requires otherwise.
- Do not add analytics, upload music, or transmit listening history without a clear, documented user choice.
- Never commit signing keys, passwords, access tokens, local device data, or private music files.

## Development

- Use JDK 17 and Android SDK 35.
- Open the project in Android Studio or build from the repository root:

  ```bash
  gradle testDebugUnitTest assembleDebug
  ```

- Test on a physical Android device when possible, especially media permission flows, screen-off playback, notification controls, queue changes, and Android-version-specific behavior.
- Include the Android version and device model when reporting device-specific issues.

## Pull requests

Please include:
1. A short summary of the user-visible change.
2. The reason for the change.
3. Tests or manual checks performed, including anything not tested.
4. Screenshots or a short recording for meaningful UI changes.

Keep pull requests small and avoid unrelated refactors. CI must pass before a change is considered ready.

## Reporting bugs and requesting features

Use the repository's GitHub issue templates. Include reproduction steps and expected versus actual behavior. Do not attach private audio files or personal data.
