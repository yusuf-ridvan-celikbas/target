package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
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
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val name: String,
    val date: Long? = null,
    val orderIndex: Int,
)
