package com.ridvan.target.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exams ADD COLUMN studyStartDate INTEGER")
        db.execSQL("ALTER TABLE exams ADD COLUMN examLocation TEXT")
        db.execSQL("ALTER TABLE exams ADD COLUMN registrationStartDate INTEGER")
        db.execSQL("ALTER TABLE exams ADD COLUMN registrationEndDate INTEGER")

        db.execSQL("ALTER TABLE topics ADD COLUMN parentTopicId INTEGER REFERENCES topics(id) ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_parentTopicId ON topics(parentTopicId)")
        db.execSQL("ALTER TABLE topics ADD COLUMN testCount INTEGER")
        db.execSQL("ALTER TABLE topics ADD COLUMN questionCount INTEGER")
        db.execSQL("ALTER TABLE topics ADD COLUMN goalStartDate INTEGER")
        db.execSQL("ALTER TABLE topics ADD COLUMN goalEndDate INTEGER")
        db.execSQL("ALTER TABLE topics ADD COLUMN dailyQuestionTarget INTEGER")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                date INTEGER NOT NULL,
                questionsSolved INTEGER NOT NULL,
                minutesSpent INTEGER NOT NULL,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_logs_topicId_date ON daily_logs(topicId, date)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE topics DROP COLUMN testCount")
        db.execSQL("ALTER TABLE topics DROP COLUMN questionCount")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS question_banks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_question_banks_topicId ON question_banks(topicId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS question_bank_topic_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questionBankId INTEGER NOT NULL,
                topicId INTEGER NOT NULL,
                testCount INTEGER,
                questionCount INTEGER,
                FOREIGN KEY(questionBankId) REFERENCES question_banks(id) ON DELETE CASCADE,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_question_bank_topic_stats_questionBankId_topicId " +
                "ON question_bank_topic_stats(questionBankId, topicId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_question_bank_topic_stats_topicId " +
                "ON question_bank_topic_stats(topicId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exams (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                examId INTEGER NOT NULL,
                examType TEXT NOT NULL,
                name TEXT NOT NULL,
                scheduledDate INTEGER,
                allottedMinutes INTEGER,
                actualMinutes INTEGER,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(examId) REFERENCES exams(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_exams_examId ON practice_exams(examId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_exam_subject_scores (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                practiceExamId INTEGER NOT NULL,
                subjectTopicId INTEGER NOT NULL,
                correct INTEGER NOT NULL,
                incorrect INTEGER NOT NULL,
                blank INTEGER NOT NULL,
                FOREIGN KEY(practiceExamId) REFERENCES practice_exams(id) ON DELETE CASCADE,
                FOREIGN KEY(subjectTopicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_practice_exam_subject_scores_practiceExamId_subjectTopicId " +
                "ON practice_exam_subject_scores(practiceExamId, subjectTopicId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_practice_exam_subject_scores_subjectTopicId " +
                "ON practice_exam_subject_scores(subjectTopicId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lecture_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                title TEXT NOT NULL,
                noteContent TEXT,
                filePath TEXT,
                fileName TEXT,
                mimeType TEXT,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_notes_topicId ON lecture_notes(topicId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lecture_note_read_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lectureNoteId INTEGER NOT NULL,
                readAt INTEGER NOT NULL,
                FOREIGN KEY(lectureNoteId) REFERENCES lecture_notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_lecture_note_read_events_lectureNoteId " +
                "ON lecture_note_read_events(lectureNoteId)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS test_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                questionBankId INTEGER,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER NOT NULL,
                questionsSolved INTEGER NOT NULL,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY(questionBankId) REFERENCES question_banks(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_attempts_topicId ON test_attempts(topicId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_test_attempts_questionBankId ON test_attempts(questionBankId)"
        )
    }
}
