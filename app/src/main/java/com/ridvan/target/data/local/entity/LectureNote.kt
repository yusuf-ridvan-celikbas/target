package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecture_notes",
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
data class LectureNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val title: String,
    val noteContent: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
