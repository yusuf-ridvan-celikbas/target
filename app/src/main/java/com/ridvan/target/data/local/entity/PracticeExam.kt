package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_exams",
    foreignKeys = [
        ForeignKey(
            entity = Exam::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("examId")],
)
data class PracticeExam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val examType: String,
    val name: String,
    val scheduledDate: Long? = null,
    val allottedMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
