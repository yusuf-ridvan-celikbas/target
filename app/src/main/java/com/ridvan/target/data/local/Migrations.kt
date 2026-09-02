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
