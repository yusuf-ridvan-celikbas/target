# Target — Offline Exam Prep Tracker

## What this app is

A personal, fully offline Android app for organizing exam study materials and tracking study progress. Not tied to any specific exam — the user defines whatever exam(s) they're preparing for, then organizes materials underneath.

**The data model was fully reset once already** (see git history around "Full reset" for what came before — nested Topics, Question Banks, Practice Exams, Lecture Notes, Test Attempts, pace estimation). None of that survived the reset; don't resurrect ideas from it without the user asking again. What's described below is the current, second-generation model.

Hierarchy: **Exam → (optional) Section → Courses**, plus a global **Course** pool and a seeded **ExamType** list.

- **ExamType**: a real, seeded table (not an enum) — currently "University Entrance Exam", "High School Entrance Exam", "Language Exam", "Vocational Exam". Seeded idempotently on every app start (`ExamTypeDao.seedDefaultsIfEmpty()`, called from `TargetApplication.onCreate()`) rather than via `RoomDatabase.Callback.onCreate()` — that callback does **not** reliably fire (confirmed on-device: it's skipped when the schema is created via `fallbackToDestructiveMigration` rather than a genuine first-run). Managing this list (add/delete/rename by the user) is an explicitly deferred next step — don't build that screen unless asked.
- **Exam**: name, `examTypeId` (FK), `hasSections: Boolean` (decided at creation, not editable afterward — toggling it later raises unresolved questions about what happens to existing sections/courses), `examDate` (meaningful only when `!hasSections`), optional `studyStartDate`.
- **Course**: global, reusable, free-text name (e.g. "Chemistry"). Created inline wherever it's picked (type a name, create-if-missing) — there's no dedicated "manage all courses" screen yet, matching `ExamType`'s deferred-management state.
- **ExamCourse**: join table — an exam's own course pool. Always present regardless of `hasSections`, because sections pick their courses *from* this pool.
- **Section**: only exists when `Exam.hasSections`. Name + own `date` (a sectioned exam's "exam date" is really N section dates, not one field).
- **SectionCourse**: join table — a section's subset of its parent exam's course pool. The add-course-to-section UI is a **checklist picker** over `ExamCourseDao.getByExamId(exam.id)` minus what's already assigned, not create-if-missing (unlike the exam-level course pool) — a section can't have a course the exam doesn't already have.

Screen shape: `ExamListScreen` (dashboard, add exam) → `ExamDetailScreen` (edit/delete exam; always shows Courses; shows Sections too when `hasSections`) → `SectionDetailScreen` (edit/delete section; course-subset picker). Add/Edit exam share one dialog, `ui/common/ExamFormDialog.kt`'s `AddOrEditExamDialog` — it's non-trivial (name field, exam-type picker, has-sections switch, conditional date picker(s)) so it's the one deliberate exception to this app's usual "duplicate small dialogs per screen" pattern.

## Explicitly out of scope right now (do not build unless asked)

- Managing `ExamType` or the global `Course` list directly (rename/delete independent of any one exam) — called out by the user as a deferred "next step."
- Anything about study progress/tracking, question banks, practice exams, lecture notes, or pace estimation — all dropped in the reset, not part of the current model.
- Search, tags, local notifications/reminders, backup/export/restore, sync/accounts/network access of any kind.

Don't scaffold data model fields or UI for any of the above speculatively — add them when explicitly requested.

## Hard constraint: this app must stay fully offline

- No `INTERNET` permission in the manifest. No network calls, no analytics SDKs, no crash reporting services that phone home. If a dependency wants network access, flag it before adding it.
- All data lives on-device only, in a local Room database.

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3. Every "add" dialog uses `AlertDialog`; `ui/common/ExamFormDialog.kt` is the only shared cross-screen dialog so far (see above for why). Date input uses Material3's `DatePicker`/`DatePickerDialog` (`ui/common/DateFormatting.kt` has the shared display formatter) — first introduced for this model, not used in the discarded one.
- **Persistence**: Room (SQLite), currently **schema version 1** — the reset restarted versioning from scratch since none of the old tables carry over. No migration chain exists yet for *this* model. `TargetDatabase` currently carries a **temporary** `fallbackToDestructiveMigration(dropAllTables = true)` to absorb any device still holding a pre-reset (old-model) database; remove it once you're confident no such device/build is still in play — going forward, schema changes to *this* model should get real `Migration`s, not destructive fallback (that discipline from the old model still applies).
- **Seeding**: prefer an idempotent "check count, insert if empty" DAO method called from `TargetApplication.onCreate()` over `RoomDatabase.Callback.onCreate()` — see the `ExamType` note above for why the callback approach silently failed.
- **Architecture**: MVVM — Composable screens + ViewModel (StateFlow) per screen area, `AndroidViewModel` + `SavedStateHandle.toRoute()` for nav-scoped ViewModels (no DI framework).
- **Navigation**: Jetpack Navigation Compose with type-safe (`kotlinx.serialization`) routes: `ExamListRoute`, `ExamDetailRoute(examId)`, `SectionDetailRoute(sectionId)`.

Target device is a single personal phone (Samsung, One UI 8.5, Android 16) — this app is not intended for distribution, so don't add back-compat workarounds for older Android versions unless asked. Check `app/build.gradle.kts` for the actual configured `minSdk`/`targetSdk` rather than assuming a value here.

## Project conventions

- Package id: `com.ridvan.target`
- Build/run: `./gradlew assembleDebug` to build, `./gradlew installDebug` to install to the connected device (or use Android Studio's Run button / VS Code equivalent). Deploy target is the developer's own phone over adb (USB or wireless debugging) — no emulator setup exists or is needed. The phone connects over wireless debugging, whose port changes across reconnects — re-check `adb devices -l` after any connection drop rather than assuming a cached serial still works.
- Git: commit after each feature is working and tested on-device, with a descriptive commit message, then push to `origin/main` (GitHub) — don't leave commits sitting local-only. Don't batch unrelated features into one commit.
- Editor: development happens primarily in VS Code with the Claude Code CLI; Android Studio is used occasionally just for SDK management / Gradle sync, not as the primary editor.
- On-device testing via adb input/screenshots is fine, but this is the developer's actively-used personal phone — check `dumpsys window | grep mCurrentFocus` before sending taps, and stop immediately (ask the developer to drive manually instead) if focus lands on an app other than Target, since that means real concurrent use is happening.

## Current status

Second-generation model (Exam/ExamType/Section/Course) implemented and pushed, schema version 1. All three screens (List/Exam Detail/Section Detail) built with add/edit/delete for exams and sections, course pool management on the exam, and course-subset assignment on sections. Confirmed building and installing cleanly on-device; the two seeding/migration issues described above were caught and fixed via on-device testing (pulling and inspecting the live database, not just watching logcat) rather than assumed.

Not yet built: `ExamType`/`Course` management screens (deferred per the user), and the `fallbackToDestructiveMigration` temporary shim is still in place and should be revisited.
