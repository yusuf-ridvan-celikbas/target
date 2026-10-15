package com.ridvan.target.ui.sports

import com.ridvan.target.data.local.dao.LiftRow
import com.ridvan.target.data.local.entity.GoalPeriod
import com.ridvan.target.data.local.entity.GoalTargetType
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutSet
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Pure Sports maths: pace, totals, goals, streaks, personal bests. Calendar-based like the rest of the
// app outside the planner; every function takes the clock/zone so tests are deterministic.

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Seconds per kilometre, or per 100 m for swimming; null when there is no usable distance. */
fun paceSeconds(sport: Sport, durationSeconds: Int, distanceMeters: Int?): Double? {
    if (distanceMeters == null || distanceMeters <= 0 || durationSeconds <= 0) return null
    val unitMeters = if (sport == Sport.SWIMMING) 100.0 else 1000.0
    return durationSeconds / (distanceMeters / unitMeters)
}

/** "5:30" from a number of seconds (rounded to the nearest second). */
fun formatClock(seconds: Double): String {
    val total = Math.round(seconds).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** "1:05:09" for a long time, "42:10" for a short one. */
fun formatHms(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Kilometres with at most two decimals and no trailing zeros, locale decimal mark: "5", "10.25". */
fun formatKm(meters: Int): String {
    val text = BigDecimal(meters).divide(BigDecimal(1000), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return text.replace('.', DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator)
}

/** Parses "5", "5.2" or "5,2"; null when blank or not a positive number. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

fun minutesOf(durationSeconds: Int): Int = (durationSeconds + 30) / 60

// ---- Totals ---------------------------------------------------------------------------------------

data class SportTotals(val count: Int, val seconds: Long, val meters: Long) {
    val minutes: Int get() = ((seconds + 30) / 60).toInt()
    operator fun plus(other: SportTotals) = SportTotals(count + other.count, seconds + other.seconds, meters + other.meters)

    companion object {
        val ZERO = SportTotals(0, 0, 0)
    }
}

fun totalsOf(workouts: List<Workout>): SportTotals =
    SportTotals(workouts.size, workouts.sumOf { it.durationSeconds.toLong() }, workouts.sumOf { (it.distanceMeters ?: 0).toLong() })

/** Totals per sport, biggest time first; sports without workouts are left out. */
fun totalsBySport(workouts: List<Workout>): List<Pair<Sport, SportTotals>> =
    workouts.groupBy { it.sport }.map { (sport, list) -> sport to totalsOf(list) }.sortedByDescending { it.second.seconds }

enum class SportsPeriod { WEEK, MONTH }

private fun calendar(millis: Long, zone: TimeZone) = Calendar.getInstance(zone).apply {
    firstDayOfWeek = Calendar.MONDAY
    minimalDaysInFirstWeek = 4
    timeInMillis = millis
}

private fun Calendar.toDayStart(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

/** Start of the Monday-based week or calendar month holding [now]. */
fun periodStart(period: SportsPeriod, now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val c = calendar(now, zone).toDayStart()
    when (period) {
        SportsPeriod.WEEK -> c.add(Calendar.DAY_OF_YEAR, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        SportsPeriod.MONTH -> c.set(Calendar.DAY_OF_MONTH, 1)
    }
    return c.timeInMillis
}

/** Start of the period after the one starting at [start]. */
fun periodEnd(period: SportsPeriod, start: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val c = calendar(start, zone)
    if (period == SportsPeriod.WEEK) c.add(Calendar.DAY_OF_YEAR, 7) else c.add(Calendar.MONTH, 1)
    return c.timeInMillis
}

fun workoutsIn(workouts: List<Workout>, start: Long, end: Long): List<Workout> =
    workouts.filter { it.startedAt in start until end }

/** Minutes trained on each day of [start, end) — the bars of the Sports chart. */
fun dailyMinutes(workouts: List<Workout>, start: Long, end: Long, zone: TimeZone = TimeZone.getDefault()): List<Int> {
    val dayStarts = ArrayList<Long>()
    val c = calendar(start, zone)
    while (c.timeInMillis < end) {
        dayStarts += c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
    }
    dayStarts += end
    return (0 until dayStarts.size - 1).map { i ->
        minutesOf(workouts.filter { it.startedAt >= dayStarts[i] && it.startedAt < dayStarts[i + 1] }.sumOf { it.durationSeconds })
    }
}

// ---- Goals ----------------------------------------------------------------------------------------

data class GoalProgress(val current: Double, val target: Double) {
    val fraction: Float get() = if (target <= 0) 0f else (current / target).toFloat().coerceIn(0f, 1f)
    val reached: Boolean get() = target > 0 && current >= target
}

fun goalProgress(goal: SportGoal, workouts: List<Workout>, now: Long, zone: TimeZone = TimeZone.getDefault()): GoalProgress {
    val period = if (goal.period == GoalPeriod.WEEK) SportsPeriod.WEEK else SportsPeriod.MONTH
    val start = periodStart(period, now, zone)
    val inPeriod = workoutsIn(workouts, start, periodEnd(period, start, zone)).filter { goal.sport == null || it.sport == goal.sport }
    val current = when (goal.targetType) {
        GoalTargetType.DISTANCE_KM -> inPeriod.sumOf { (it.distanceMeters ?: 0).toLong() } / 1000.0
        GoalTargetType.MINUTES -> inPeriod.sumOf { it.durationSeconds.toLong() } / 60.0
        GoalTargetType.WORKOUTS -> inPeriod.size.toDouble()
    }
    return GoalProgress(current, goal.targetValue)
}

// ---- Streaks --------------------------------------------------------------------------------------

/** Local calendar day number (days since 1970-01-01 in [zone]). */
fun dayNumber(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Math.floorDiv(millis + zone.getOffset(millis), DAY_MILLIS)

/** Monday-based week number; weeks differ by exactly 1 when adjacent. */
fun weekNumber(day: Long): Long = Math.floorDiv(day + 3, 7L)

data class Streaks(val days: Int, val weeks: Int)

/**
 * Current streaks: consecutive days (weeks) with a workout, ending today (this week) — or yesterday
 * (last week) if nothing is logged yet today, so the streak isn't shown as lost mid-day.
 */
fun streaks(workouts: List<Workout>, now: Long, zone: TimeZone = TimeZone.getDefault()): Streaks {
    val days = workouts.map { dayNumber(it.startedAt, zone) }.toSet()
    val today = dayNumber(now, zone)
    var dayCount = 0
    var day = if (today in days) today else today - 1
    while (day in days) { dayCount++; day-- }
    val weeks = days.map { weekNumber(it) }.toSet()
    val thisWeek = weekNumber(today)
    var weekCount = 0
    var week = if (thisWeek in weeks) thisWeek else thisWeek - 1
    while (week in weeks) { weekCount++; week-- }
    return Streaks(dayCount, weekCount)
}

// ---- Personal bests -------------------------------------------------------------------------------

private const val FIVE_K_METERS = 5000

/** The fastest 5 km extrapolated from runs of at least 5 km (pace x 5): the workout and its 5 km seconds. */
fun fastestFiveK(workouts: List<Workout>): Pair<Workout, Double>? =
    workouts.filter { it.sport == Sport.RUNNING && (it.distanceMeters ?: 0) >= FIVE_K_METERS && it.durationSeconds > 0 }
        .map { it to paceSeconds(Sport.RUNNING, it.durationSeconds, it.distanceMeters)!! * 5 }
        .minByOrNull { it.second }

/** The workout with the longest distance in [sport]. */
fun longestDistance(workouts: List<Workout>, sport: Sport): Workout? =
    workouts.filter { it.sport == sport && (it.distanceMeters ?: 0) > 0 }.maxByOrNull { it.distanceMeters!! }

data class LiftBest(val name: String, val weightKg: Double, val reps: Int, val startedAt: Long, val workoutId: Long)

/** Heaviest set per exercise name (case-insensitive; more reps wins a tie), heaviest first. */
fun heaviestLifts(rows: List<LiftRow>): List<LiftBest> =
    rows.filter { it.weightKg > 0 && it.exerciseName.isNotBlank() }
        .groupBy { it.exerciseName.trim().lowercase() }
        .map { (_, list) ->
            val best = list.maxWith(compareBy<LiftRow>({ it.weightKg }, { it.reps }))
            LiftBest(best.exerciseName.trim(), best.weightKg, best.reps, best.startedAt, best.workoutId)
        }
        .sortedByDescending { it.weightKg }

// ---- Gym set display ------------------------------------------------------------------------------

/** Consecutive identical sets collapsed: 3 x 10 x 60 kg. */
data class SetGroup(val count: Int, val reps: Int, val weightKg: Double)

fun groupSets(sets: List<WorkoutSet>): List<SetGroup> {
    val groups = ArrayList<SetGroup>()
    sets.forEach { set ->
        val last = groups.lastOrNull()
        if (last != null && last.reps == set.reps && last.weightKg == set.weightKg) groups[groups.size - 1] = last.copy(count = last.count + 1)
        else groups += SetGroup(1, set.reps, set.weightKg)
    }
    return groups
}

/** "60", "62.5" — whole numbers without a decimal. */
fun formatWeight(kg: Double): String {
    val text = BigDecimal(kg).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return text.replace('.', DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator)
}
