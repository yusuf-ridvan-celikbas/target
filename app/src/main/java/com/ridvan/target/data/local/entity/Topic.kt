package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
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
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val name: String,
    val orderIndex: Int,
    val status: TopicStatus = TopicStatus.NOT_STARTED,
    val lastStudiedAt: Long? = null,
)
