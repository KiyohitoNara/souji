# AGENTS.md

## Project Overview
**Souji** is an Android application that helps users keep their notification shade clean by automatically cancelling or filtering notifications from selected apps.

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (ViewModel + Repository pattern)
- **Dependency Injection**: Dagger Hilt
- **Build System**: Gradle (Android Gradle Plugin)

## Development Workflow

**Build:**
```bash
./gradlew assembleDebug
```

**Test:**
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```
