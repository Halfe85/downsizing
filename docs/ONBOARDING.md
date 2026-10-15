# Storage and first-run setup

`downsizing_private.db` version 2 stores the profile (country code, name, ISO date of birth, height in cm), weight history, meal history, activity/routines, goals and app settings. The version 1 migration preserves existing log tables and imports old preference values. Old preference keys are removed only after the database opens successfully.

`downsizing_food.db` version 1 stores reference foods (country, name, barcode, kcal per 100 g, source). It starts empty. No Norwegian food dataset, downloader or catalog search is included yet. Deleting/replacing the catalog must never delete or overwrite the private database.

Startup uses the [Android splash-screen API](https://developer.android.com/develop/ui/views/launch/splash-screen) while local storage opens. If opening fails, a retry screen is shown without erasing data. New users see a modal with these steps:

1. Choose Norway, displayed with its flag. This is the only available country.
2. Enter a name.
3. Enter date of birth as DD.MM.YYYY.
4. Enter height in cm and current weight in kg.
5. Enter a weight goal in kg, then press Next to save and open home.

Each step has a Next button, with Back from step 2 onward. Invalid entries cannot advance. Both commas and periods are accepted for metric decimal values. Draft input survives Android saved-state restoration. Leaving before completion writes no profile. Saving the profile, initial weight and goal is one transaction; failed saves retain the form for retry. Duplicate completion does not add another starting weight. Later launches skip the modal when a profile exists. Existing installs without a profile see the modal while retaining their previous logs.

The full app, privacy screen and splash use white/turquoise in light mode and dark grey/turquoise in system dark mode. The home layout remains provisional. No manual theme override is added.

The advertising implementation receives none of the profile or health values, country selection, meal history, or goals. Its consent request is made only after home is reached. Ad SDK network behavior still needs verification on a device; source-level separation does not isolate the SDK in a separate process.

Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` before promoting source to main. The automated tests exercise real SQLite under Robolectric, v1 migration, catalog replacement isolation, atomic save failure/retry, duplicate completion, strict dates, metric parsing, Compose step navigation/validation, state restoration and system theme selection. They do not replace Samsung device testing.
