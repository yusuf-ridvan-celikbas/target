package com.ridvan.target.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.LectureNoteDao
import com.ridvan.target.data.local.dao.LectureNoteReadEventDao
import com.ridvan.target.data.local.dao.PracticeExamDao
import com.ridvan.target.data.local.dao.PracticeExamSubjectScoreDao
import com.ridvan.target.data.local.dao.QuestionBankDao
import com.ridvan.target.data.local.dao.QuestionBankTopicStatDao
import com.ridvan.target.data.local.dao.ResourceDao
import com.ridvan.target.data.local.dao.TestAttemptDao
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.LectureNote
import com.ridvan.target.data.local.entity.LectureNoteReadEvent
import com.ridvan.target.data.local.entity.PracticeExam
import com.ridvan.target.data.local.entity.PracticeExamSubjectScore
import com.ridvan.target.data.local.entity.QuestionBank
import com.ridvan.target.data.local.entity.QuestionBankTopicStat
import com.ridvan.target.data.local.entity.Resource
import com.ridvan.target.data.local.entity.TestAttempt
import com.ridvan.target.data.local.entity.Topic

@Database(
    entities = [
        Exam::class,
        Topic::class,
        Resource::class,
        QuestionBank::class,
        QuestionBankTopicStat::class,
        PracticeExam::class,
        PracticeExamSubjectScore::class,
        LectureNote::class,
        LectureNoteReadEvent::class,
        TestAttempt::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TargetDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun topicDao(): TopicDao
    abstract fun resourceDao(): ResourceDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun questionBankDao(): QuestionBankDao
    abstract fun questionBankTopicStatDao(): QuestionBankTopicStatDao
    abstract fun practiceExamDao(): PracticeExamDao
    abstract fun practiceExamSubjectScoreDao(): PracticeExamSubjectScoreDao
    abstract fun lectureNoteDao(): LectureNoteDao
    abstract fun lectureNoteReadEventDao(): LectureNoteReadEventDao

    companion object {
        @Volatile
        private var INSTANCE: TargetDatabase? = null

        fun getInstance(context: Context): TargetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TargetDatabase::class.java,
                    "target.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
    }
}
