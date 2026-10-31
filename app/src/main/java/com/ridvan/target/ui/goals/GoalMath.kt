package com.ridvan.target.ui.goals

import com.ridvan.target.data.local.dao.MockExamSummary
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.GoalLog
import com.ridvan.target.data.local.entity.GoalMilestone
import com.ridvan.target.data.local.entity.HabitPeriod
import com.ridvan.target.data.local.entity.GoalSource
import com.ridvan.target.data.local.entity.PracticeLog
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.ui.reading.isReading
import com.ridvan.target.ui.reading.pagesRead
import com.ridvan.target.ui.sports.dayNumber
import com.ridvan.target.ui.sports.weekNumber
import java.util.TimeZone

// Pure goal maths (Calendar/day-number based, no java.time). Progress is always derived, never stored.

/** [current] of [target] (null for milestones without steps); [fraction] is 0..1. */
data class GoalProgress(val current: Double, val target: Double?, val fraction: Float, val reached: Boolean)

/** Everything an auto-filled goal can count, already scoped to the account. */
data class GoalSourceData(
    val sessions: List<FocusSession> = emptyList(),
    val practiceLogs: List<PracticeLog> = emptyList(),
    val mockExams: List<MockExamSummary> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val books: List<Book> = emptyList(),
)

/** What [source] adds up to from [since] (a day start) on. */
fun sourceValue(source: GoalSource, since: Long, data: GoalSourceData): Double = when (source) {
    GoalSource.STUDY_MINUTES ->
        (data.sessions.filter { !it.isReading && it.startedAt >= since }.sumOf { it.totalWorkMinutes } +
            data.practiceLogs.filter { it.loggedAt >= since }.sumOf { it.durationMinutes } +
            data.mockExams.filter { it.mockExam.takenAt >= since }.sumOf { it.mockExam.durationMinutes }).toDouble()
    GoalSource.QUESTIONS_ANSWERED ->
        (data.practiceLogs.filter { it.loggedAt >= since }.sumOf { it.questionCount ?: (it.solvedCount + it.unsolvedCount) } +
            data.mockExams.filter { it.mockExam.takenAt >= since }.sumOf { it.correctCount + it.wrongCount }).toDouble()
    GoalSource.PAGES_READ -> data.sessions.filter { it.startedAt >= since }.sumOf { it.pagesRead() }.toDouble()
    GoalSource.BOOKS_FINISHED -> data.books.count { book ->
        // No finish date is stored: a finished book counts when its last reading session falls in the window.
        book.isFinished && (data.sessions.filter { it.bookId == book.id }.maxOfOrNull { it.startedAt } ?: 0L) >= since
    }.toDouble()
    GoalSource.WORKOUTS -> data.workouts.count { it.startedAt >= since }.toDouble()
    GoalSource.WORKOUT_MINUTES -> data.workouts.filter { it.startedAt >= since }.sumOf { it.durationSeconds } / 60.0
    GoalSource.WORKOUT_KM -> data.workouts.filter { it.startedAt >= since }.sumOf { it.distanceMeters ?: 0 } / 1000.0
}

/** The latest reading of a MEASURE goal, or its start value. */
fun latestReading(goal: Goal, logs: List<GoalLog>): Double? =
    logs.maxWithOrNull(compareBy<GoalLog>({ it.date }, { it.createdAt }))?.amount ?: goal.startValue

/** Times a HABIT goal was done in the period containing [now]. */
fun habitCountInPeriod(goal: Goal, logs: List<GoalLog>, now: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val today = dayNumber(now, zone)
    return logs.count { log ->
        val day = dayNumber(log.date, zone)
        if (goal.period == HabitPeriod.WEEK) weekNumber(day) == weekNumber(today) else day == today
    }
}

fun goalProgress(
    goal: Goal,
    logs: List<GoalLog>,
    milestones: List<GoalMilestone>,
    data: GoalSourceData,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): GoalProgress {
    fun of(current: Double, target: Double?): GoalProgress {
        val fraction = if (target == null || target <= 0.0) 0f else (current / target).toFloat().coerceIn(0f, 1f)
        return GoalProgress(current, target, fraction, target != null && target > 0.0 && current >= target)
    }
    return when (goal.kind) {
        GoalKind.TARGET -> of(goal.source?.let { sourceValue(it, goal.startDate, data) } ?: logs.sumOf { it.amount }, goal.targetValue)
        GoalKind.HABIT -> of(habitCountInPeriod(goal, logs, now, zone).toDouble(), goal.targetValue)
        GoalKind.MILESTONES -> {
            val total = milestones.size
            of(milestones.count { it.doneAt != null }.toDouble(), total.toDouble().takeIf { total > 0 })
        }
        GoalKind.MEASURE -> {
            val start = goal.startValue
            val target = goal.targetValue
            val current = latestReading(goal, logs) ?: 0.0
            if (start == null || target == null || start == target) {
                GoalProgress(current, target, 0f, false)
            } else {
                val fraction = ((current - start) / (target - start)).toFloat().coerceIn(0f, 1f)
                GoalProgress(current, target, fraction, fraction >= 1f)
            }
        }
    }
}

/** How a goal with a deadline is doing: [needPerDay] of the unit still needed each day (TARGET only). */
data class GoalPace(val daysLeft: Int, val needPerDay: Double?, val onTrack: Boolean, val overdue: Boolean)

fun goalPace(goal: Goal, progress: GoalProgress, now: Long, zone: TimeZone = TimeZone.getDefault()): GoalPace? {
    val deadline = goal.deadline ?: return null
    if (goal.completedAt != null || progress.reached || goal.kind == GoalKind.HABIT) return null
    val today = dayNumber(now, zone)
    val start = dayNumber(goal.startDate, zone)
    val end = dayNumber(deadline, zone)
    val daysLeft = (end - today + 1).toInt()
    if (daysLeft <= 0) return GoalPace(0, null, onTrack = false, overdue = true)
    val totalDays = (end - start + 1).coerceAtLeast(1)
    val elapsed = (today - start).coerceIn(0, totalDays)
    val expected = elapsed.toFloat() / totalDays
    val needPerDay = if (goal.kind == GoalKind.TARGET && progress.target != null) {
        ((progress.target - progress.current) / daysLeft).coerceAtLeast(0.0)
    } else null
    return GoalPace(daysLeft, needPerDay, onTrack = progress.fraction >= expected, overdue = false)
}

/** Periods in a row in which a HABIT goal hit its target; the current period counts once hit and doesn't break it before. */
fun habitStreak(goal: Goal, logs: List<GoalLog>, now: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val times = goal.targetValue?.toInt()?.coerceAtLeast(1) ?: 1
    val periodOf: (Long) -> Long = { day -> if (goal.period == HabitPeriod.WEEK) weekNumber(day) else day }
    val counts = logs.groupingBy { periodOf(dayNumber(it.date, zone)) }.eachCount()
    val current = periodOf(dayNumber(now, zone))
    var period = if ((counts[current] ?: 0) >= times) current else current - 1
    var streak = 0
    while ((counts[period] ?: 0) >= times) { streak++; period-- }
    return streak
}
