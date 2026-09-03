# Target — Offline Exam Prep Tracker

## What this app is

A personal, fully offline Android app for organizing exam study materials and tracking study progress. Not tied to any specific exam — the user defines whatever exam(s) they're preparing for, then organizes materials underneath. It's now genuinely multi-account (see below) — "personal" describes the deployment (one phone, not distributed), not a single-user data model.

**The data model was fully reset once already** (see git history around "Full reset" for what came before — nested Topics, Question Banks, Practice Exams, Lecture Notes, Test Attempts, pace estimation). None of that survived the reset; don't resurrect ideas from it without the user asking again. What's described below is the current, second-generation model, now with accounts layered on top.

### Accounts

Real multi-account auth, not a single-profile gate — `Register` can create additional accounts at any time; `Login` looks up by username. `User`: firstName, middleName (optional), lastName, username (unique), email (optional), passwordHash/passwordSalt (`data/PasswordHasher.kt` — salted SHA-256 via `SecureRandom`/`MessageDigest`, no new dependency; passwords are never stored or compared in plaintext), preferredName + preferredNameSource.

**Preferred name** is chosen via a toggle (First / Middle) plus three mutually-exclusive checkboxes (Last Name / Username / Other — "Other" reveals a free-text field), not typed directly — `ui/common/PreferredNameSelector.kt`, shared between Register and Edit Profile. The choice **stays live**: `User.preferredNameSource` is stored, and `data/PreferredNameResolver.kt`'s `resolvePreferredName()` recomputes the stored `preferredName` string from the current source field every time the profile is saved — so editing First Name later automatically updates Preferred Name too, if that's the active source. This is the one place in the app where a value is deliberately kept in sync via recomputation-on-save rather than either a live read-time join or a frozen one-time value.

Session persistence and the light/dark theme preference both live in `data/local/AppPreferences.kt` — a plain `SharedPreferences` wrapper (deliberately not DataStore, no new dependency needed for two small values). `currentUserId` is read once per ViewModel at construction, not observed reactively — there's no log-out/switch-account UI yet, so a session can't change mid-process. `isDarkMode` **is** a `StateFlow`, since `MainActivity` needs to react immediately when Settings toggles it.

`Exam` and `Course` carry a nullable `userId` (FK, cascade) — nullable because real pre-auth exam data already existed on-device with no owner when accounts were added; those rows just don't appear once list queries filter by the logged-in user, rather than being destroyed. Every exam/course query and insert must be scoped by `AppPreferences.currentUserId` — there is no global, cross-account exam or course data (unlike `ExamType`, which stays a shared/seeded lookup table, not per-user).

### Exam data (unchanged in shape since the last update, now user-scoped)

Hierarchy: **Exam → (optional) Section → Courses**, plus a per-user **Course** pool and a shared, seeded **ExamType** list.

