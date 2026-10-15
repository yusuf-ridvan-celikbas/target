package com.ridvan.target.ui.routines

import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import java.util.Calendar

/** ISO weekday numbers (1=Monday..7=Sunday) out of a routine's comma-separated repeat days. */
fun parseRepeatDays(raw: String): Set<Int> =
    raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

fun repeatDaysToString(days: Set<Int>): String = days.sorted().joinToString(",")

/** ISO weekday (1=Monday..7=Sunday) of the day containing [epochMillis]. */
fun isoDayOf(epochMillis: Long): Int {
    val dow = Calendar.getInstance().apply { timeInMillis = epochMillis }.get(Calendar.DAY_OF_WEEK)
    return (dow + 5) % 7 + 1
}

private fun dayStart(epochMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = epochMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * Whether the routine is on for the day starting at [dayStartMillis] (ISO weekday [isoDay]): active, repeating
 * on that weekday, and not before the day it was created.
 */
fun Routine.runsOn(dayStartMillis: Long, isoDay: Int = isoDayOf(dayStartMillis)): Boolean =
    isActive && isoDay in parseRepeatDays(repeatDays) && dayStartMillis >= dayStart(createdAt)

data class RoutineProgress(val done: Int, val total: Int)

/** Steps of one routine ticked on the day starting at [dayStartMillis]. */
fun routineProgress(steps: List<RoutineStep>, completions: List<RoutineStepCompletion>, dayStartMillis: Long): RoutineProgress {
    val ids = steps.map { it.id }.toSet()
    return RoutineProgress(completions.count { it.date == dayStartMillis && it.routineStepId in ids }, steps.size)
}
