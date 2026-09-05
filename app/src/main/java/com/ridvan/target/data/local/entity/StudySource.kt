package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sources",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("courseId"), Index("languageId")],
)
data class StudySource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val courseId: Long? = null,
    val languageId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
