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
