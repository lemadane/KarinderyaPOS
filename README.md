# KarinderyaPOS

KarinderyaPOS is an Android point-of-sale app for canteens and karinderyas. It supports taking orders and managing a menu offline, with app data stored locally on the device in SQLite.

## Features

- Crew account registration, sign-in, and password changes
- Menu management with categories, availability, and dish photos
- The Order tab shows only dishes tagged as Meal of the Day; tag or untag dishes in the Menu editor. If none are tagged, the order menu is empty.
- Dine-in and take-out orders, discounts, notes, and cash, GCash, or Maya payments
- Manage reusable named 5%, 8%, 10%, 12%, 15%, 18%, and 20% discounts under More > Manage discounts. At checkout, select one discount: None, Senior, PWD, or a custom discount. Orders and receipts retain the applied name and rate even after a custom discount is edited or deleted. Backup export/import includes custom discounts.
- Opening Manage discounts requires the signed-in user's current password each time. Selecting an existing discount at checkout does not require re-authentication.
- Order status management and shareable receipts
- Sales reports with date ranges, payment breakdowns, and best sellers
- JSON backup export and import
- A 200-day offline trial in release builds; debug builds are trial-exempt

## Requirements

- JDK 21
- Android SDK Platform 36
- Android Studio or the included Gradle wrapper

## Build and test

Build a debug APK:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests:

```sh
./gradlew testDebugUnitTest
```

To install on a connected Android device or emulator:

```sh
./gradlew installDebug
```

## Google Play internal testing

Build the release Android App Bundle:

```sh
./gradlew testDebugUnitTest lintRelease bundleRelease
```

Release signing is loaded from `~/.local/share/karinderyapos-signing/upload.properties`,
or from the path in `KARINDERYA_SIGNING_PROPERTIES`. This private properties file must
contain `storeFile` (absolute keystore path), `storePassword`, `keyAlias`, and
`keyPassword`. Keep the keystore and passwords outside the project, restrict access,
and back them up securely. Never commit or share them. Without this file, the release
bundle is unsigned and is not ready for Play upload.

The bundle is written to `app/build/outputs/bundle/release/app-release.aab`.
The release uses package `ph.lemadane.karinderya.pos`, version name `1.1`, and version
code `2`. Increase the version code before each subsequent Play release.
Release builds retain the 200-day offline trial.

In Play Console, select the app, open **Testing > Internal testing**, create a
release, enroll in Play App Signing, and upload the signed AAB. Add release notes,
resolve any Console requirements, and roll out to the internal testing track.
Add testers and share the opt-in link; this does not publish the app publicly.

Export an in-app JSON backup before replacing the locally installed debug app.
The Play-distributed app normally has a different signing certificate and cannot
update that debug installation in place. Do not uninstall until the backup is safe.
Password recovery is not yet implemented; testers should retain their account
passwords. The beta has build/unit-test validation but still needs device testing.

## Project structure

- `app/src/main/java/ph/karinderya/pos/ui` — authentication, point-of-sale, menu, orders, and reports screens
- `app/src/main/java/ph/karinderya/pos/data` — SQLite database, repositories, models, session, and settings
- `app/src/main/java/ph/karinderya/pos/trial` — offline trial handling
- `app/src/main/java/ph/karinderya/pos/util` — money, receipt, image, and JSON utilities
- `app/src/test` — local unit tests

The app is a native Java Android application using AndroidX, Material Components, View Binding, and SQLite. No network connection is required for normal operation.