- **ExamType**: a real, seeded table (not an enum) — currently "University Entrance Exam", "High School Entrance Exam", "Language Exam", "Vocational Exam". Seeded idempotently on every app start (`ExamTypeDao.seedDefaultsIfEmpty()`, called from `TargetApplication.onCreate()`) rather than via `RoomDatabase.Callback.onCreate()` — that callback does **not** reliably fire when the schema is created via a destructive-fallback recreation rather than a genuine first-run (confirmed on-device). Managing this list (add/delete/rename by the user) is an explicitly deferred next step — don't build that screen unless asked. Shared across all accounts, not per-user.
- **Exam**: name, `examTypeId` (FK), `userId` (FK, nullable — see Accounts), `hasSections: Boolean` (decided at creation, not editable afterward — toggling it later raises unresolved questions about what happens to existing sections/courses), `examDate` (meaningful only when `!hasSections`), optional `studyStartDate`.
- **Course**: per-user, reusable, free-text name (e.g. "Chemistry"). Has its own full CRUD screen (`ui/courselist/`, reached from Home's drawer): list/add/rename/delete across all of a user's exams. The exam-level "Add course" dialog (`ExamDetailScreen`) shows the user's not-yet-attached courses as a **multi-select checklist** (`ExamDetailViewModel.availableCoursesToAdd`, checked ones added together via `addExistingCourses`) alongside a text field for creating a new course inline (create-if-missing, case-insensitive name match against the user's pool). It used to be blind free-text entry only — an already-created course had no visible way to be picked, only re-typed exactly — fixed after being reported as a bug.
- **ExamCourse**: join table — an exam's own course pool. Always present regardless of `hasSections`, because sections pick their courses *from* this pool.
- **Section**: only exists when `Exam.hasSections`. Name + own `date` (a sectioned exam's "exam date" is really N section dates, not one field).
- **SectionCourse**: join table — a section's subset of its parent exam's course pool. The add-course-to-section UI is a **checklist picker** over `ExamCourseDao.getByExamId(exam.id)` minus what's already assigned, not create-if-missing (unlike the exam-level course pool) — a section can't have a course the exam doesn't already have.

### App icon

Two selectable launcher icons — `ic_launcher`/`ic_launcher_round` (white/default) and `ic_launcher_blue`/`ic_launcher_blue_round` (blue) — each an adaptive icon (foreground/background XML pair in `drawable`/`drawable-xxxhdpi`, plus legacy raster fallbacks at every mipmap density). Both were generated from flat-background design PNGs (developer's desktop `target-app-designs` folder) by chroma-keying the background color to transparent for the foreground layer — there's no Android Studio Image Asset step in this workflow, so any future icon variant needs the same treatment (sample the actual flat background pixel, not a corner pixel, which can be an anti-aliasing outlier). Switching icons is done via two `<activity-alias>` entries in the manifest (`AppIconDefault`, `AppIconBlue`, both `targetActivity=".MainActivity"`), toggled at runtime through `PackageManager.setComponentEnabledSetting(..., DONT_KILL_APP)` from `SettingsViewModel.setUseBlueAppIcon()` — `MainActivity` itself no longer carries the `LAUNCHER` intent-filter, only the aliases do. The choice persists in `AppPreferences.useBlueAppIcon` (`StateFlow<Boolean>`, same pattern as `isDarkMode`) so the Settings toggle reflects the right side on reopen. Settings shows this as a White/Blue segmented toggle styled like `PreferredNameSelector`'s First/Middle segments (not a plain `Switch`) — the second deliberate reuse of that visual pattern outside its original component. Known quirk: switching aliases on an app already installed via adb can cause the launcher to show a stale/duplicate icon until the app is uninstalled and reinstalled — a launcher-cache artifact, not a data bug.

### Navigation shell

`ui/shell/AppShell.kt` is the persistent chrome — a `ModalNavigationDrawer` (a 140dp-tall banner image, `drawable-xxxhdpi/drawer_banner.png` rendered via `Image(..., contentScale = ContentScale.Crop)`, clickable to go Home — then Exams, Courses, then a "Home" item pinned to the drawer's bottom via a `ColumnScope` weight spacer) and a `TopAppBar` with a hamburger (opens the drawer) and a three-dot overflow menu (User → Edit Profile, Settings → theme toggle). **Every** shell-level screen renders through it: `HomeScreen`, `ExamListScreen`, `CourseListScreen`, `UserEditScreen`, `SettingsScreen` all call `AppShell(navigation = shellNavigation, title = ...) { innerPadding -> ... }` instead of their own bare `Scaffold` — the menu is never absent on any of them, and none of them has a back-arrow icon anymore (getting back to Home is via the drawer, not a back button; the Android system back gesture still works normally through the nav back stack). `ShellNavigation` (also in `AppShell.kt`) bundles the five `onNavigate*` callbacks and is threaded through `TargetNavHost` into each of these screens' composable calls.

`ExamDetailScreen` and `SectionDetailScreen` are the deliberate exception — reached by drilling into a *specific* exam/section (not a drawer destination), they keep their own simple back-arrow `Scaffold` as before, not `AppShell`.

Drawer/overflow navigation between the five shell-level destinations uses `popUpTo<HomeRoute> { saveState = true }` + `launchSingleTop = true` + `restoreState = true` (a private `NavHostController.navigateToShellDestination()` extension in `TargetNavHost.kt`) — bouncing between Exams/Courses/User/Settings via the menu doesn't pile up back-stack entries the way plain repeated `navigate()` calls would.

Screen shape: `LoginScreen`/`RegisterScreen` → `HomeScreen` → `ExamListScreen` → `ExamDetailScreen` (edit/delete exam; always shows Courses; shows Sections too when `hasSections`) → `SectionDetailScreen` (edit/delete section; course-subset picker); `CourseListScreen`, `UserEditScreen`, `SettingsScreen` reachable from any shell screen's drawer/overflow menu, not just Home's. Add/Edit exam share one dialog, `ui/common/ExamFormDialog.kt`'s `AddOrEditExamDialog` — it's non-trivial (name field, exam-type picker, has-sections switch, conditional date picker(s)) so it's the one deliberate exception to this app's usual "duplicate small dialogs per screen" pattern (the other being `PreferredNameSelector`, above).

Forms with more than a couple of fields need `Modifier.imePadding()` on their scrollable content column, or the on-screen keyboard covers whatever's focused — `enableEdgeToEdge()` plus a manifest `windowSoftInputMode="adjustResize"` alone isn't sufficient in Compose. All of `LoginScreen`/`RegisterScreen`/`UserEditScreen` have it; apply it to any new multi-field form too.

## Explicitly out of scope right now (do not build unless asked)

- Log out / switch account — no UI for it yet. `AppPreferences.currentUserId` is designed to support it later (a screen that clears it and navigates back to `LoginRoute`); don't build the screen speculatively.
- Managing `ExamType` directly (rename/delete) — called out by the user as a deferred "next step." (Course management now exists — see above.)
- Anything about study progress/tracking, question banks, practice exams, lecture notes, or pace estimation — all dropped in the reset, not part of the current model.
- Search, tags, local notifications/reminders, backup/export/restore, sync/network access of any kind (accounts are now in scope, but they're local-only — no cloud sync).

Don't scaffold data model fields or UI for any of the above speculatively — add them when explicitly requested.

## Hard constraint: this app must stay fully offline

- No `INTERNET` permission in the manifest. No network calls, no analytics SDKs, no crash reporting services that phone home. If a dependency wants network access, flag it before adding it.
- All data lives on-device only, in a local Room database.

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3. Every "add" dialog uses `AlertDialog`; `ui/common/ExamFormDialog.kt` is the only shared cross-screen dialog so far (see above for why). Date input uses Material3's `DatePicker`/`DatePickerDialog` (`ui/common/DateFormatting.kt` has the shared display formatter) — first introduced for this model, not used in the discarded one.
- **Persistence**: Room (SQLite), currently **schema version 3** (`data/local/Migrations.kt`: `MIGRATION_1_2` added `users`/`userId` columns, `MIGRATION_2_3` added `User.preferredNameSource`). The temporary `fallbackToDestructiveMigration` from the previous reset has been **removed** — real `Migration`s are back in force for all schema changes, including the ones that added accounts, since there's real on-device exam data to preserve.
- **Seeding**: prefer an idempotent "check count, insert if empty" DAO method called from `TargetApplication.onCreate()` over `RoomDatabase.Callback.onCreate()` — see the `ExamType` note above for why the callback approach silently failed.
- **Architecture**: MVVM — Composable screens + ViewModel (StateFlow) per screen area, `AndroidViewModel` + `SavedStateHandle.toRoute()` for nav-scoped ViewModels (no DI framework).
- **Navigation**: Jetpack Navigation Compose with type-safe (`kotlinx.serialization`) routes: `LoginRoute`, `RegisterRoute`, `HomeRoute`, `ExamListRoute`, `ExamDetailRoute(examId)`, `SectionDetailRoute(sectionId)`, `CourseListRoute`, `UserEditRoute`, `SettingsRoute`. Start destination is conditional on `AppPreferences.currentUserId` (`HomeRoute` if set, else `LoginRoute`), computed once in `TargetNavHost` via `LocalContext.current`.

Target device is a single personal phone (Samsung, One UI 8.5, Android 16) — this app is not intended for distribution, so don't add back-compat workarounds for older Android versions unless asked. Check `app/build.gradle.kts` for the actual configured `minSdk`/`targetSdk` rather than assuming a value here.

## Project conventions

- Package id: `com.ridvan.target`
- Build/run: `./gradlew assembleDebug` to build, `./gradlew installDebug` to install to the connected device (or use Android Studio's Run button / VS Code equivalent). Deploy target is the developer's own phone over adb (USB or wireless debugging) — no emulator setup exists or is needed. The phone connects over wireless debugging, whose port changes across reconnects — re-check `adb devices -l` after any connection drop rather than assuming a cached serial still works.
- Git: commit after each feature is working and tested on-device, with a descriptive commit message, then push to `origin/main` (GitHub) — don't leave commits sitting local-only. Don't batch unrelated features into one commit.
- Editor: development happens primarily in VS Code with the Claude Code CLI; Android Studio is used occasionally just for SDK management / Gradle sync, not as the primary editor.
- On-device testing via adb input/screenshots is fine, but this is the developer's actively-used personal phone — check `dumpsys window | grep mCurrentFocus` before sending taps, and stop immediately (ask the developer to drive manually instead) if focus lands on an app other than Target, since that means real concurrent use is happening.

## Current status

Second-generation model (Exam/ExamType/Section/Course) plus accounts (User/auth), the persistent navigation shell (`ui/shell/AppShell.kt`, present on Home/Exams/Courses/User/Settings, not just Home) with a real drawer banner image, Settings (theme toggle + White/Blue app icon preference), and Courses management are all implemented and pushed, schema version 3. Confirmed building, migrating cleanly over real on-device exam data, and running end-to-end on-device (register, dark mode toggle, Exams/Courses via the drawer, User/Settings via the overflow menu, the preferred-name selector and old/new/confirm password change, the menu staying present across all four non-Home shell screens, switching the launcher icon between white/blue, and the exam-level course picker showing/multi-selecting existing courses).

Not yet built: log out / switch account, `ExamType` management.
