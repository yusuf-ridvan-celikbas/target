package com.ridvan.target.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                firstName TEXT NOT NULL,
                middleName TEXT,
                preferredName TEXT NOT NULL,
                lastName TEXT NOT NULL,
                username TEXT NOT NULL,
                email TEXT,
                passwordHash TEXT NOT NULL,
                passwordSalt TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users(username)")

        db.execSQL("ALTER TABLE exams ADD COLUMN userId INTEGER REFERENCES users(id) ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exams_userId ON exams(userId)")

        db.execSQL("ALTER TABLE courses ADD COLUMN userId INTEGER REFERENCES users(id) ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_userId ON courses(userId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN preferredNameSource TEXT NOT NULL DEFAULT 'OTHER'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN icon TEXT")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS languages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_languages_userId ON languages(userId)")

        db.execSQL("ALTER TABLE exams ADD COLUMN languageId INTEGER REFERENCES languages(id) ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exams_languageId ON exams(languageId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN examTypeId INTEGER REFERENCES exam_types(id) ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_examTypeId ON courses(examTypeId)")
        // Best-effort backfill: infer a pre-existing course's exam type from an exam it's already attached to.
        db.execSQL(
            """
            UPDATE courses SET examTypeId = (
                SELECT e.examTypeId FROM exam_courses ec
                JOIN exams e ON e.id = ec.examId
                WHERE ec.courseId = courses.id
                LIMIT 1
            )
            WHERE examTypeId IS NULL
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sources (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                courseId INTEGER REFERENCES courses(id) ON DELETE CASCADE,
                languageId INTEGER REFERENCES languages(id) ON DELETE CASCADE,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sources_courseId ON study_sources(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sources_languageId ON study_sources(languageId)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN category TEXT")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE study_sources ADD COLUMN type TEXT")
        db.execSQL("ALTER TABLE study_sources ADD COLUMN publisher TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // "Study Sources" was renamed to "Study Resources" throughout the app; rename the table
        // and its indices to match rather than recreating them, to preserve existing rows.
        db.execSQL("ALTER TABLE study_sources RENAME TO study_resources")
        db.execSQL("DROP INDEX IF EXISTS index_study_sources_courseId")
        db.execSQL("DROP INDEX IF EXISTS index_study_sources_languageId")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_resources_courseId ON study_resources(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_resources_languageId ON study_resources(languageId)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                courseId INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_courseId ON topics(courseId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_resource_topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                studyResourceId INTEGER NOT NULL REFERENCES study_resources(id) ON DELETE CASCADE,
                topicId INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
                testCount INTEGER NOT NULL DEFAULT 0,
                questionCount INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_study_resource_topics_studyResourceId_topicId " +
                "ON study_resource_topics(studyResourceId, topicId)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_resource_topics_topicId ON study_resource_topics(topicId)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE study_resource_topics ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        // Backfill orderIndex to match the current alphabetical-by-topic-name order per resource
        // (a correlated-subquery rank, not a window function, for portability), so migrating
        // doesn't visually reorder any existing attached-topics list.
        db.execSQL(
            """
            UPDATE study_resource_topics
            SET orderIndex = (
                SELECT COUNT(*)
                FROM study_resource_topics srt2
                JOIN topics t2 ON t2.id = srt2.topicId
                JOIN topics t1 ON t1.id = study_resource_topics.topicId
                WHERE srt2.studyResourceId = study_resource_topics.studyResourceId
                  AND (t2.name < t1.name OR (t2.name = t1.name AND srt2.id < study_resource_topics.id))
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                studyResourceTopicId INTEGER NOT NULL REFERENCES study_resource_topics(id) ON DELETE CASCADE,
                testsSolved INTEGER NOT NULL,
                solvedCount INTEGER NOT NULL,
                unsolvedCount INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                loggedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_logs_studyResourceTopicId ON practice_logs(studyResourceTopicId)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE study_resources ADD COLUMN questionCount INTEGER")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exam_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                studyResourceId INTEGER NOT NULL REFERENCES study_resources(id) ON DELETE CASCADE,
                correctCount INTEGER NOT NULL,
                wrongCount INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                loggedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_attempts_studyResourceId ON practice_exam_attempts(studyResourceId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exam_topic_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                practiceExamAttemptId INTEGER NOT NULL REFERENCES practice_exam_attempts(id) ON DELETE CASCADE,
                topicId INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
                questionCount INTEGER NOT NULL,
                correctCount INTEGER NOT NULL,
                wrongCount INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_topic_results_practiceExamAttemptId ON practice_exam_topic_results(practiceExamAttemptId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_topic_results_topicId ON practice_exam_topic_results(topicId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_practice_exam_topic_results_practiceExamAttemptId_topicId " +
                "ON practice_exam_topic_results(practiceExamAttemptId, topicId)"
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // MIGRATION_12_13 wrongly put a single questionCount on the StudyResource itself and modeled
        // practice exams as many dated "attempts." The real shape (confirmed after testing on-device):
        // a Practice Exam StudyResource is a container of many independently-named exams ("Deneme 1",
        // "Deneme 2", ...), each with its own question count. Undo the wrong column without touching
        // any real study_resources data, via the standard SQLite drop-column-via-recreate pattern.
        db.execSQL(
            """
            CREATE TABLE study_resources_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                courseId INTEGER REFERENCES courses(id) ON DELETE CASCADE,
                languageId INTEGER REFERENCES languages(id) ON DELETE CASCADE,
                type TEXT,
                publisher TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO study_resources_new (id, name, courseId, languageId, type, publisher, createdAt)
            SELECT id, name, courseId, languageId, type, publisher, createdAt FROM study_resources
            """.trimIndent()
        )
        db.execSQL("DROP TABLE study_resources")
        db.execSQL("ALTER TABLE study_resources_new RENAME TO study_resources")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_resources_courseId ON study_resources(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_resources_languageId ON study_resources(languageId)")

        // The wrong-shaped attempt tables held only this session's own test data, not real study data.
        db.execSQL("DROP TABLE IF EXISTS practice_exam_topic_results")
        db.execSQL("DROP TABLE IF EXISTS practice_exam_attempts")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exam_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                studyResourceId INTEGER NOT NULL REFERENCES study_resources(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                questionCount INTEGER NOT NULL,
                correctCount INTEGER NOT NULL,
                wrongCount INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_entries_studyResourceId ON practice_exam_entries(studyResourceId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exam_entry_topic_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                practiceExamEntryId INTEGER NOT NULL REFERENCES practice_exam_entries(id) ON DELETE CASCADE,
                topicId INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
                questionCount INTEGER NOT NULL,
                correctCount INTEGER NOT NULL,
                wrongCount INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_entry_topic_results_practiceExamEntryId ON practice_exam_entry_topic_results(practiceExamEntryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exam_entry_topic_results_topicId ON practice_exam_entry_topic_results(topicId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_practice_exam_entry_topic_results_practiceExamEntryId_topicId " +
                "ON practice_exam_entry_topic_results(practiceExamEntryId, topicId)"
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nullable: existing sessions have no recorded question count and can't be backfilled
        // (the same "orphaned rather than destroyed" precedent as every other added column with
        // no inferable source, e.g. MIGRATION_12_13's study_resources.questionCount). Lets a
        // logged session's blank/unanswered count be derived (questionCount - solved - unsolved)
        // instead of assuming solved+unsolved was the whole test.
        db.execSQL("ALTER TABLE practice_logs ADD COLUMN questionCount INTEGER")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Splits a Language Exam's proficiency level (e.g. "A2") out of the free-text exam
        // name (which previously had to hold something like "Goethe A2" as one string) into
        // its own nullable free-text column, same shape/no-backfill precedent as languageId
        // and every other Language-Exam-only field — meaningless, and left null, for any
        // other exam type.
        db.execSQL("ALTER TABLE exams ADD COLUMN level TEXT")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS planner_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                title TEXT NOT NULL,
                notes TEXT,
                category TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                startMinuteOfDay INTEGER,
                durationMinutes INTEGER,
                recurrenceUnit TEXT,
                recurrenceInterval INTEGER NOT NULL DEFAULT 1,
                recurrenceWeekdays TEXT,
                recurrenceEndDate INTEGER,
                courseId INTEGER REFERENCES courses(id) ON DELETE SET NULL,
                topicId INTEGER REFERENCES topics(id) ON DELETE SET NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_planner_events_userId ON planner_events(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_planner_events_courseId ON planner_events(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_planner_events_topicId ON planner_events(topicId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS planner_event_completions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plannerEventId INTEGER NOT NULL REFERENCES planner_events(id) ON DELETE CASCADE,
                occurrenceDate INTEGER NOT NULL,
                completedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_planner_event_completions_plannerEventId ON planner_event_completions(plannerEventId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_planner_event_completions_plannerEventId_occurrenceDate " +
                "ON planner_event_completions(plannerEventId, occurrenceDate)"
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Focus Timer: reusable named work/break presets, and one row per completed run.
        // No backfill needed — brand new feature with no prior data, same shape as MIGRATION_16_17.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_presets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                workMinutes INTEGER NOT NULL,
                breakMinutes INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_presets_userId ON focus_presets(userId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                presetId INTEGER REFERENCES focus_presets(id) ON DELETE SET NULL,
                presetName TEXT NOT NULL,
                workMinutes INTEGER NOT NULL,
                breakMinutes INTEGER NOT NULL,
                courseId INTEGER REFERENCES courses(id) ON DELETE SET NULL,
                topicId INTEGER REFERENCES topics(id) ON DELETE SET NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL,
                cyclesCompleted INTEGER NOT NULL,
                totalWorkMinutes INTEGER NOT NULL,
                totalBreakMinutes INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_userId ON focus_sessions(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_presetId ON focus_sessions(presetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_courseId ON focus_sessions(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_topicId ON focus_sessions(topicId)")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Topics generalize from Course-only to Course-or-Language, mirroring StudyResource's
        // dual-nullable courseId/languageId shape. SQLite can't relax an existing NOT NULL column
        // via ALTER TABLE, so this uses the same drop-column/recreate pattern as MIGRATION_13_14 —
        // every existing row keeps its courseId, languageId comes back NULL.
        db.execSQL(
            """
            CREATE TABLE topics_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                courseId INTEGER REFERENCES courses(id) ON DELETE CASCADE,
                languageId INTEGER REFERENCES languages(id) ON DELETE CASCADE,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO topics_new (id, name, courseId, languageId, createdAt)
            SELECT id, name, courseId, NULL, createdAt FROM topics
            """.trimIndent()
        )
        db.execSQL("DROP TABLE topics")
        db.execSQL("ALTER TABLE topics_new RENAME TO topics")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_courseId ON topics(courseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_languageId ON topics(languageId)")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Focus Timer sessions gain a free-text notes field (edited on FocusSessionDetailScreen) —
        // plain nullable ADD COLUMN, no backfill possible for existing sessions.
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN notes TEXT")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Focus Timer sessions can now optionally link to a Language (+ one of its Topics)
        // instead of a Course — same plain nullable ADD COLUMN shape as MIGRATION_4_5's
        // exams.languageId, no recreate needed since focus_sessions' columns are already nullable.
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN languageId INTEGER REFERENCES languages(id) ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_languageId ON focus_sessions(languageId)")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Focus sessions can now be logged by hand (no timer, no preset); every existing row ran on the timer.
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Reading: a per-user Books table, reading presets, and book/page columns on focus sessions.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                title TEXT NOT NULL,
                author TEXT,
                totalPages INTEGER,
                isFinished INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_books_userId ON books(userId)")
        db.execSQL("ALTER TABLE focus_presets ADD COLUMN isReading INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN bookId INTEGER REFERENCES books(id) ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_bookId ON focus_sessions(bookId)")
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN startPage INTEGER")
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN endPage INTEGER")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Library: genre, publisher/year, rating, notes, a want-to-read status and a generated cover colour.
        db.execSQL("ALTER TABLE books ADD COLUMN genre TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN publisher TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN publishedYear INTEGER")
        db.execSQL("ALTER TABLE books ADD COLUMN rating INTEGER")
        db.execSQL("ALTER TABLE books ADD COLUMN notes TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN isWantToRead INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN coverColor INTEGER NOT NULL DEFAULT 0")
        // Existing books get a random cover colour too (12 = the cover palette's size).
        db.execSQL("UPDATE books SET coverColor = abs(random()) % 12")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Reading goals: an optional finish-by day per book.
        db.execSQL("ALTER TABLE books ADD COLUMN goalDate INTEGER")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Flow Timer: count-up sessions saved alongside Focus Timer ones.
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN isFlow INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Trip Planner: trips plus their itinerary, bookings, packing list and expenses. Brand-new tables, no backfill.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER REFERENCES users(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                destination TEXT,
                startDate INTEGER NOT NULL,
                endDate INTEGER NOT NULL,
                currency TEXT NOT NULL,
                budgetMinor INTEGER,
                notes TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_userId ON trips(userId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_activities (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                date INTEGER NOT NULL,
                startMinuteOfDay INTEGER,
                durationMinutes INTEGER,
                place_name TEXT,
                place_address TEXT,
                place_latitude REAL,
                place_longitude REAL,
                notes TEXT,
                isDone INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_activities_tripId ON trip_activities(tripId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_bookings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                provider TEXT,
                confirmationCode TEXT,
                startDate INTEGER NOT NULL,
                startMinuteOfDay INTEGER,
                endDate INTEGER,
                endMinuteOfDay INTEGER,
                place_name TEXT,
                place_address TEXT,
                place_latitude REAL,
                place_longitude REAL,
                arrival_name TEXT,
                arrival_address TEXT,
                arrival_latitude REAL,
                arrival_longitude REAL,
                notes TEXT,
                externalRef TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_bookings_tripId ON trip_bookings(tripId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_packing_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                isPacked INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_packing_items_tripId ON trip_packing_items(tripId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trip_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tripId INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
                title TEXT NOT NULL,
                amountMinor INTEGER NOT NULL,
                category TEXT NOT NULL,
                date INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_expenses_tripId ON trip_expenses(tripId)")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Routines: a named, repeating group of ordered steps, ticked per step per day. Brand-new tables, no backfill.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                startMinuteOfDay INTEGER,
                repeatDays TEXT NOT NULL,
                notes TEXT,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_userId ON routines(userId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                orderIndex INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_steps_routineId ON routine_steps(routineId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_step_completions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineStepId INTEGER NOT NULL REFERENCES routine_steps(id) ON DELETE CASCADE,
                date INTEGER NOT NULL,
                completedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_step_completions_routineStepId ON routine_step_completions(routineStepId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_routine_step_completions_routineStepId_date ON routine_step_completions(routineStepId, date)"
        )
    }
}

/** Per-account modules: NULL = every module on, so existing accounts keep everything. */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN enabledModules TEXT")
    }
}

/** Offline password reset: salted hash of the account recovery code (NULL = none set). */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN recoveryHash TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN recoverySalt TEXT")
    }
}

/** Trips: optional home currency + manual exchange rate; per-user reusable packing templates. */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN homeCurrency TEXT")
        db.execSQL("ALTER TABLE trips ADD COLUMN exchangeRate REAL")
        db.execSQL("CREATE TABLE IF NOT EXISTS `trip_packing_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `name` TEXT NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_packing_templates_userId` ON `trip_packing_templates` (`userId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `trip_packing_template_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `quantity` INTEGER NOT NULL, FOREIGN KEY(`templateId`) REFERENCES `trip_packing_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_packing_template_items_templateId` ON `trip_packing_template_items` (`templateId`)")
    }
}

/** Sports: workouts, gym exercises/sets and weekly/monthly goals. */
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `workouts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `sport` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `distanceMeters` INTEGER, `notes` TEXT, `effort` INTEGER, `isLive` INTEGER NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_userId` ON `workouts` (`userId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutId` INTEGER NOT NULL, `name` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_workoutId` ON `workout_exercises` (`workoutId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL NOT NULL, FOREIGN KEY(`exerciseId`) REFERENCES `workout_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sport_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `sport` TEXT, `period` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetValue` REAL NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sport_goals_userId` ON `sport_goals` (`userId`)")
    }
}

/** Combined mock exams ("deneme"): one timed sitting across an exam's courses, results per course and topic. */
val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `mock_exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, `name` TEXT NOT NULL, `publisher` TEXT, `timeLimitMinutes` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `takenAt` INTEGER NOT NULL, `finishedAt` INTEGER, FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mock_exams_examId` ON `mock_exams` (`examId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `mock_exam_parts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mockExamId` INTEGER NOT NULL, `courseId` INTEGER NOT NULL, `questionCount` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, FOREIGN KEY(`mockExamId`) REFERENCES `mock_exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mock_exam_parts_mockExamId` ON `mock_exam_parts` (`mockExamId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mock_exam_parts_courseId` ON `mock_exam_parts` (`courseId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mock_exam_parts_mockExamId_courseId` ON `mock_exam_parts` (`mockExamId`, `courseId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `mock_exam_topic_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mockExamPartId` INTEGER NOT NULL, `topicId` INTEGER NOT NULL, `questionCount` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, FOREIGN KEY(`mockExamPartId`) REFERENCES `mock_exam_parts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`topicId`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mock_exam_topic_results_mockExamPartId` ON `mock_exam_topic_results` (`mockExamPartId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mock_exam_topic_results_topicId` ON `mock_exam_topic_results` (`topicId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mock_exam_topic_results_mockExamPartId_topicId` ON `mock_exam_topic_results` (`mockExamPartId`, `topicId`)")
    }
}

/** Mock exams can be planned (optional start date/time) and started later from their page. */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE mock_exams ADD COLUMN status TEXT NOT NULL DEFAULT 'FINISHED'")
        db.execSQL("ALTER TABLE mock_exams ADD COLUMN scheduledAt INTEGER")
        db.execSQL("UPDATE mock_exams SET status = 'RUNNING' WHERE finishedAt IS NULL")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exams ADD COLUMN alesScoring INTEGER NOT NULL DEFAULT 0")
        listOf("alesSayBase", "alesSayCoef", "alesSozBase", "alesSozCoef", "alesEaBase", "alesEaCoef").forEach {
            db.execSQL("ALTER TABLE exams ADD COLUMN $it REAL")
        }
    }
}
