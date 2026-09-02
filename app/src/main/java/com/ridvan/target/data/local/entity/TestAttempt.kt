package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_attempts",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestionBank::class,
            parentColumns = ["id"],
            childColumns = ["questionBankId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("topicId"), Index("questionBankId")],
)
data class TestAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val questionBankId: Long? = null,
    val startedAt: Long,
    val finishedAt: Long,
    val questionsSolved: Int,
)
