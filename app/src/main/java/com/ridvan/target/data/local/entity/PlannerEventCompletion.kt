package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planner_event_completions",
    foreignKeys = [
        ForeignKey(
            entity = PlannerEvent::class,
            parentColumns = ["id"],
            childColumns = ["plannerEventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("plannerEventId"),
        Index(value = ["plannerEventId", "occurrenceDate"], unique = true),
    ],
)
data class PlannerEventCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plannerEventId: Long,
    /** Epoch millis, start-of-day, of the specific occurrence marked done. */
    val occurrenceDate: Long,
    val completedAt: Long = System.currentTimeMillis(),
)
