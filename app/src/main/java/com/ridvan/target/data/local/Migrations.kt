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
