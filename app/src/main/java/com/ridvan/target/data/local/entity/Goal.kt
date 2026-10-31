package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** TARGET = reach a total, HABIT = N times per day/week, MEASURE = move a value from start to target, MILESTONES = tick steps. */
enum class GoalKind { TARGET, HABIT, MEASURE, MILESTONES }

enum class HabitPeriod { DAY, WEEK }

/** Existing data a TARGET goal can fill itself from (counted from its start date); manual logs are off then. */
enum class GoalSource { STUDY_MINUTES, QUESTIONS_ANSWERED, PAGES_READ, BOOKS_FINISHED, WORKOUTS, WORKOUT_MINUTES, WORKOUT_KM }

/**
 * A personal goal. [targetValue]: TARGET total, HABIT times per [period], MEASURE target reading (unused for
 * MILESTONES). [startValue] is MEASURE's first reading. [startDate]/[deadline] are day starts.
 */
@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val kind: GoalKind,
    val unit: String? = null,
    val targetValue: Double? = null,
    val startValue: Double? = null,
    val period: HabitPeriod? = null,
    val source: GoalSource? = null,
    val startDate: Long,
    val deadline: Long? = null,
    val notes: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One step: an amount added (TARGET), one time done (HABIT, amount 1) or a new reading (MEASURE). [date] is a day start. */
@Entity(
    tableName = "goal_logs",
    foreignKeys = [ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId")],
)
data class GoalLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val date: Long,
    val amount: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "goal_milestones",
    foreignKeys = [ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId")],
)
data class GoalMilestone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val name: String,
    val orderIndex: Int,
    val doneAt: Long? = null,
)
