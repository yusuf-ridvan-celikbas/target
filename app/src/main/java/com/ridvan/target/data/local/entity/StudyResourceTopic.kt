package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_resource_topics",
    foreignKeys = [
        ForeignKey(
            entity = StudyResource::class,
            parentColumns = ["id"],
            childColumns = ["studyResourceId"],
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
        Index(value = ["studyResourceId", "topicId"], unique = true),
        Index("topicId"),
    ],
)
data class StudyResourceTopic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyResourceId: Long,
    val topicId: Long,
    val testCount: Int = 0,
    val questionCount: Int = 0,
)
