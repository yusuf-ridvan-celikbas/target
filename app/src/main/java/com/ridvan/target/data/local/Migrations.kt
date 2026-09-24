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

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Focus Timer sessions can now optionally link to a Language (+ one of its Topics)
        // instead of a Course — same plain nullable ADD COLUMN shape as MIGRATION_4_5's
        // exams.languageId, no recreate needed since focus_sessions' columns are already nullable.
        db.execSQL("ALTER TABLE focus_sessions ADD COLUMN languageId INTEGER REFERENCES languages(id) ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_languageId ON focus_sessions(languageId)")
    }
}
