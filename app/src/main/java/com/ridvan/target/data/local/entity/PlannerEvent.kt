package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planner_events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("userId"), Index("courseId"), Index("topicId")],
)
data class PlannerEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val title: String,
    val notes: String? = null,
    val category: PlannerEventCategory = PlannerEventCategory.GENERAL,
    /** Epoch millis, start-of-day, of the first/only occurrence. */
    val startDate: Long,
    /** Minute-of-day (0..1439); null = untimed/all-day. */
    val startMinuteOfDay: Int? = null,
    /** Only meaningful when startMinuteOfDay != null; null = display falls back to 60 minutes. */
    val durationMinutes: Int? = null,
    /** null = a single, non-recurring occurrence on startDate. */
    val recurrenceUnit: RecurrenceUnit? = null,
    val recurrenceInterval: Int = 1,
    /** Comma-separated ISO weekday numbers (1=Monday..7=Sunday); WEEK only, null = startDate's own weekday. */
    val recurrenceWeekdays: String? = null,
    /** Inclusive cutoff, epoch millis; null = repeats indefinitely. */
    val recurrenceEndDate: Long? = null,
    /** STUDY only. */
    val courseId: Long? = null,
    /** STUDY only, optional even when courseId is set. */
    val topicId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
