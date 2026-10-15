# Downsizing privacy notes — developer draft

**This is not the final Play Store privacy policy.** A separate published privacy policy, Data safety declaration and health-data disclosure review are required before distribution.

## Personal records
Weight, goals, meal logs, activity and routines are stored in a private local SQLite file and app settings. No Downsizing account exists in this prototype. Health Connect is optional and read-only: the app displays steps, workouts, pulse and energy expenditure after Android grants access. None of these records are deliberately supplied to the advertising module.

## Google advertisements
Google Mobile Ads (AdMob) and User Messaging Platform (UMP) are third-party libraries. Google says its advertising SDK can process IP addresses, device identifiers, product interactions, and diagnostics. That processing is separate from the app's private records. Even 'non-personalized' ads are not automatically anonymous. This prototype uses Google's official **test** AdMob IDs until the publisher configures real ones. A consent flow is requested before ads start, and without permission to request ads there is no banner.

Before release, review Google's user consent requirements for the EEA, UK and Switzerland; make sure the CMP message is configured in the publisher's AdMob console. Accurately declare SDK data processing and check app traffic with a proxy and on-device tests.

## Notifications
Daily check-in reminders are scheduled locally with WorkManager. They do not require our server or Firebase messaging.

## Future GitHub food database
GitHub must serve reference food data only. The user-specific SQLite database must remain a separate file and never be uploaded as part of update requests.

## Before publication
Add local delete/export, Android permission-revocation handling, comprehensive accessibility tests, a final published privacy policy, and a truthful Play Store health declaration. Do not advertise the app as having zero third-party telemetry while using Google ads.
