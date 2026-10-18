package com.ridvan.target.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A combined mock exam ("deneme") taken non-stop across an exam's courses, e.g. ALES:
 * 50 Turkish + 50 Math in 150 minutes. One total duration; results live per course in [MockExamPart].
 */
@Entity(
    tableName = "mock_exams",
    foreignKeys = [
        ForeignKey(entity = Exam::class, parentColumns = ["id"], childColumns = ["examId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("examId")],
)
data class MockExam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val name: String,
    val publisher: String? = null,
    val timeLimitMinutes: Int,
    /** Time actually used; set when the timer finishes or typed in by hand. */
    val durationMinutes: Int = 0,
    /** When it was started (timer) or taken (manual entry); for a planned one, when it was saved. */
    val takenAt: Long,
    /** Null until it's finished (planned or timer still running). */
    val finishedAt: Long? = null,
    @ColumnInfo(defaultValue = "FINISHED") val status: MockExamStatus = MockExamStatus.FINISHED,
    /** Optional planned start; when it comes, the app asks whether to start. */
    val scheduledAt: Long? = null,
)

enum class MockExamStatus { PLANNED, RUNNING, FINISHED }

@Entity(
    tableName = "mock_exam_parts",
    foreignKeys = [
        ForeignKey(entity = MockExam::class, parentColumns = ["id"], childColumns = ["mockExamId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Course::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("mockExamId"),
        Index("courseId"),
        Index(value = ["mockExamId", "courseId"], unique = true),
    ],
)
data class MockExamPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mockExamId: Long,
    val courseId: Long,
    val questionCount: Int,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val orderIndex: Int = 0,
)

@Entity(
    tableName = "mock_exam_topic_results",
    foreignKeys = [
        ForeignKey(entity = MockExamPart::class, parentColumns = ["id"], childColumns = ["mockExamPartId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("mockExamPartId"),
        Index("topicId"),
        Index(value = ["mockExamPartId", "topicId"], unique = true),
    ],
)
data class MockExamTopicResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mockExamPartId: Long,
    val topicId: Long,
    val questionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
)
