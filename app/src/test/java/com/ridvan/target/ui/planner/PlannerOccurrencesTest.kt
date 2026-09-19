package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.RecurrenceUnit
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerOccurrencesTest {

    private fun event(
        anchor: LocalDate,
        unit: RecurrenceUnit? = null,
        interval: Int = 1,
        weekdays: Set<DayOfWeek>? = null,
        endDate: LocalDate? = null,
    ) = PlannerEvent(
        title = "Test",
        startDate = anchor.toStartOfDayMillis(),
        recurrenceUnit = unit,
        recurrenceInterval = interval,
        recurrenceWeekdays = weekdays?.toRecurrenceWeekdaysString(),
        recurrenceEndDate = endDate?.toStartOfDayMillis(),
    )

    @Test
    fun `one-time event only appears on its own date`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val e = event(anchor)
        assertEquals(listOf(anchor), occurrencesInRange(e, anchor, anchor))
        assertEquals(emptyList<LocalDate>(), occurrencesInRange(e, anchor.plusDays(1), anchor.plusDays(5)))
        assertEquals(emptyList<LocalDate>(), occurrencesInRange(e, anchor.minusDays(5), anchor.minusDays(1)))
    }

    @Test
    fun `daily recurrence every 2 days fast-forwards correctly into a distant window`() {
        val anchor = LocalDate.of(2020, 1, 1)
        val e = event(anchor, RecurrenceUnit.DAY, interval = 2)
        // A window years later must still land only on anchor-parity days, resolved instantly.
        val rangeStart = LocalDate.of(2026, 6, 1)
        val rangeEnd = LocalDate.of(2026, 6, 10)
        val result = occurrencesInRange(e, rangeStart, rangeEnd)
        for (date in result) {
            val daysSinceAnchor = java.time.temporal.ChronoUnit.DAYS.between(anchor, date)
            assertEquals(0L, daysSinceAnchor % 2)
        }
        assertEquals(5, result.size) // 10-day window, every other day
    }

    @Test
    fun `weekly recurrence on specific weekdays across three weeks`() {
        val anchor = LocalDate.of(2026, 9, 1) // a Tuesday
        val e = event(anchor, RecurrenceUnit.WEEK, interval = 1, weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
        val result = occurrencesInRange(e, anchor, anchor.plusWeeks(3))
        assertEquals(true, result.all { it.dayOfWeek == DayOfWeek.TUESDAY || it.dayOfWeek == DayOfWeek.THURSDAY })
        // 3 full weeks + the anchor week itself = up to 4 Tue/Thu pairs, minus any before anchor.
        assertEquals(listOf(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 3),
            LocalDate.of(2026, 9, 8),
            LocalDate.of(2026, 9, 10),
            LocalDate.of(2026, 9, 15),
            LocalDate.of(2026, 9, 17),
            LocalDate.of(2026, 9, 22),
        ), result)
    }

    @Test
    fun `weekly recurrence every 2 weeks skips the off week`() {
        val anchor = LocalDate.of(2026, 9, 1) // Tuesday, week A
        val e = event(anchor, RecurrenceUnit.WEEK, interval = 2)
        val result = occurrencesInRange(e, anchor, anchor.plusWeeks(4))
        assertEquals(listOf(anchor, anchor.plusWeeks(2), anchor.plusWeeks(4)), result)
    }

    @Test
    fun `monthly recurrence clips day-of-month to shorter months`() {
        val anchor = LocalDate.of(2026, 1, 31)
        val e = event(anchor, RecurrenceUnit.MONTH, interval = 1)
        val result = occurrencesInRange(e, anchor, LocalDate.of(2026, 4, 30))
        assertEquals(listOf(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28), // clipped, never rolls into March
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30), // clipped
        ), result)
    }

    @Test
    fun `yearly recurrence clips Feb 29 to Feb 28 on non-leap years`() {
        val anchor = LocalDate.of(2024, 2, 29) // 2024 is a leap year
        val e = event(anchor, RecurrenceUnit.YEAR, interval = 1)
        val result = occurrencesInRange(e, anchor, LocalDate.of(2028, 12, 31))
        assertEquals(listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 2, 28),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2027, 2, 28),
            LocalDate.of(2028, 2, 29), // 2028 is a leap year again
        ), result)
    }

    @Test
    fun `recurrenceEndDate cuts off future occurrences`() {
        val anchor = LocalDate.of(2026, 1, 1)
        val e = event(anchor, RecurrenceUnit.DAY, interval = 1, endDate = LocalDate.of(2026, 1, 3))
        val result = occurrencesInRange(e, anchor, LocalDate.of(2026, 1, 10))
        assertEquals(listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 1, 3),
        ), result)
    }

    @Test
    fun `no occurrences before the anchor date even with recurrence`() {
        val anchor = LocalDate.of(2026, 5, 10)
        val e = event(anchor, RecurrenceUnit.DAY, interval = 1)
        val result = occurrencesInRange(e, anchor.minusDays(5), anchor.minusDays(1))
        assertEquals(emptyList<LocalDate>(), result)
    }
}
