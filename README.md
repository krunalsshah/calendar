# My Calendar
Simple event calendar, with agenda view.

## Requirements
* Android SDK 23
* Android SDK Tools 25.0.8
* Android SDK Build-tools 23.0.2
* Android Support Library 23.2

## Build & Test

**Build**

    ./gradlew :app:assembleDebug

**Test & Coverage**

    ./gradlew :app:lintDebug
    ./gradlew :app:testDebug
    ./gradlew :app:jacocoTestCoverage

## Discussions:

Choice of widget:

* `android.widget.CalendarView`:
  * zero efforts
  * no event indicator
  * no month change listener
  * no state restoration
  * inconsistent look and feel across API levels