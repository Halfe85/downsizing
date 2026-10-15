# Development, promotion and phone builds

The existing VS Code workspace stays unchanged. These folders are separate Git clones:

| Folder | Branch | Purpose |
| --- | --- | --- |
| `/home/halfe/Prosjekter/downsizing/development branch` | `feature/android-foundation` | Develop and validate changes |
| `/home/halfe/Prosjekter/downsizing/main` | `main` | Receive validated source and build APKs |

Make app changes in development first. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` there and review the results before committing. Build/lint checks do not prove behavior on a phone. Record any device testing separately.

For GitHub promotion, push the development branch, open a pull request into `main`, and require a passing Android build before merging. Pull `main` with `git pull --ff-only`, then build its APK. APKs are ignored build outputs; source code belongs on both branches. GitHub Actions publishes downloadable APK artifacts for pull requests and pushes to `main`.

For a local promotion before publishing, first ensure both checkouts are clean and on their intended branches. From the `main` folder:

```bash
git fetch '../development branch' feature/android-foundation
git merge --ff-only FETCH_HEAD
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

If the fast-forward is refused, review the divergence and integrate it in development before retrying. Local promotion does not update GitHub. Keep remote changes synchronized before publishing.

The main build produces `main/app/build/outputs/apk/debug/app-debug.apk` relative to the parent `downsizing` folder. This debug APK is for testing, not Google Play publication.

## Later: Samsung phone testing

The app requires Android 9 (API 28) or later. When the phone is ready, enable USB debugging and authorize this computer. From `main`, inspect `adb devices -l`, then install onto the intended device:

```bash
adb -s PHONE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Use the same debug signing key for subsequent updates; a different signing key cannot replace the existing installation. Do not uninstall to fix a signature mismatch without preserving any personal data first.

Check launch, navigation, saving and deleting sample food/activity/weight entries, persistence after restart, routine toggles, notification permission handling, and optional Health Connect permissions and reads. Confirm only test ads appear. Test denied permissions as well as granted permissions. Health Connect and Samsung Health synchronization must be verified on the actual phone.

Phone testing is deferred until the device is available. A successful APK build is not a device test or production release approval.
