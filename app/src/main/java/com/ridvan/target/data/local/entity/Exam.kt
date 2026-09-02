package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ExamType::class,
            parentColumns = ["id"],
            childColumns = ["examTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("examTypeId"), Index("userId")],
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val examTypeId: Long,
    val userId: Long? = null,
    val hasSections: Boolean,
    val examDate: Long? = null,
    val studyStartDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
