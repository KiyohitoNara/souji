# AGENTS.md

## Project Overview

**Souji** is an Android application that helps users keep their notification shade clean by automatically cancelling or filtering notifications from selected apps.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (ViewModel + Repository pattern)
- **Dependency Injection**: Dagger Hilt
- **Build System**: Gradle (Android Gradle Plugin)
- **Format**: ktlint (via `org.jlleitschuh.gradle.ktlint`)
- **Lint**: ktlint (via `org.jlleitschuh.gradle.ktlint`) / Android Lint (via Android Gradle Plugin)
- **Test**: JUnit (unit tests) / Espresso (instrumented tests)

## Development Workflow

### Branch Strategy

This project follows GitLab Flow.

### Development Commands

**Build:**
```bash
./gradlew assembleDebug
```

**Run:**
```bash
./gradlew installDebug
```

**Format:**
```bash
./gradlew ktlintFormat
```

**Lint:**
```bash
./gradlew ktlintCheck
./gradlew lintDebug
```

**Test:**
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Project Structure

Main source under `app/src/main/java/io/github/kiyohitonara/souji/`:

- `data/`: Repository / data source implementations
- `model/`: Data models
- `ui/`: Compose screens and view models
