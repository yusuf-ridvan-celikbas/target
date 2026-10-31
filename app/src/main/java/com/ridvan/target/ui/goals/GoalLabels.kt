package com.ridvan.target.ui.goals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.HabitPeriod
import com.ridvan.target.data.local.entity.GoalSource
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.formatRate

fun GoalKind.icon(): ImageVector = when (this) {
    GoalKind.TARGET -> Icons.Filled.Flag
    GoalKind.HABIT -> Icons.Filled.Repeat
    GoalKind.MEASURE -> Icons.Filled.ShowChart
    GoalKind.MILESTONES -> Icons.Filled.Checklist
}

fun GoalKind.labelRes(): Int = when (this) {
    GoalKind.TARGET -> R.string.goal_kind_target
    GoalKind.HABIT -> R.string.goal_kind_habit
    GoalKind.MEASURE -> R.string.goal_kind_measure
    GoalKind.MILESTONES -> R.string.goal_kind_milestones
}

fun GoalSource.labelRes(): Int = when (this) {
    GoalSource.STUDY_MINUTES -> R.string.goal_source_study_minutes
    GoalSource.QUESTIONS_ANSWERED -> R.string.goal_source_questions
    GoalSource.PAGES_READ -> R.string.goal_source_pages
    GoalSource.BOOKS_FINISHED -> R.string.goal_source_books
    GoalSource.WORKOUTS -> R.string.goal_source_workouts
    GoalSource.WORKOUT_MINUTES -> R.string.goal_source_workout_minutes
    GoalSource.WORKOUT_KM -> R.string.goal_source_km
}

private fun GoalSource.unitRes(): Int = when (this) {
    GoalSource.STUDY_MINUTES, GoalSource.WORKOUT_MINUTES -> R.string.goal_unit_minutes
    GoalSource.QUESTIONS_ANSWERED -> R.string.goal_unit_questions
    GoalSource.PAGES_READ -> R.string.goal_unit_pages
    GoalSource.BOOKS_FINISHED -> R.string.goal_unit_books
    GoalSource.WORKOUTS -> R.string.goal_unit_workouts
    GoalSource.WORKOUT_KM -> R.string.goal_unit_km
}

/** The unit shown after amounts: the source's own for auto goals, else what the user typed. */
@Composable
fun goalUnit(goal: Goal): String = goal.source?.let { stringResource(it.unitRes()) } ?: goal.unit.orEmpty()

/** "320 / 5000 questions", "3 / 5 this week", "Now 78.5 kg · target 75 kg", "2 of 5 steps done". */
@Composable
fun goalProgressLine(item: GoalItem): String {
    val goal = item.goal
    val p = item.progress
    val target = p.target?.let { formatRate(it) } ?: "–"
    return when (goal.kind) {
        GoalKind.TARGET -> stringResource(R.string.goal_progress_of, formatRate(p.current), target, goalUnit(goal)).trim()
        GoalKind.HABIT -> stringResource(
            if (goal.period == HabitPeriod.DAY) R.string.goal_habit_today else R.string.goal_habit_week,
            formatRate(p.current), target,
        )
        GoalKind.MEASURE -> stringResource(R.string.goal_measure_line, formatRate(p.current), target, goal.unit.orEmpty()).trim()
        GoalKind.MILESTONES -> stringResource(R.string.goal_steps_line, p.current.toInt(), p.target?.toInt() ?: 0)
    }
}

/** "12 days left · 25 questions a day needed · on track", or "Deadline passed"; null without a deadline. */
@Composable
fun goalPaceLine(item: GoalItem): String? {
    val pace = item.pace ?: return null
    if (pace.overdue) return stringResource(R.string.goal_overdue)
    return listOfNotNull(
        stringResource(R.string.goal_left, countText(R.plurals.count_days, pace.daysLeft)),
        pace.needPerDay?.takeIf { it > 0 }?.let { stringResource(R.string.goal_need_per_day, formatRate(it), goalUnit(item.goal)).trim() },
        stringResource(if (pace.onTrack) R.string.goal_on_track else R.string.goal_behind),
    ).joinToString(" · ")
}
