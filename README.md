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

Use Android Studio (JDK 17 and Android SDK API 36) and Gradle 8.13. The repository currently has no committed Gradle wrapper JAR; to create one from a locally installed Gradle run:

```bash
gradle wrapper --gradle-version 8.13
./gradlew :app:assembleDebug
```

Or run `gradle :app:assembleDebug` if Gradle 8.13 is already installed. GitHub Actions CI uses `gradle/actions/setup-gradle` to fetch Gradle automatically and uploads a debug APK when successful.

To configure a release build with your own Google AdMob identifiers:

```bash
gradle :app:assembleRelease -PadmobAppId='ca-app-pub-XXXXXXXX~YYYYYYYY' -PadmobBannerId='ca-app-pub-XXXXXXXX/ZZZZZZZZ'
```

Development builds without production IDs use Google's **test ad unit**. Release builds without both production IDs display no ad banner. Real ads require an approved AdMob account and a configured Privacy & messaging consent message. Never click your own live ads.

## Security and privacy

Private health and meal logs are never deliberately sent to Google. However, **AdMob is not tracker-free**: Google collects advertising and network-related data independently under its SDK terms. The consent dialog appears before the SDK is initialized. See the [privacy notes](docs/PRIVACY.md). No account, remote health API, or proprietary backend is required for current personal-data functionality.

The future public food catalog must be a **second SQLite file** delivered by GitHub Releases with integrity and signature checks. Personal user data stays in `downsizing_private.db` and must never be overwritten by catalog updates.

## Release readiness

This is a first development foundation; device testing, a passing build, secure export/delete, a real privacy policy, Health Connect Play declarations and an app signing setup are still required before Google Play publication. Health and weight guidance in the app is informational, not medical advice.
