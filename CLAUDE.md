# Target — Offline Exam Prep Tracker

## What this app is

A personal, fully offline Android app for organizing exam study materials and tracking study progress. Not tied to any specific exam — the user defines whatever exam(s) they're preparing for, then organizes materials underneath.

Hierarchy: **Exam → Topic (nestable) → {Question Bank, Practice Exam, Lecture Note}**

- **Exam**: something the user is preparing for — name, exam date (`targetDate`), study-start date, exam location, registration open/close dates. All optional except name.
- **Topic**: a subject/chapter/sub-chapter within an Exam (name, ordering, status, last-studied date). Topics can nest to any depth via an optional `parentTopicId` — e.g. YKS → Chemistry → TYT is three Topic rows nested two deep. This is deliberate: the schema doesn't hardcode any specific exam's structure (like YKS's Subject/TYT-AYT split); depth and shape are whatever the user builds. A Topic also does double duty as a reusable "curriculum topic" name (e.g. "Mol Kavramı") that a Question Bank's stats or a Practice Exam's subject score can point at — same entity, two uses.
  - A Topic can carry a study goal (`goalStartDate`, `goalEndDate`, `dailyQuestionTarget`), compared against that day's summed `TestAttempt`s (see below) — there's no separately-stored daily total to drift out of sync.
- **Question Bank**: filed under a Topic branch (e.g. "Chemistry → TYT"). Links to curriculum Topics via `QuestionBankTopicStat` (test count + question count *per topic per bank*) — the same topic can appear in multiple question banks with different counts in each, since the stats live on the link, not on the Topic.
- **Test Attempt**: the one way study progress gets logged — one solved test, with `topicId`, an optional `questionBankId` (nullable: studying without pulling from a specific bank's inventory still counts toward pace, just doesn't decrement that bank's remaining count; `ON DELETE SET_NULL` so deleting a bank doesn't erase study history), `startedAt`/`finishedAt`, and `questionsSolved`. `QuestionBankTopicStatDao`'s `QuestionBankTopicStatProgress` projection joins attempts to compute `remainingTests`/`remainingQuestions` per (bank, topic) — summed across a topic's banks for "remaining in total," and a bank is "complete" when every one of its rows hits zero remaining (derived, not a stored flag). `StudyPaceEstimator.estimateRemainingPace()` projects days/minutes remaining from a topic's most recent ~7 days of attempts — a rolling window, not an all-time average, so pace adapts as study habits change.
- **Practice Exam**: attaches directly to an Exam (not to a specific Topic) with a free-text `examType` (e.g. "TYT"/"AYT") — a "General" exam spans every subject at once, so it can't hang off one Topic branch. Has a scheduled date, allotted/actual time, and one `PracticeExamSubjectScore` row per subject covered (correct/incorrect/blank counts, net = correct − incorrect/4, computed not stored). One score row = subject-specific exam; multiple = General — derived from the row count, not a stored flag.
- **Lecture Note**: belongs to one Topic; typed text and/or an imported file (same SAF pattern as below). Each read is logged as a `LectureNoteReadEvent` row (timestamp) rather than a counter, so read count / last-read / "nth read" are all derived from that history.

An older generic `Resource` (Note/File) entity + `TopicDetailScreen` still exist in code but are slated for removal once real screens exist for the three kinds above — see Current status.

Progress tracking: each Topic has a status (`Not Started` / `In Progress` / `Done`) and a last-studied timestamp. Each Exam shows a completion percentage rolled up from its **leaf** Topics' statuses only (a Topic that just holds sub-topics isn't itself counted). A dashboard screen lists all Exams with their completion %.

Reminders (e.g. for registration deadlines or daily study goals) are a planned future addition — not built yet, don't scaffold for them speculatively.

## Explicitly out of scope for v1 (do not build unless asked)

- Interactive quizzing / self-testing with scored attempts, or spaced repetition. Question banks track test/question counts per topic, not individual questions or answers.
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
- **Persistence**: Room (SQLite) — Exam, Topic, TestAttempt, QuestionBank/QuestionBankTopicStat, PracticeExam/PracticeExamSubjectScore, LectureNote/LectureNoteReadEvent, plus the older Resource. Schema changes go through a real `Migration` (see `data/local/Migrations.kt`) — `fallbackToDestructiveMigration()` is deliberately not used, since there's real on-device study data to preserve across schema changes. Room's runtime validator requires an *exact* column match against a table it has a matching `@Entity` for — a removed field needs an actual `ALTER TABLE ... DROP COLUMN`, not just deleting it from the Kotlin entity — but it does *not* seem to mind an orphaned table with no matching `@Entity` at all (e.g. the old `daily_logs` table, left in place rather than dropped).
- **Business logic outside Room**: `data/StudyPaceEstimator.kt` is plain Kotlin (no Room/Android dependency) — that's the intended home for computation that isn't persistence itself (query results in, a computed answer out), rather than folding it into a DAO query or a ViewModel.
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

Project scaffolded and pushed to GitHub. `TargetDatabase` is at schema version 4 (real migrations from v1, each verified on-device against the developer's actual data, not a fresh install). Navigation Compose wires three screens — Exam List (dashboard) → Exam Detail (topic list) → Topic Detail (note resources) — confirmed working end-to-end on-device (add exam → add topic → cycle status → add note).

Data layer has Exam/Topic/TestAttempt plus QuestionBank/QuestionBankTopicStat, PracticeExam/PracticeExamSubjectScore, and LectureNote/LectureNoteReadEvent entities and DAOs, plus the remaining-work/pace-projection queries and `StudyPaceEstimator` — **no UI for any of this yet**. The older Resource entity + TopicDetailScreen (Note-only) are still what's actually on screen; they'll be removed once real screens exist for the three new kinds (a question-bank editor with remaining-work/completion display, a practice-exam scorer, a lecture-note viewer with read-tracking) — that's the next feature to build, and it's a substantial one.

Also not yet built: UI for the Exam/Topic metadata fields already in the schema (registration dates/location, study goals, daily check-in logging), SAF file-import, and reminders.
