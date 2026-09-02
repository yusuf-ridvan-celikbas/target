package com.ridvan.target.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ridvan.target.data.local.dao.CourseDao
import com.ridvan.target.data.local.dao.ExamCourseDao
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamTypeDao
import com.ridvan.target.data.local.dao.SectionCourseDao
import com.ridvan.target.data.local.dao.SectionDao
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamCourse
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.SectionCourse

@Database(
    entities = [ExamType::class, Exam::class, Section::class, Course::class, ExamCourse::class, SectionCourse::class],
    version = 1,
    exportSchema = false,
)
abstract class TargetDatabase : RoomDatabase() {
    abstract fun examTypeDao(): ExamTypeDao
    abstract fun examDao(): ExamDao
    abstract fun sectionDao(): SectionDao
    abstract fun courseDao(): CourseDao
    abstract fun examCourseDao(): ExamCourseDao
    abstract fun sectionCourseDao(): SectionCourseDao

    companion object {
        @Volatile
        private var INSTANCE: TargetDatabase? = null

        fun getInstance(context: Context): TargetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TargetDatabase::class.java,
                    "target.db",
                )
                    // One-time: wipes any leftover pre-reset (v2-v4) database on this device.
                    // The old schema's tables no longer exist as entities, so there is no
                    // meaningful migration path from them -- remove once this reset has landed.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}
