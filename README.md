# Merge Seven

A hex-grid number merge game built for Android using Kotlin and Jetpack Compose.

## Architecture

This project strictly follows the architecture outlined in the Master Plan:
- **UI Layer**: Jetpack Compose, MVVM pattern
- **Domain Layer**: Pure Kotlin game engine and rules
- **Data Layer**: Room (local DB) and DataStore (preferences)
- **Dependency Injection**: Hilt

The game rules (merging, scoring, board validation) are entirely decoupled from the UI, making the core game engine easily testable.

## Build Instructions

1. Open this project in Android Studio (Jellyfish or newer recommended).
2. Sync Project with Gradle Files.
3. Select an emulator or physical device.
4. Click **Run 'app'**.

## Roadmap

See `Merge_Seven_Complete_Android_Development_Master_Plan.md` for the complete task list and development roadmap.
