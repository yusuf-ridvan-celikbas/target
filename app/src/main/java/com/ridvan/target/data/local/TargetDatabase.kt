package com.ridvan.target.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ridvan.target.data.local.dao.CourseDao
import com.ridvan.target.data.local.dao.ExamCourseDao
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamTypeDao
import com.ridvan.target.data.local.dao.LanguageDao
import com.ridvan.target.data.local.dao.SectionCourseDao
import com.ridvan.target.data.local.dao.SectionDao
import com.ridvan.target.data.local.dao.UserDao
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamCourse
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.SectionCourse
import com.ridvan.target.data.local.entity.User

@Database(
    entities = [
        ExamType::class,
        Exam::class,
        Section::class,
        Course::class,
        ExamCourse::class,
        SectionCourse::class,
        User::class,
        Language::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class TargetDatabase : RoomDatabase() {
    abstract fun examTypeDao(): ExamTypeDao
    abstract fun examDao(): ExamDao
    abstract fun sectionDao(): SectionDao
    abstract fun courseDao(): CourseDao
    abstract fun examCourseDao(): ExamCourseDao
    abstract fun sectionCourseDao(): SectionCourseDao
    abstract fun userDao(): UserDao
    abstract fun languageDao(): LanguageDao

    companion object {
        @Volatile
        private var INSTANCE: TargetDatabase? = null

        fun getInstance(context: Context): TargetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TargetDatabase::class.java,
                    "target.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
    }
}
