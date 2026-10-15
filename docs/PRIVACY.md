# Downsizing privacy notes — developer draft

**This is not the final Play Store privacy policy.** A separate published privacy policy, Data safety declaration and health-data disclosure review are required before distribution.

## Personal records
Country, name, date of birth, height, weight, goals, meal logs, activity, routines and settings are stored in `downsizing_private.db`. Existing settings are migrated from the original preferences into this database. No Downsizing account exists in this prototype. Health Connect is optional and read-only: the app displays steps, workouts, pulse and energy expenditure after Android grants access. None of these records may be supplied to the advertising module, ad targeting, analytics, or advertising identifiers.

The application's ad API accepts an Activity and a readiness callback only. It has no profile, database, or Health Connect input, and its requests have no custom targeting fields. The app requests ad consent after onboarding has completed. This is a code-level data-flow boundary, not an operating-system sandbox around the third-party SDK. Device traffic verification remains pending.

## Google advertisements
Google Mobile Ads (AdMob) and User Messaging Platform (UMP) are third-party libraries. Google says its advertising SDK can process IP addresses, device identifiers, product interactions, and diagnostics. That processing is separate from the app's private records. Even 'non-personalized' ads are not automatically anonymous. This prototype uses Google's official **test** AdMob IDs until the publisher configures real ones. A consent flow is requested before ads start, and without permission to request ads there is no banner.

Before release, review Google's user consent requirements for the EEA, UK and Switzerland; make sure the CMP message is configured in the publisher's AdMob console. Accurately declare SDK data processing and check app traffic with a proxy and on-device tests.

## Notifications
Daily check-in reminders are scheduled locally with WorkManager. They do not require our server or Firebase messaging.

## Food database
`downsizing_food.db` contains reference food tables only and is initially empty. The meal diary belongs in the private database, not the reference catalog. No download or upload is performed by this catalog foundation. A future GitHub catalog updater must serve reference food data only and must never upload or replace the private database.

## Before publication
Add local delete/export, Android permission-revocation handling, comprehensive accessibility tests, a final published privacy policy, and a truthful Play Store health declaration. Do not advertise the app as having zero third-party telemetry while using Google ads.
