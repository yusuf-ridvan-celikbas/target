package com.ridvan.target.data.local

import com.ridvan.target.data.local.dao.BookDao
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.dao.TripActivityDao
import com.ridvan.target.data.local.dao.TripBookingDao
import com.ridvan.target.data.local.dao.TripDao
import com.ridvan.target.data.local.dao.TripExpenseDao
import com.ridvan.target.data.local.dao.TripPackingItemDao
import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripPackingItem
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ridvan.target.data.local.dao.CourseDao
import com.ridvan.target.data.local.dao.ExamCourseDao
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamTypeDao
import com.ridvan.target.data.local.dao.FocusPresetDao
import com.ridvan.target.data.local.dao.FocusSessionDao
import com.ridvan.target.data.local.dao.LanguageDao
import com.ridvan.target.data.local.dao.PlannerEventCompletionDao
import com.ridvan.target.data.local.dao.PlannerEventDao
import com.ridvan.target.data.local.dao.PracticeExamEntryDao
import com.ridvan.target.data.local.dao.PracticeExamEntryTopicResultDao
import com.ridvan.target.data.local.dao.PracticeLogDao
import com.ridvan.target.data.local.dao.SectionCourseDao
import com.ridvan.target.data.local.dao.SectionDao
import com.ridvan.target.data.local.dao.StudyResourceDao
import com.ridvan.target.data.local.dao.StudyResourceTopicDao
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.dao.UserDao
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamCourse
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import com.ridvan.target.data.local.entity.PracticeExamEntry
import com.ridvan.target.data.local.entity.PracticeExamEntryTopicResult
import com.ridvan.target.data.local.entity.PracticeLog
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.SectionCourse
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceTopic
import com.ridvan.target.data.local.entity.Topic
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
        StudyResource::class,
        Topic::class,
        StudyResourceTopic::class,
        PracticeLog::class,
        PracticeExamEntry::class,
        PracticeExamEntryTopicResult::class,
        PlannerEvent::class,
        PlannerEventCompletion::class,
        FocusPreset::class,
        FocusSession::class,
        Book::class,
        Trip::class,
        TripActivity::class,
        TripBooking::class,
        TripPackingItem::class,
        TripExpense::class,
        com.ridvan.target.data.local.entity.TripPackingTemplate::class,
        com.ridvan.target.data.local.entity.TripPackingTemplateItem::class,
        com.ridvan.target.data.local.entity.Routine::class,
        com.ridvan.target.data.local.entity.RoutineStep::class,
        com.ridvan.target.data.local.entity.RoutineStepCompletion::class,
        com.ridvan.target.data.local.entity.Workout::class,
        com.ridvan.target.data.local.entity.WorkoutExercise::class,
        com.ridvan.target.data.local.entity.WorkoutSet::class,
        com.ridvan.target.data.local.entity.SportGoal::class,
        com.ridvan.target.data.local.entity.MockExam::class,
        com.ridvan.target.data.local.entity.MockExamPart::class,
        com.ridvan.target.data.local.entity.MockExamTopicResult::class,
    ],
    version = 34,
    exportSchema = true,
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
    abstract fun studyResourceDao(): StudyResourceDao
    abstract fun topicDao(): TopicDao
    abstract fun studyResourceTopicDao(): StudyResourceTopicDao
    abstract fun practiceLogDao(): PracticeLogDao
    abstract fun practiceExamEntryDao(): PracticeExamEntryDao
    abstract fun practiceExamEntryTopicResultDao(): PracticeExamEntryTopicResultDao
    abstract fun plannerEventDao(): PlannerEventDao
    abstract fun plannerEventCompletionDao(): PlannerEventCompletionDao
    abstract fun focusPresetDao(): FocusPresetDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun bookDao(): BookDao
    abstract fun tripDao(): TripDao
    abstract fun tripActivityDao(): TripActivityDao
    abstract fun tripBookingDao(): TripBookingDao
    abstract fun tripPackingItemDao(): TripPackingItemDao
    abstract fun tripExpenseDao(): TripExpenseDao
    abstract fun tripPackingTemplateDao(): com.ridvan.target.data.local.dao.TripPackingTemplateDao
    abstract fun routineDao(): com.ridvan.target.data.local.dao.RoutineDao
    abstract fun workoutDao(): com.ridvan.target.data.local.dao.WorkoutDao
    abstract fun mockExamDao(): com.ridvan.target.data.local.dao.MockExamDao

    companion object {
        @Volatile
        private var INSTANCE: TargetDatabase? = null

        fun getInstance(context: Context): TargetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TargetDatabase::class.java,
                    "target.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34).build().also { INSTANCE = it }
            }
    }
}
