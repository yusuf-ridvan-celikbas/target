package com.ridvan.target.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ResourceDao
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.Resource
import com.ridvan.target.data.local.entity.Topic

@Database(
    entities = [Exam::class, Topic::class, Resource::class],
    version = 1,
    exportSchema = false,
)
abstract class TargetDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun topicDao(): TopicDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        @Volatile
        private var INSTANCE: TargetDatabase? = null

        fun getInstance(context: Context): TargetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TargetDatabase::class.java,
                    "target.db",
                ).build().also { INSTANCE = it }
            }
    }
}
