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
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["parentTopicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("examId"), Index("parentTopicId")],
)
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val parentTopicId: Long? = null,
    val name: String,
    val orderIndex: Int,
    val status: TopicStatus = TopicStatus.NOT_STARTED,
    val lastStudiedAt: Long? = null,
    val goalStartDate: Long? = null,
    val goalEndDate: Long? = null,
    val dailyQuestionTarget: Int? = null,
)
