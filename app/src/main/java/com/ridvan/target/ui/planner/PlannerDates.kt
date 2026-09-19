package com.ridvan.target.ui.planner

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The only boundary in the app between the Long-epoch-millis convention every other
 * screen uses and java.time (added specifically for the Planner's recurrence math —
 * see PlannerOccurrences.kt). Nothing outside ui/planner touches java.time.
 */
fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.toStartOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/** The Monday on/before [date] — weeks are always bucketed Monday-start, regardless of locale. */
fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
