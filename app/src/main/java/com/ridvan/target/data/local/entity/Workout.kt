package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Sports the Sports module knows. Stored by name; [isDistance] sports log a distance. */
enum class Sport(val isDistance: Boolean) {
    RUNNING(true), WALKING(true), CYCLING(true), SWIMMING(true), GYM(false),
    YOGA(false), HIKING(true), FOOTBALL(false), BASKETBALL(false), TENNIS(false),
}

enum class GoalPeriod { WEEK, MONTH }

enum class GoalTargetType { DISTANCE_KM, MINUTES, WORKOUTS }

/** One workout. Duration in seconds, distance in metres (null when not a distance sport or not logged). */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val sport: Sport,
    val startedAt: Long,
    val durationSeconds: Int,
    val distanceMeters: Int? = null,
    val notes: String? = null,
    /** Perceived effort 1..5. */
    val effort: Int? = null,
    /** Logged with the live stopwatch rather than by hand. */
    val isLive: Boolean = false,
)

/** A gym exercise inside a workout, e.g. "Bench press". */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(entity = Workout::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val orderIndex: Int = 0,
)

/** One set of an exercise; [weightKg] 0 = bodyweight. */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutExercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("exerciseId")],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val orderIndex: Int = 0,
    val reps: Int,
    val weightKg: Double = 0.0,
)

/** A weekly/monthly target; [sport] null = any sport. [targetValue] is km, minutes or a workout count. */
@Entity(
    tableName = "sport_goals",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class SportGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val sport: Sport? = null,
    val period: GoalPeriod,
    val targetType: GoalTargetType,
    val targetValue: Double,
)
