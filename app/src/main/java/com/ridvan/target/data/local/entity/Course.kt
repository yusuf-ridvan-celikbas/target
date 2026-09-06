package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExamType::class,
            parentColumns = ["id"],
            childColumns = ["examTypeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("userId"), Index("examTypeId")],
)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userId: Long? = null,
    val icon: String? = null,
    val examTypeId: Long? = null,
    val category: CourseCategory? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
