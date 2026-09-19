package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.RecurrenceUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Pure backstop against a corrupt row (e.g. recurrenceInterval somehow 0) — never hit by any
 *  realistic Week/Month/Year window, since every branch below fast-forwards straight to the
 *  relevant range rather than looping one step at a time from the anchor. */
private const val MAX_CANDIDATES = 10_000

/** ISO weekday numbers (1=Monday..7=Sunday), comma-separated — see PlannerEvent.recurrenceWeekdays. */
fun parseRecurrenceWeekdays(raw: String?): Set<DayOfWeek>? =
    raw?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it in 1..7 }
        ?.map { DayOfWeek.of(it) }
        ?.toSet()
        ?.takeIf { it.isNotEmpty() }

fun Set<DayOfWeek>.toRecurrenceWeekdaysString(): String =
    sortedBy { it.value }.joinToString(",") { it.value.toString() }

private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

/**
 * Every concrete occurrence date of [event] that falls within [rangeStart]..[rangeEnd]
 * (both inclusive). Fast-forwards mathematically to just before [rangeStart] rather than
 * looping from the anchor one step at a time, so an event created years ago still resolves
 * instantly for a one-week window.
 */
fun occurrencesInRange(event: PlannerEvent, rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDate> {
    if (rangeStart > rangeEnd) return emptyList()
    val anchor = event.startDate.toLocalDate()

    if (event.recurrenceUnit == null) {
        return if (anchor in rangeStart..rangeEnd) listOf(anchor) else emptyList()
    }

    val effectiveEnd = minOf(rangeEnd, event.recurrenceEndDate?.toLocalDate() ?: rangeEnd)
    val windowStart = maxOf(rangeStart, anchor)
    if (windowStart > effectiveEnd) return emptyList()

    val interval = event.recurrenceInterval.coerceAtLeast(1).toLong()
    val result = mutableListOf<LocalDate>()

    when (event.recurrenceUnit) {
        RecurrenceUnit.DAY -> {
            val daysFromAnchor = ChronoUnit.DAYS.between(anchor, windowStart)
            var cursor = anchor.plusDays(ceilDiv(daysFromAnchor, interval) * interval)
            while (!cursor.isAfter(effectiveEnd) && result.size <= MAX_CANDIDATES) {
                if (!cursor.isBefore(windowStart)) result.add(cursor)
                cursor = cursor.plusDays(interval)
            }
        }

        RecurrenceUnit.WEEK -> {
            val weekdays = parseRecurrenceWeekdays(event.recurrenceWeekdays) ?: setOf(anchor.dayOfWeek)
            val anchorWeekStart = mondayOf(anchor)
            val windowWeekStart = mondayOf(windowStart)
            val weeksFromAnchor = ChronoUnit.WEEKS.between(anchorWeekStart, windowWeekStart)
            var bucket = anchorWeekStart.plusWeeks(ceilDiv(weeksFromAnchor, interval) * interval)
            while (!bucket.isAfter(effectiveEnd) && result.size <= MAX_CANDIDATES) {
                for (day in weekdays.sortedBy { it.value }) {
                    val candidate = bucket.plusDays((day.value - 1).toLong())
                    if (candidate in windowStart..effectiveEnd) result.add(candidate)
                }
                bucket = bucket.plusWeeks(interval)
            }
        }

        RecurrenceUnit.MONTH -> {
            val anchorMonth = YearMonth.from(anchor)
            val windowMonth = YearMonth.from(windowStart)
            val monthsFromAnchor = ChronoUnit.MONTHS.between(anchorMonth, windowMonth)
            var candidateMonth = anchorMonth.plusMonths(ceilDiv(monthsFromAnchor, interval) * interval)
            while (!candidateMonth.atDay(1).isAfter(effectiveEnd) && result.size <= MAX_CANDIDATES) {
                val day = anchor.dayOfMonth.coerceAtMost(candidateMonth.lengthOfMonth())
                val candidate = candidateMonth.atDay(day)
                if (candidate in windowStart..effectiveEnd) result.add(candidate)
                candidateMonth = candidateMonth.plusMonths(interval)
            }
        }

        RecurrenceUnit.YEAR -> {
            val yearsFromAnchor = (windowStart.year - anchor.year).toLong().coerceAtLeast(0)
            var candidateYear = anchor.year + ceilDiv(yearsFromAnchor, interval) * interval
            while (candidateYear <= effectiveEnd.year.toLong() && result.size <= MAX_CANDIDATES) {
                val isFeb29 = anchor.monthValue == 2 && anchor.dayOfMonth == 29
                val day = if (isFeb29 && !Year.isLeap(candidateYear)) 28 else anchor.dayOfMonth
                val candidate = LocalDate.of(candidateYear.toInt(), anchor.monthValue, day)
                if (candidate in windowStart..effectiveEnd) result.add(candidate)
                candidateYear += interval
            }
        }
    }

    return result
}
