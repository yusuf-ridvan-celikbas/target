package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_banks",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("topicId")],
)
data class QuestionBank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
