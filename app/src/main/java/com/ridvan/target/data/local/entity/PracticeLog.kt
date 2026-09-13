package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_logs",
    foreignKeys = [
        ForeignKey(
            entity = StudyResourceTopic::class,
            parentColumns = ["id"],
            childColumns = ["studyResourceTopicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studyResourceTopicId")],
)
data class PracticeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyResourceTopicId: Long,
    val testsSolved: Int,
    val solvedCount: Int,
    val unsolvedCount: Int,
    val durationMinutes: Int,
    val loggedAt: Long = System.currentTimeMillis(),
)
