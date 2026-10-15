package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RoutineCategory { MORNING, EVENING, WORKOUT, CUSTOM }

/** A named group of ordered steps done on chosen weekdays (e.g. a morning routine). */
@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val category: RoutineCategory = RoutineCategory.CUSTOM,
    /** Minute-of-day (0..1439); null = no set time (and no reminder). */
    val startMinuteOfDay: Int? = null,
    /** Comma-separated ISO weekday numbers (1=Monday..7=Sunday); all seven = every day. */
    val repeatDays: String = ALL_DAYS,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val ALL_DAYS = "1,2,3,4,5,6,7"
    }
}

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("routineId")],
)
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val name: String,
    val orderIndex: Int = 0,
)

/** One step ticked on one day; [date] is that day's start-of-day epoch millis. Each day starts fresh. */
@Entity(
    tableName = "routine_step_completions",
    foreignKeys = [
        ForeignKey(entity = RoutineStep::class, parentColumns = ["id"], childColumns = ["routineStepId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("routineStepId"),
        Index(value = ["routineStepId", "date"], unique = true),
    ],
)
data class RoutineStepCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineStepId: Long,
    val date: Long,
    val completedAt: Long = System.currentTimeMillis(),
)
