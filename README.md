# Kotlin Quiz Master

A stand-alone Android quiz app for beginner Kotlin learners (SOFT708 Assignment 1).
Instructors create one 10-question quiz per level, students take the quizzes (pass mark 8/10), and each level unlocks after the previous one is passed.

## Requirements

- Android Studio (latest stable)
- Android SDK Platform 37
- An emulator or device with Google Play services (API 24 or later)

## Setup

1. Clone the repository and open the project in Android Studio.
2. Add your Google Maps API key to `local.properties` in the project root:

   ```properties
   MAPS_API_KEY=your_api_key_here
   ```

   Get a key from the [Google Cloud Console](https://console.cloud.google.com/) with **Maps SDK for Android** enabled.
   `local.properties` is git-ignored, so the key is never committed.
3. Sync Gradle and run the `app` configuration.

Without a key the app still runs, but the map on the Contact Us screen stays blank.

## Demo accounts

Created automatically on first launch.

| Role       | Email                | Password       |
|------------|----------------------|----------------|
| Admin      | admin@ais.ac.nz      | Admin123!      |
| Instructor | instructor@ais.ac.nz | Instructor123! |
| Student    | student@ais.ac.nz    | Student123!    |
