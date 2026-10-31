package com.ridvan.target.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import com.ridvan.target.ui.reading.isReading
import com.ridvan.target.ui.routines.runsOn
import com.ridvan.target.ui.sports.dayNumber
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

// Streaks are derived, never stored. Calendar-based (no java.time outside planner/notifications).

/** Days in a row: [current] runs up to today, or up to yesterday while today isn't done yet. */
data class Streak(val current: Int, val best: Int, val doneToday: Boolean) {
    companion object {
        val NONE = Streak(0, 0, false)
    }
}

/** Streak over local day numbers; [days] are the days that count. */
fun dayStreak(days: Set<Long>, today: Long): Streak {
    val doneToday = today in days
    var current = 0
    var day = if (doneToday) today else today - 1
    while (day in days) { current++; day-- }
    return Streak(current, bestRun(days.filter { it <= today }.sorted()), doneToday)
}

private fun bestRun(sortedDays: List<Long>): Int {
    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in sortedDays) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

/**
 * Streak over a schedule: [results] = whether each scheduled day was done, oldest first, the last one
 * being today when [lastIsToday]. Unscheduled days never appear, so they don't break it; an unfinished today
 * doesn't either.
 */
fun scheduledStreak(results: List<Boolean>, lastIsToday: Boolean): Streak {
    val doneToday = lastIsToday && results.lastOrNull() == true
    val counted = if (lastIsToday && !doneToday) results.dropLast(1) else results
    val current = counted.asReversed().takeWhile { it }.size
    var best = 0
    var run = 0
    results.forEach { done -> run = if (done) run + 1 else 0; best = maxOf(best, run) }
    return Streak(current, best, doneToday)
}

/**
 * Study days from (start time, minutes) entries: with [minMinutes] > 0 a day counts once its minutes reach it,
 * otherwise any entry counts.
 */
fun studyDays(entries: List<Pair<Long, Int>>, minMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Set<Long> =
    entries.groupBy({ dayNumber(it.first, zone) }, { it.second })
        .filterValues { minMinutes <= 0 || it.sum() >= minMinutes }
        .keys

fun readingDays(sessions: List<FocusSession>, zone: TimeZone = TimeZone.getDefault()): Set<Long> =
    sessions.filter { it.isReading }.map { dayNumber(it.startedAt, zone) }.toSet()

/** A routine's streak: its scheduled days (from creation to today) with every step ticked. */
fun routineStreak(
    routine: Routine,
    steps: List<RoutineStep>,
    completions: List<RoutineStepCompletion>,
    todayStart: Long,
): Streak {
    if (steps.isEmpty()) return Streak.NONE
    val stepIds = steps.map { it.id }.toSet()
    val doneCount = completions.filter { it.routineStepId in stepIds }.groupingBy { it.date }.eachCount()
    val day = Calendar.getInstance().apply {
        timeInMillis = routine.createdAt
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val results = mutableListOf<Boolean>()
    var lastIsToday = false
    while (day.timeInMillis <= todayStart) {
        val millis = day.timeInMillis
        if (routine.runsOn(millis)) {
            results += (doneCount[millis] ?: 0) >= steps.size
            lastIsToday = millis == todayStart
        }
        day.add(Calendar.DAY_OF_MONTH, 1)
    }
    return scheduledStreak(results, lastIsToday)
}

data class StudyReadingStreaks(val study: Streak, val reading: Streak)

/** The account's study streak (Focus/Flow/manual, practice logs, finished mock exams; reading excluded) and reading streak. */
fun studyReadingStreaksFlow(app: TargetApplication, userId: Long?): Flow<StudyReadingStreaks> {
    if (userId == null) return flowOf(StudyReadingStreaks(Streak.NONE, Streak.NONE))
    val db = app.database
    return combine(
        db.focusSessionDao().getAllWithLinksByUserId(userId),
        db.practiceLogDao().getAllForPlanner(userId),
        db.mockExamDao().getSummariesForUser(userId),
        app.preferences.studyStreakMinMinutes,
    ) { focus, practice, mocks, minMinutes ->
        val sessions = focus.map { it.session }
        val entries = sessions.filter { !it.isReading }.map { it.startedAt to it.totalWorkMinutes } +
            practice.map { it.practiceLog.loggedAt to it.practiceLog.durationMinutes } +
            mocks.map { it.mockExam.takenAt to it.mockExam.durationMinutes }
        val today = dayNumber(System.currentTimeMillis())
        StudyReadingStreaks(
            study = dayStreak(studyDays(entries, minMinutes), today),
            reading = dayStreak(readingDays(sessions), today),
        )
    }
}

/** "🔥 Study streak: 5 days", plus "do it today to keep it" while today isn't done; nothing when there's no streak. */
@Composable
fun StreakLine(streak: Streak, labelRes: Int, keepTodayRes: Int, modifier: Modifier = Modifier, showBest: Boolean = false) {
    if (streak.current == 0 && !(showBest && streak.best > 0)) return
    val parts = buildList {
        add(stringResource(labelRes, countText(R.plurals.count_days, streak.current)))
        if (streak.current > 0 && !streak.doneToday) add(stringResource(keepTodayRes))
        if (showBest) add(stringResource(R.string.streak_best, countText(R.plurals.count_days, streak.best)))
    }
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 6.dp),
    )
}
