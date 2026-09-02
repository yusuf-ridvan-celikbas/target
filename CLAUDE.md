# Target — Offline Exam Prep Tracker

## What this app is

A personal, fully offline Android app for organizing exam study materials and tracking study progress. Not tied to any specific exam — the user defines whatever exam(s) they're preparing for, then organizes materials underneath.

Hierarchy: **Exam → Topic (nestable) → Resource**

- **Exam**: something the user is preparing for — name, exam date (`targetDate`), study-start date, exam location, registration open/close dates. All optional except name.
- **Topic**: a subject/chapter/sub-chapter within an Exam (name, ordering, status, last-studied date). Topics can nest to any depth via an optional `parentTopicId` — e.g. an exam's YKS → Chemistry → TYT → "Topic 1" is just four Topic rows nested three deep. This is deliberate: the schema doesn't hardcode any specific exam's structure (like YKS's TYT/AYT split); depth and shape are whatever the user builds.
  - A Topic can carry question-bank stats (`testCount`, `questionCount`) and a study goal (`goalStartDate`, `goalEndDate`, `dailyQuestionTarget`) for the material it directly holds.
  - **Daily logs**: one row per Topic per day (questions solved, minutes spent), keyed uniquely on (topic, date) — re-logging the same day overwrites rather than duplicating. Whether a day "met the goal" is always derived by comparing that day's solved count to `dailyQuestionTarget`, never stored as its own field — there is no separate outcome enum to drift out of sync with the actual number.
- **Resource**: study material attached to a Topic. v1 supports two types:
  - **Note** — text typed directly in the app.
  - **File** — any file imported via Android's Storage Access Framework (SAF), copied into app-private storage on import (so it survives even if the original file is moved/deleted on the phone). Opened via an `ACTION_VIEW` intent to whatever app the user already has for that file type.

Progress tracking: each Topic has a status (`Not Started` / `In Progress` / `Done`) and a last-studied timestamp. Each Exam shows a completion percentage rolled up from its **leaf** Topics' statuses only (a Topic that just holds sub-topics isn't itself counted). A dashboard screen lists all Exams with their completion %.

Reminders (e.g. for registration deadlines or daily study goals) are a planned future addition — not built yet, don't scaffold for them speculatively.

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
- **Persistence**: Room (SQLite) for Exam/Topic/Resource/DailyLog data. Schema changes go through a real `Migration` (see `data/local/Migrations.kt`) — `fallbackToDestructiveMigration()` is deliberately not used, since there's real on-device study data to preserve across schema changes.
- **Architecture**: MVVM — Composable screens + ViewModel (StateFlow) per screen area
- **Navigation**: Jetpack Navigation Compose. Screen flow: Exam List (dashboard) → Exam Detail (Topic list) → Topic Detail (Resource list)
- **File import**: `ACTION_OPEN_DOCUMENT` (SAF) to pick files; copy bytes into the app's private external files dir on import rather than relying on a persisted URI permission

Target device is a single personal phone (Samsung, One UI 8.5, Android 16) — this app is not intended for distribution, so don't add back-compat workarounds for older Android versions unless asked. Check `app/build.gradle.kts` for the actual configured `minSdk`/`targetSdk` rather than assuming a value here.

## Project conventions

- Package id: `com.ridvan.target`
- Build/run: `./gradlew assembleDebug` to build, `./gradlew installDebug` to install to the connected device (or use Android Studio's Run button / VS Code equivalent). Deploy target is the developer's own phone over adb (USB or wireless debugging) — no emulator setup exists or is needed.
- Git: commit after each feature is working and tested on-device, with a descriptive commit message, then push to `origin/main` (GitHub) — don't leave commits sitting local-only. Don't batch unrelated features into one commit.
- Editor: development happens primarily in VS Code with the Claude Code CLI; Android Studio is used occasionally just for SDK management / Gradle sync, not as the primary editor.

## Current status

Project scaffolded and pushed to GitHub. Data model (Exam/Topic/Resource/DailyLog entities + DAOs, `TargetDatabase` at schema version 2 with a real migration from v1) is implemented; completion % is computed on read from leaf topics only, never stored. Navigation Compose wires three screens — Exam List (dashboard) → Exam Detail (topic list) → Topic Detail (note resources) — all confirmed working end-to-end on-device (add exam → add topic → cycle status → add note), including the v1→v2 migration verified against real on-device data.

Not yet built: any UI for the newer Topic/Exam fields (nested sub-topics beyond one level, question-bank stats, study goals, daily check-in logging, exam metadata like registration dates/location), SAF file-import for File-type resources, and reminders. Those are the next features.
