package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_bank_topic_stats",
    foreignKeys = [
        ForeignKey(
            entity = QuestionBank::class,
            parentColumns = ["id"],
            childColumns = ["questionBankId"],
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
        Index(value = ["questionBankId", "topicId"], unique = true),
        Index("topicId"),
    ],
)
data class QuestionBankTopicStat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionBankId: Long,
    val topicId: Long,
    val testCount: Int? = null,
    val questionCount: Int? = null,
)
