# Downsizing

**Downsizing** is a local-first Android weight, nutrition, movement and habit tracking application. Advertising revenue is intended to fund future UtgardNet development, with a later option to replace Google ads with the UtgardNet advertising platform.

## Android prototype 0.1.0

Available functionality:
- Record weight and target weight, view recent weight change.
- Log food and calorie intake locally, edit the daily energy-expenditure estimate.
- Log activities (name, duration and estimated calories burned) and daily routine checklists.
- Read steps, latest pulse, exercise sessions, and total calories burned from Android Health Connect when approved.
- Galaxy Watch and compatible watches via Samsung Health/other applications that sync to Health Connect.
- Opt-in daily local notifications.
- AdMob banner with UMP privacy choices and **official test advertisements**, not live revenue.
- SQLite-based user data stored solely on the phone.

Not included yet: food product lookup, downloadable SQLite food catalog, barcode scanning, Android step-sensor fallback where Health Connect is unavailable, actual live workout recording, true remote push messages, smartwatch companion, encrypted cloud backup, and reliable statistical weight prediction. These are on the [roadmap](docs/ROADMAP.md).

## Build and test

Use JDK 17 or 21 and Android SDK API 36. The committed Gradle wrapper downloads the pinned Gradle 8.13 distribution and verifies its SHA-256 checksum. Set `ANDROID_HOME` to your SDK directory or configure `sdk.dir` in an untracked `local.properties` file.

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the same build and lint checks and uploads the debug APK when successful. See [the development and main workflow](docs/WORKFLOW.md) for branch promotion and later Samsung phone testing.

To configure a release build with your own Google AdMob identifiers:

```bash
./gradlew :app:assembleRelease -PadmobAppId='ca-app-pub-XXXXXXXX~YYYYYYYY' -PadmobBannerId='ca-app-pub-XXXXXXXX/ZZZZZZZZ'
```

Development builds without production IDs use Google's **test ad unit**. Release builds without both production IDs display no ad banner. Real ads require an approved AdMob account and a configured Privacy & messaging consent message. Never click your own live ads.

## Security and privacy

Private health and meal logs are never deliberately sent to Google. However, **AdMob is not tracker-free**: Google collects advertising and network-related data independently under its SDK terms. The consent dialog appears before the SDK is initialized. See the [privacy notes](docs/PRIVACY.md). No account, remote health API, or proprietary backend is required for current personal-data functionality.

The future public food catalog must be a **second SQLite file** delivered by GitHub Releases with integrity and signature checks. Personal user data stays in `downsizing_private.db` and must never be overwritten by catalog updates.

## Release readiness

This is a first development foundation. A local debug build and lint check passed on 2026-10-15 (0 lint errors, 31 warnings). Device testing, verified GitHub CI, secure export/delete, a real privacy policy, Health Connect Play declarations and release signing are still required before Google Play publication. Health and weight guidance in the app is informational, not medical advice.
