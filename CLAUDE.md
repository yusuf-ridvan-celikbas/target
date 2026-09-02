# Target — Offline Exam Prep Tracker

## What this app is

A personal, fully offline Android app for organizing exam study materials and tracking study progress. Not tied to any specific exam — the user defines whatever exam(s) they're preparing for, then organizes materials underneath.

Hierarchy: **Exam → Topic → Resource**

- **Exam**: something the user is preparing for (name, optional target date).
- **Topic**: a subject/chapter within an Exam (name, ordering, status, last-studied date).
- **Resource**: study material attached to a Topic. v1 supports two types:
  - **Note** — text typed directly in the app.
  - **File** — any file imported via Android's Storage Access Framework (SAF), copied into app-private storage on import (so it survives even if the original file is moved/deleted on the phone). Opened via an `ACTION_VIEW` intent to whatever app the user already has for that file type.

Progress tracking: each Topic has a status (`Not Started` / `In Progress` / `Done`) and a last-studied timestamp. Each Exam shows a completion percentage rolled up from its Topics' statuses. A dashboard screen lists all Exams with their completion %.

## Explicitly out of scope for v1 (do not build unless asked)

- Interactive quizzing / self-testing with scored attempts. Question-bank resources are just Note or File resources in v1 — no answer-checking, no scoring, no spaced repetition yet.
- Search, tags, local notifications/reminders.
- Backup/export/restore.
- Any form of sync, accounts, or network access of any kind.

These are candidate v2+ features. Don't scaffold data model fields or UI for them speculatively — add them when explicitly requested.

## Hard constraint: this app must stay fully offline

- No `INTERNET` permission in the manifest. No network calls, no analytics SDKs, no crash reporting services that phone home. If a dependency wants network access, flag it before adding it.
- All data lives on-device only, in a local Room database plus app-private file storage for imported Resource files.

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Persistence**: Room (SQLite) for Exam/Topic/Resource/progress data
- **Architecture**: MVVM — Composable screens + ViewModel (StateFlow) per screen area
- **Navigation**: Jetpack Navigation Compose. Screen flow: Exam List (dashboard) → Exam Detail (Topic list) → Topic Detail (Resource list)
- **File import**: `ACTION_OPEN_DOCUMENT` (SAF) to pick files; copy bytes into the app's private external files dir on import rather than relying on a persisted URI permission

Target device is a single personal phone (Samsung, One UI 8.5, Android 16) — this app is not intended for distribution, so don't add back-compat workarounds for older Android versions unless asked. Check `app/build.gradle.kts` for the actual configured `minSdk`/`targetSdk` rather than assuming a value here.

## Project conventions

- Package id: `com.ridvan.target`
- Build/run: `./gradlew assembleDebug` to build, `./gradlew installDebug` to install to the connected device (or use Android Studio's Run button / VS Code equivalent). Deploy target is the developer's own phone over adb (USB or wireless debugging) — no emulator setup exists or is needed.
- Git: commit after each feature is working and tested on-device, with a descriptive commit message. Don't batch unrelated features into one commit.
- Editor: development happens primarily in VS Code with the Claude Code CLI; Android Studio is used occasionally just for SDK management / Gradle sync, not as the primary editor.

## Current status

Project scaffolded (Empty Activity / Compose template), pushed to GitHub, confirmed building and running on the developer's phone. Data model (Room entities for Exam/Topic/Resource/progress) has not been implemented yet — that's the next feature to build.
