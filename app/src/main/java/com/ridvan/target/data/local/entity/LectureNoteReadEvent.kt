package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecture_note_read_events",
    foreignKeys = [
        ForeignKey(
            entity = LectureNote::class,
            parentColumns = ["id"],
            childColumns = ["lectureNoteId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lectureNoteId")],
)
data class LectureNoteReadEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureNoteId: Long,
    val readAt: Long = System.currentTimeMillis(),
)
