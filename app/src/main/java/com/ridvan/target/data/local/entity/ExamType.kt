package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_types")
data class ExamType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
