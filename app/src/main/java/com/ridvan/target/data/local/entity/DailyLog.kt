package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_logs",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["topicId", "date"], unique = true)],
)
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val date: Long,
    val questionsSolved: Int = 0,
    val minutesSpent: Int = 0,
)
