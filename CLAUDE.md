# Target — Offline Study & Life Tracker (Android)

A personal, **fully offline** Android app: the user defines exams, organizes study material under them, logs practice and focus/reading time, and plans their days. Multi-account (local only). Feature history and on-device test narratives live in git history (`git log`, and this file before the 2026-10-13 trim) — this file only describes the **current** app and the rules for working on it.

## Hard constraints

- **No network, ever.** No `INTERNET` permission, no analytics/crash reporting. Flag any dependency that wants network. Share-sheet handoffs, `AlarmManager`, file pickers are fine (local IPC).
- All data lives in one local Room database (`target.db`).
- Don't speculatively scaffold features or fields. Build what's asked; ask a clarifying question when a real product choice is open.

## Out of scope unless asked

- Managing `ExamType` (add/rename/delete) — deferred.
- Lecture-notes features, pace estimation from the pre-reset model (see git "Full reset"); don't resurrect the old model.
- Search/tags beyond what exists, any sync/cloud. Preferences are not part of backup/restore (see Backup).

## Tech stack

- Kotlin, Jetpack Compose, Material 3 (+ `material-icons-extended`), MVVM: one `AndroidViewModel` + `StateFlow` per screen, no DI. Nav-scoped VMs read route args via `SavedStateHandle` (by argument name when one VM backs several routes, e.g. `CourseListByTypeViewModel`, `FocusHistoryViewModel`).
- Navigation Compose with type-safe `kotlinx.serialization` routes (`ui/navigation/Routes.kt`, wired in `TargetNavHost.kt`). Start destination: `HomeRoute` if `AppPreferences.currentUserId` is set, else `LoginRoute`.
- Room, **schema v37** (`data/local/Migrations.kt`, every version has a real `Migration`; no destructive fallback). Nullable enums are stored natively by name (no TypeConverter).
- Tests: JVM `StringsConsistencyTest` checks EN/TR strings+plurals have the same names and format placeholders. Room schema JSON is exported to `app/schemas` (ksp `room.schemaLocation`); commit new versions so migrations can be tested against them.
- Seeding: idempotent "insert if empty" DAO calls from `TargetApplication.onCreate()` (`ExamTypeDao.seedDefaultsIfEmpty()`), never `RoomDatabase.Callback.onCreate()` (doesn't fire reliably).
- `java.time` (core library desugaring on) is used **only** in `ui/planner/` and `data/notifications/`. Everywhere else uses `Calendar`/`SimpleDateFormat` (`ui/common/DateFormatting.kt`).
- Unit tests: `app/src/test/.../ui/planner/PlannerOccurrencesTest.kt`, `.../ui/reading/ReadingForecastTest.kt` (also covers `formatRate`), `.../ui/trips/TripMathTest.kt`. Most verification is on-device.
- Preferences: `data/local/AppPreferences.kt` (plain SharedPreferences, `StateFlow` per pref). Device-wide, not per user. `currentUserId` is read once per ViewModel; Log Out / Switch Account reset the whole back stack (`popUpTo(0)`).

## Data model (current)

**Accounts** — `User`: first/middle/last name, unique username, optional email, `passwordHash`/`passwordSalt` (`data/PasswordHasher.kt`, salted SHA-256; never plaintext), `preferredName` + `preferredNameSource` (recomputed on every profile save by `data/PreferredNameResolver.kt`; picker `ui/common/PreferredNameSelector.kt`). Every exam/course/language/planner/focus/book query and insert is scoped by `currentUserId`. `Exam.userId`/`Course.userId` are nullable (pre-account rows just don't show).

**Exams**
- `ExamType`: seeded shared lookup ("University Entrance Exam", "High School Entrance Exam", "Language Exam", "Vocational Exam", "Postgraduate Exam" (v36 migration adds it and moves exams named ALES there)). English in the DB; translate only at display via `examTypeDisplayName()`. Compare against `LANGUAGE_EXAM_TYPE_NAME` (in `ExamTypeDao.kt`), never a literal.
- `Exam`: name, `examTypeId`, `hasSections` (fixed at creation), `examDate` (non-sectioned only), optional `studyStartDate`, and for Language Exams only: `languageId` (SET_NULL) and free-text `level` (name = publisher, e.g. "Goethe" + "A2"). `examDisplayLabel()` = "name level"; Home's Upcoming uses "Goethe - A2 - German". A Language Exam always shows *some* language/level state ("No language set"/"Not set"), never omits the line.
- `Section` (name, own `date`) only when `hasSections`. `ExamCourse` = an exam's course pool (always present); `SectionCourse` = a section's subset of that pool (checklist picker, no create-inline). Sectioned exams hide the exam-level course list (only "+ Add" stays); Language Exams hide the Courses section entirely.

**Subjects**
- `Course`: per user, name, optional `icon` key (`ui/common/CourseIcons.kt` fixed palette, `CourseIconAvatar` everywhere a course is shown), `examTypeId` (SET_NULL; reassignable from Course Detail; Language Exam excluded), `category: CourseCategory?` (QUANTITATIVE/VERBAL). Course lists group Quantitative / Verbal / Uncategorized, collapsible, default closed.
- `Language`: per user, name only (no icon). Language Exams use a Language, never `Course` rows.
- `StudyResource`: name, exactly one of `courseId`/`languageId` (CASCADE), `type: StudyResourceType?` (QUESTION_BANK / LECTURE_TEXTBOOK / PRACTICE_EXAM / LECTURE_NOTES; required when adding), optional `publisher`. Detail screen owns Edit/Delete/Duplicate (Duplicate copies attached topics with counts and order).
- `Topic`: name, exactly one of `courseId`/`languageId` (CASCADE). `StudyResourceTopic` joins a Question Bank to a topic with target `testCount`/`questionCount` and a drag-reorderable `orderIndex`. `TopicDao.getTopicTotalsBy*` gives cross-publisher totals; Topic Detail shows the per-publisher breakdown.

**Progress**
- `PracticeLog` (per `StudyResourceTopic`): `testsSolved`, `questionCount?` (required for new logs; legacy rows fall back to solved+unsolved via `COALESCE`), `solvedCount` (correct), `unsolvedCount` (wrong), `durationMinutes`, `loggedAt`. Logging is capped live so totals never exceed the topic's target (other sessions excluded when editing one).
- `PracticeExamEntry` (inside a PRACTICE_EXAM resource): name, `questionCount`, one editable `correctCount`/`wrongCount`/`durationMinutes`; optional per-topic `PracticeExamEntryTopicResult` rows, capped both per row and against the entry's remaining correct/wrong.
- **Derived, never stored**: blank = questions − correct − wrong; net = correct − wrong/4; accuracy = correct/(correct+wrong) (blanks excluded); remaining = target − logged; pace.
- Statistics (`ui/statistics/`): one fetched list per source filtered/bucketed in Kotlin. Sources: Practice Sessions, Practice Exams (with "Topics to work on"), Mock exams (finished denemes: average/best net, chart, "Where you lose points" = topic wrong+blank, list with ALES/TYT scores → `StatsMockExamDetailRoute`), Reading. Periods Daily/Weekly/Monthly/All-time. Queries scope by user through `courses` **or** `languages` (LEFT JOIN + OR) so language-owned topics aren't silently dropped.

**Planner** (`ui/planner/`)
- `PlannerEvent`: title, notes, `category` (GENERAL/BIRTHDAY/STUDY), `startDate`, optional `startMinuteOfDay` + `durationMinutes` (derived from required Start/End time pickers), recurrence (`recurrenceUnit` DAY/WEEK/MONTH/YEAR, interval, ISO weekdays for WEEK, optional end date; Birthday forced yearly), STUDY-only `courseId`/`topicId`. `PlannerEventCompletion` is per occurrence (unique event+date).
- All recurrence maths is `PlannerOccurrences.kt`'s `occurrencesInRange()` (fast-forwards; Feb 29 → Feb 28). Week/Month/Year views merge events, exam/section dates, and read-only studied items (Focus/Flow/manual sessions, Practice logs, reading kept separate from study time). Days fold into Exams / Plans / Focus / Reading / Practice groups (folded by default).
- "Set a device alarm" hands off to the system Clock (`AlarmClock.ACTION_SET_ALARM`, time + weekdays only — caption explains it can't target a future date).

**Focus / Flow / Reading** (`ui/focustimer/`, `ui/reading/`)
- `FocusPreset`: per user, name, `workMinutes`, `breakMinutes`, `isReading`. No hardcoded templates.
- `FocusSession`: snapshotted `presetName`/minutes, optional link (`courseId` or `languageId`, + `topicId`) or `bookId` with `startPage`/`endPage`, `startedAt`/`endedAt`, `cyclesCompleted`, `totalWorkMinutes`/`totalBreakMinutes`, `notes`, `isManual`, `isFlow`. One table for Focus Timer, Flow Timer (counts up; cycles = breaks taken) and manual logs.
- Timers are foreground-only (screen kept on), wall-clock based. Start → confirm dialog → 10 s get-ready countdown. Focus: round end plays a sound and shows a 30 s "Start break?/Start work?" prompt (timeout ends and saves the session); add/subtract time, skip, Stop with confirmation. `onCleared()` saves an in-progress session. Four per-event sounds + vibration live in Settings → Focus Timer Preferences.
- `Book`: title, author, `totalPages`, `isFinished`, `isWantToRead` (status derived), genre, publisher, year, rating, notes, `coverColor` (drawn covers, no photos), `goalDate`. Current page = furthest `endPage`. Forecast and daily goal maths in `ReadingForecast.kt`. Library shows upright spines (`BookShelfView.kt`); tap pulls a book out, tap again opens it.

**Trips** (`ui/trips/`, Plan tab → Trips)
- `Trip`: per user, name, `destination`, `startDate`/`endDate` (day starts, inclusive), one `currency` (ISO code) for every amount, optional `budgetMinor`. Children (all CASCADE): `TripActivity` (day, optional time/duration, category, embedded `TripPlace`, `isDone`), `TripBooking` (type FLIGHT/TRAIN/BUS/FERRY/CAR_RENTAL/HOTEL/OTHER, provider, confirmation code, start/end day+time, place/arrival `TripPlace`), `TripPackingItem` (category, quantity, `isPacked`; "Add the basics" from `trip_starter_items`), `TripExpense` (`amountMinor`, category, day).
- Money is stored in hundredths (`parseAmountMinor`/`formatMoney`, `TripMath.kt`, Calendar-based); budget/remaining/per-day are derived. A new booking's optional price is saved as an expense, not on the booking.
- Detail screen: overview + Plan / Bookings / Packing / Budget tabs. Planner shows each trip day, activity and booking under a **Trips** group (Year view: one row per trip); tapping opens the trip.
- Trip form: one `DateRangeField` (`ui/common`, M3 DateRangePicker) for the dates; currency is chosen from a searchable list of `Currency.getAvailableCurrencies()` (common four on top). Optional `homeCurrency` + manual `exchangeRate` (home units per 1 trip unit): `Trip.moneyWithHome()` appends "(≈ …)" to budget totals and the shared summary. Plan tab = per-day timeline (`dayTimeline()`, timed first, untimed last). Packing rows swipe to delete with Undo; per-user packing templates (`trip_packing_templates` + `_items`): "Save as template" / "Add from template". Detail ⋮ → Share trip summary (`tripSummaryText()`, ACTION_SEND text/plain). Pure logic in `TripExtras.kt` (tested in `TripExtrasTest`).
- Everything is entered by hand. `data/trips/TripLookups.kt` holds interfaces for future place/booking/exchange-rate lookups, all null (`TripLookups.OFFLINE`) and unused by the UI; `TripPlace.latitude/longitude` and `TripBooking.externalRef` stay null until such a lookup exists. Adding one means network access, so it needs the developer's explicit OK first.

**Routines** (`ui/routines/`, Plan tab → Routines)
- `Routine`: per user, name, `category` (MORNING/EVENING/WORKOUT/CUSTOM, each with an icon), optional `startMinuteOfDay`, `repeatDays` (ISO weekdays, all 7 = every day), notes, `isActive`. `RoutineStep`: name + `orderIndex`, no times (CASCADE). `RoutineStepCompletion`: unique (step, day) — each day starts fresh; progress is derived (`RoutineLogic.kt`, `runsOn()`; no java.time there).
- Detail screen: header, checklist for a day (Planner passes that day's `dateMillis`; 0 = today), "Edit steps" mode (add/rename/delete/move up-down). Home shows a Routines card (one row per routine on today, "name · 07:00 · 2/5 done"); Planner Week/Month agenda has a folded Routines group (not in Year view). Reminder at the start time of timed active routines (lead-time pref), skipped once a day is fully ticked. No streaks.

**Notifications** (`data/notifications/`): no table. `NotificationScheduler.reschedule()` recomputes the single next reminder (planner events, exam/section dates, reading goals, trip first days, timed trip activities and bookings) and arms one exact alarm; firing shows what came due in the last 30 min and re-arms. Timed items use the lead-time pref, all-day items 09:00, reading goals the reading-reminder time. Rescheduled by a Room `InvalidationTracker` observer (exams, sections, planner tables, books, focus_sessions, trips, trip_activities, trip_bookings), pref setters, app start and boot.

**Exports**: Course → ⋮ → Export/Import Question Bank CSV (`data/export/CourseQuestionBank*`, BOM-prefixed UTF-8, import merges by resource name+publisher; known ambiguity when two resources share both). Section/Exam → Progress CSV (one row per course+topic, for AI weekly plans). Files read via `ui/common/ShareFile.kt` `readTextFromUri()` (handles Drive virtual docs).

## Navigation & UI

- **Shell** (`ui/shell/AppShell.kt`): bottom `NavigationBar` with five tabs — Home · Study · Plan · Focus · Stats — plus a profile icon (My Account / Settings / Help). Study, Plan (Planner + Trips) and Focus open hub pages (`ui/hub/HubScreens.kt`). Each tab keeps its own back stack (`navigateToShellDestination`, saveState/restoreState); reselecting a tab pops to its root. Shell pages that aren't tab roots get a back arrow. Drill-down detail screens use their own back-arrow `Scaffold`, no bottom bar.
- **Separate parents need separate routes**: the same screen reached from two places gets two routes (e.g. Courses vs Study Resources vs Topics bucket drill-downs; Study History from the tab vs from the timer). Study Resources/Topics flows skip Course Detail; the Courses flow goes through it.
- Detail screens own Edit/Delete (list rows only navigate). Destructive actions sit behind ⋮ or a confirmation dialog that names the consequence.
- Shared components — use them, don't re-copy: `SegmentedToggle`, `AddFab`, `GroupedCard` (animates size), `HelpTooltip` (tap to show), `DropdownField` (`ui/common/`), `countText()` plurals (`CountText.kt`), `formatDuration()` ("38m", "1h 5m"), `formatRate()` (no trailing ".0").
- `ListItem` inside a `GroupedCard` needs `ListItemDefaults.colors(containerColor = Color.Transparent)` or it hides the card tint.
- Multi-field forms need `Modifier.imePadding()`; long bodies/dialog lists need `verticalScroll`.
- Form dialogs (any input) pass `properties = FormDialogProperties` (`ui/common`) so an outside tap does not dismiss; Back still cancels. Plain confirmations may keep the default.
- Theme: custom `AppShapes`; `BannerColor` pref picks the accent (Blue = dynamic color, others = `pinnedColorScheme`); White/Blue launcher icon via two `<activity-alias>` entries.

## Localization (EN default, TR)

- Every UI string is in `values/strings.xml` + `values-tr/strings.xml`; no literals in `ui/`. Add both locales together, with matching placeholders.
- Counts use `plurals` + `countText()`, never "%d tests".
- Leading/trailing spaces need the value wrapped in double quotes. Unescaped `'` breaks the build.
- User free text is never translated, except course names for display (`courseDisplayName()`), never in an edit field's initial value or in exports.
- ViewModels emit typed results (`ErrorMessage` enum, sealed outcomes); Composables resolve the strings.
- `LazyListScope` builder bodies aren't composable: group by raw keys there, call `stringResource` inside `item {}`.
- Language switch = locale in `MainActivity.attachBaseContext()` + `Activity.recreate()` (dialogs ignore Compose-only overrides). Notifications build text with a locale-wrapped context.

## Code gotchas

- Hand-built reorderable lists: wrap rows in `key(id)`, and consume pointer input during the long-press wait inside a scrollable parent (see `detectDragHandleGesture` in `StudyResourceDetailScreen.kt`).
- Fan-out: aggregate queries join only one "many" side at a time; otherwise sum in Kotlin.
- Migrations: SQLite can't drop/relax columns — use create-new/copy/drop/rename (precedent in `MIGRATION_8_9`, `13_14`, `18_19`). Added columns with no source stay `NULL` ("orphaned, not destroyed"). Never edit a migration that already ran on the phone.
- Statistics once crashed because a string gained a placeholder and one call site wasn't updated — grep every usage when changing a format string.

## Build, device, git

- Package `com.ridvan.target`. `./gradlew assembleDebug` / `./gradlew installDebug`. Target is the developer's own phone (Samsung, Android 16) over wireless adb — the port changes; re-check `adb devices -l`. Check `app/build.gradle.kts` for min/target SDK.
- It's the developer's **daily phone**: check `dumpsys window | grep mCurrentFocus` before tapping; stop if another app is in front. If a personal notification appears, stop screenshots and verify via `dumpsys`/DB pulls. Data that looks wrong mid-test may be the developer using the phone — pull the DB before "fixing" anything. Delete test data afterwards.
- Device testing tricks: screenshot coordinates × ~1.17; `adb shell input text` fails on non-ASCII (use ASCII placeholders); AlertDialog buttons may need a held tap (`input swipe X Y X Y 80`); drag gestures via `input draganddrop`; pull DB **with** `-wal`/`-shm` files (or it's stale); prefer `dumpsys`/`sqlite3` over screenshots to save tokens.
- Workflow per feature: implement → verify on device → commit (descriptive, one feature per commit) → push → add a **short** note here (current behavior only, no test narrative).
- **Parallel sessions**: several Claude sessions may work at once. Each works on its own worktree branch. Session `cross-session-visibility` ("Compass") coordinates: message it before any commit to main, push, `installDebug` or adb use, say "done" after; claim the next schema version with it before writing a migration; announce edits to shared files (`Migrations.kt`, `TargetDatabase`, `TargetNavHost.kt`, `Routes.kt`, `AppShell.kt`, `HubScreens.kt`, `strings.xml`, this file). Nothing merges to `main` without the developer's OK.
- Token budget is tight: keep replies short, avoid needless screenshots and subagents.

## Backup
Settings > Backup card: `data/backup/BackupManager.kt` writes the whole Room DB (WAL checkpointed first) to a user-picked file (`CreateDocument`, `Target-backup-YYYY-MM-DD.db`) and stores `AppPreferences.lastBackupAt`. Restore (`OpenDocument`, binary virtual-document fallback) stages the file in cacheDir, validates it with `BackupValidator` (SQLite header, required tables, user_version <= 29; unit-tested), warns, saves a safety copy to `filesDir/backups/`, closes the DB, replaces target.db (deletes -wal/-shm), clears the session if the signed-in user isn't in the restored DB, relaunches and exits the process. Older backups migrate normally on next open. Preferences are never included.

- **Home top bar**: the greeting ("Welcome to Target, <bold name>!") and today's weekday/date live in the top bar (`AppShell` `titleContent` slot, single-line ellipsis each); the clock card is gone from Home's body.
- **Swipe-to-delete with Undo**: `FocusHistoryRow(onDelete = ...)` wraps the row in a `SwipeToDismissBox` (end-to-start, red background); `rememberUndoDelete` deletes via the ViewModel and shows a "Session deleted / Undo" snackbar (`AppShell` `snackbarHost` param) that re-inserts the same row. Used on Focus/Flow Timer, Study/Work History and a book's sessions.

- **Today Home** (`ui/home/`): cards in order, each hidden when empty: Countdowns (nearest 3 exam/section dates + See all to Exams), Today's plans (tomorrow's if none today, checkboxes + Open Planner), Today so far (studied today/week, reading + daily goal, up to 3 session rows), Work on next, and an always-shown Quick start (Focus/Flow Timer buttons).
- **Work on next** (`WorkOnNext.kt`, `StudyResourceTopicDao.getWorkOnNextRows`): up to 3 topics merged across Question Banks; first those with >=10 answered and accuracy <85% (lowest first, "Accuracy N%"), then most questions left vs target ("N questions left"). Tap opens TopicProgressRoute when the topic is in one bank, else TopicDetailRoute.

## Modules (per account)
`User.enabledModules` (comma-separated `AppModule` names; NULL = all on, existing accounts keep everything). `AppModule`: EXAMS_STUDY, PLANNER_ROUTINES, FOCUS, READING, TRIPS, SPORTS (`data/modules/`); `TargetApplication.modules.enabled` is a live StateFlow, read in Compose via `rememberEnabledModules()`. Picked on `ui/modules/ModulesScreen` (right after Register, or Settings > Modules; at least one stays on). Disabled = hidden, data kept: tabs (`ui/shell/ModuleVisibility.kt`: Study<-EXAMS_STUDY, Plan<-PLANNER_ROUTINES|TRIPS, Focus<-FOCUS|READING|SPORTS, Stats<-EXAMS_STUDY|READING; AppShell goes Home if the current tab vanishes), hub rows, Home cards, Statistics sources, Planner agenda groups, notification candidates, Help exam sections. New feature screens must respect these gates.

- **Recovery code** (schema v30): `users.recoveryHash/recoverySalt`; `data/RecoveryCode.kt` (12 chars, no 0/O/1/I/L, shown XXXX-XXXX-XXXX, normalized before salted hashing). Register shows it once (`RecoveryCodeDialog`); Login > Forgot password? (`ResetPasswordScreen`) sets a new password and issues a new code; profile row creates/replaces it after the current password. No minimum password length exists app-wide.

## Sports (schema v32, `ui/sports/`, module SPORTS)
Focus hub > Sports (`SportsRoute`; live stopwatch `LiveWorkoutRoute`; `WorkoutDetailRoute` owns Edit/Delete). Tables (CASCADE): `workouts` (`Sport` enum RUNNING/WALKING/CYCLING/SWIMMING/GYM/YOGA/HIKING/FOOTBALL/BASKETBALL/TENNIS, `startedAt`, `durationSeconds`, `distanceMeters?` for distance sports, notes, effort 1-5, `isLive`), `workout_exercises` + `workout_sets` (Gym: reps, `weightKg`, 0 = bodyweight), `sport_goals` (sport or any, WEEK/MONTH, DISTANCE_KM/MINUTES/WORKOUTS). All maths is pure in `SportsMath.kt` (pace per km, swimming per 100 m; Monday weeks; goal progress; day/week streaks; fastest 5 km = pace x 5 from runs >= 5 km; longest run/ride/swim; heaviest lift per exercise name) and unit-tested. Stats live on the Sports page itself (week/month toggle, daily-minutes bar chart, per-sport totals, goals, bests, streaks, recent list with swipe-delete + Undo that restores exercises). Live workout mirrors Flow Timer (confirm, 10 s get-ready, wall-clock, keep screen on, `onCleared()` saves; runs under 10 s are dropped); after Stop it asks distance (distance sports) or exercises (Gym). Planner: folded "Sports" agenda group (Week/Month only). Home "Today so far": today workouts + goal lines. History (`SportsHistoryRoute`, `SportsHistory.kt`, from the recent card): month switcher + sport filter, grouped by day, swipe-delete + Undo; the workout form warns on future dates and confirms on Save.

## Mock exams / deneme (schema v33-36, `ui/mockexam/`, module EXAMS_STUDY)
One non-stop sitting (e.g. ALES 50+50 in 150 min), for every exam: non-sectioned = the exam's course pool; sectioned = + Add asks the section (`mock_exams.sectionId`, parts = that section's courses, prefill per section; rows show the section); language exams = free-named parts (`mock_exam_parts.courseId` NULL + `name`, no topic breakdown). Tables (CASCADE): `mock_exams` (examId, name, publisher, `timeLimitMinutes`, `durationMinutes`, `takenAt` = start, `finishedAt`, `status` PLANNED/RUNNING/FINISHED, optional `scheduledAt`), `mock_exam_parts` (per course: questionCount/correct/wrong), `mock_exam_topic_results` (per part+topic, capped like practice-exam topic rows). Net/blank derived (`MockExamMath.kt`). Exam Detail > Mock exams card: + Add prefills from the last deneme; Save = planned, Already taken = finished. Detail page: Start (confirm, 10 s `GetReadyCountdown`, then `MockExamTimerRoute` wall-clock countdown from `takenAt`, keep screen on, WORK_END sound at time-up, Finish early, Back = keep running/discard), results per course + topic rows, ⋮ Edit details/Delete. Planned start: notification at `scheduledAt` (opens the app) + app-wide `MockExamDuePrompt` in `TargetNavHost` (asks up to 6 h after; Not now = this session). Not yet in Planner/Home.
- **Questions** (schema v37, `mock_exam_questions`: part, number, `result` CORRECT/WRONG/BLANK?, `topicId` SET_NULL; part row's grid icon → `MockExamQuestionsRoute`): brush grid (result or topic, "Mark the rest correct"). `MockExamDao.saveQuestions()` recounts the part's correct/wrong once any question has a result and rebuilds its topic results once any has a topic; Edit part then hides correct/wrong. Named parts tag the language exam's language topics.
- **Scoring** (deneme ⋮ → Scoring; `MockExamScoring.kt`, maths in `MockExamMath.kt`): `mock_exams.scoringSystem` ALES/TYT/AYT (null = nets only) + per-part `scoringTest` ("counts as", guessed from category/name, copied to the next deneme). Own approximations, labelled approximate: ALES fitted constants (100 cap); TYT = 100 + 3.3/3.4 per net; YKS SAY/SÖZ/EA = 100 + 0.4×(TYT−100) + 3 per AYT net of the type (TYT part from the exam's latest finished TYT deneme). Never fetch formulas from outside sites. `exams.alesScoring`/`ales*` columns are unused leftovers.
