package com.ridvan.target.ui.sports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.ExerciseWithSets
import com.ridvan.target.data.local.entity.GoalPeriod
import com.ridvan.target.data.local.entity.GoalTargetType
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutSet

@Composable
fun Sport.label(): String = stringResource(
    when (this) {
        Sport.RUNNING -> R.string.sport_running
        Sport.WALKING -> R.string.sport_walking
        Sport.CYCLING -> R.string.sport_cycling
        Sport.SWIMMING -> R.string.sport_swimming
        Sport.GYM -> R.string.sport_gym
        Sport.YOGA -> R.string.sport_yoga
        Sport.HIKING -> R.string.sport_hiking
        Sport.FOOTBALL -> R.string.sport_football
        Sport.BASKETBALL -> R.string.sport_basketball
        Sport.TENNIS -> R.string.sport_tennis
    },
)

fun Sport.icon(): ImageVector = when (this) {
    Sport.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    Sport.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    Sport.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    Sport.SWIMMING -> Icons.Filled.Pool
    Sport.GYM -> Icons.Filled.FitnessCenter
    Sport.YOGA -> Icons.Filled.SelfImprovement
    Sport.HIKING -> Icons.Filled.Hiking
    Sport.FOOTBALL -> Icons.Filled.SportsSoccer
    Sport.BASKETBALL -> Icons.Filled.SportsBasketball
    Sport.TENNIS -> Icons.Filled.SportsTennis
}

/** "5 km" / "750 m" (swimming below a kilometre reads better in metres). */
@Composable
fun distanceText(meters: Int, sport: Sport): String =
    if (sport == Sport.SWIMMING && meters < 1000) stringResource(R.string.sports_distance_m, meters)
    else stringResource(R.string.sports_distance_km, formatKm(meters))

/** "5:30 min/km" (swimming "2:00 min/100 m"); null when the workout has no distance. */
@Composable
fun paceText(sport: Sport, durationSeconds: Int, distanceMeters: Int?): String? {
    val pace = paceSeconds(sport, durationSeconds, distanceMeters) ?: return null
    return stringResource(if (sport == Sport.SWIMMING) R.string.sports_pace_100m else R.string.sports_pace_km, formatClock(pace))
}

/** "45m · 5 km · 5:30 min/km" for a workout row. */
@Composable
fun workoutSummary(workout: Workout): String {
    val parts = mutableListOf(com.ridvan.target.ui.common.formatDuration(minutesOf(workout.durationSeconds).coerceAtLeast(1)))
    workout.distanceMeters?.takeIf { it > 0 }?.let { parts += distanceText(it, workout.sport) }
    paceText(workout.sport, workout.durationSeconds, workout.distanceMeters)?.let { parts += it }
    return parts.joinToString(" · ")
}

@Composable
fun goalTitle(goal: SportGoal): String {
    val sport = goal.sport?.label() ?: stringResource(R.string.sports_goal_any)
    val what = stringResource(
        when (goal.targetType) {
            GoalTargetType.DISTANCE_KM -> R.string.sports_goal_type_distance_short
            GoalTargetType.MINUTES -> R.string.sports_goal_type_minutes_short
            GoalTargetType.WORKOUTS -> R.string.sports_goal_type_workouts_short
        },
    )
    val per = stringResource(if (goal.period == GoalPeriod.WEEK) R.string.sports_goal_per_week else R.string.sports_goal_per_month)
    return stringResource(R.string.sports_goal_title, sport, what, per)
}

/** "12 / 20 km", "90 / 120 min", "2 / 3". */
@Composable
fun goalProgressText(goal: SportGoal, progress: GoalProgress): String {
    val current = formatNumber(progress.current)
    val target = formatNumber(progress.target)
    return stringResource(
        when (goal.targetType) {
            GoalTargetType.DISTANCE_KM -> R.string.sports_goal_progress_distance
            GoalTargetType.MINUTES -> R.string.sports_goal_progress_minutes
            GoalTargetType.WORKOUTS -> R.string.sports_goal_progress_workouts
        },
        current,
        target,
    )
}

/** Up to one decimal, none for whole numbers; locale decimal mark. */
fun formatNumber(value: Double): String = com.ridvan.target.ui.common.formatRate(value)

/** "3 × 10 × 60 kg" groups of one exercise, joined by ", ". */
@Composable
fun setsText(sets: List<WorkoutSet>): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return groupSets(sets).joinToString(", ") { group ->
        if (group.weightKg > 0) context.getString(R.string.sports_set_group, group.count, group.reps, formatWeight(group.weightKg))
        else context.getString(R.string.sports_set_group_bw, group.count, group.reps)
    }
}

@Composable
fun exerciseLine(item: ExerciseWithSets): String {
    val sets = setsText(item.sets)
    return if (sets.isEmpty()) item.exercise.name else "${item.exercise.name}  $sets"
}
