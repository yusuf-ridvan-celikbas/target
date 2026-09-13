package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_exam_entries",
    foreignKeys = [
        ForeignKey(
            entity = StudyResource::class,
            parentColumns = ["id"],
            childColumns = ["studyResourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studyResourceId")],
)
data class PracticeExamEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyResourceId: Long,
    val name: String,
    val questionCount: Int,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val durationMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
