package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QuestionResult { CORRECT, WRONG, BLANK }

/**
 * One question (1..part's questionCount) of a mock exam part, created when it's first marked.
 * Once a part has marked questions, its correct/wrong totals and topic results are counted from them.
 */
@Entity(
    tableName = "mock_exam_questions",
    foreignKeys = [
        ForeignKey(entity = MockExamPart::class, parentColumns = ["id"], childColumns = ["mockExamPartId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("mockExamPartId"),
        Index("topicId"),
        Index(value = ["mockExamPartId", "number"], unique = true),
    ],
)
data class MockExamQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mockExamPartId: Long,
    val number: Int,
    val result: QuestionResult? = null,
    val topicId: Long? = null,
)
