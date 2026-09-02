package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_exam_subject_scores",
    foreignKeys = [
        ForeignKey(
            entity = PracticeExam::class,
            parentColumns = ["id"],
            childColumns = ["practiceExamId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["subjectTopicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["practiceExamId", "subjectTopicId"], unique = true),
        Index("subjectTopicId"),
    ],
)
data class PracticeExamSubjectScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceExamId: Long,
    val subjectTopicId: Long,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val blank: Int = 0,
) {
    val net: Double
        get() = correct - incorrect / 4.0
}
