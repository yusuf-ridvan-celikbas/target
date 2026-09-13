package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_exam_entry_topic_results",
    foreignKeys = [
        ForeignKey(
            entity = PracticeExamEntry::class,
            parentColumns = ["id"],
            childColumns = ["practiceExamEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("practiceExamEntryId"),
        Index("topicId"),
        Index(value = ["practiceExamEntryId", "topicId"], unique = true),
    ],
)
data class PracticeExamEntryTopicResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceExamEntryId: Long,
    val topicId: Long,
    val questionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
)
