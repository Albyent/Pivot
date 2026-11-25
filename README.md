# Pivot

Pivot is an offline Android habit tracker with a dashboard heatmap, dated notes, habit analytics, and a separate tab for one-time tasks. It stores data in a Room database on your phone.

## Install

Copy `Pivot.apk` to an Android 8.0+ phone and open it to install. This APK is signed for local installation.

## Build

Use a JDK 17 or newer and Android SDK Platform 35. Set `ANDROID_HOME` to your Android SDK location or open this project in Android Studio.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The signed APK uses a separate local release key so future release updates can preserve the on-device database.

## Using Pivot

- In the habit editor, pick a color or search for an icon. Open **Settings → App theme** to set the overall accent.
- Tap a habit’s check button to log today. Hold a completed checkmark to clear the check-in. Tap a heatmap cell to view or edit a dated entry.
- Open a habit and use its options menu to edit, skip, pause, reorder, or archive it. Hold a pending check button for quick access to check-in options.
- Weekly goals count distinct completed days from Monday through Sunday. Amount goals can use quick-add increments. Notes belong to a habit and a date.
- Use Settings to save or restore a local JSON backup. Restoring replaces the current data after confirmation.
- Add the Pivot widget from Android’s widget picker or from Pivot’s settings. Reminders use Android notifications and may arrive later than their selected time if Android defers alarms.
