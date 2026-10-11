package com.ridvan.target.ui.trips

import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripExpenseCategory
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripMathTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, dayOfMonth)
    }.timeInMillis

    private val trip = Trip(name = "Paris", startDate = day(2026, 10, 20), endDate = day(2026, 10, 24), currency = "EUR", budgetMinor = 100_000)

    @Test
    fun `trip days are inclusive of both ends`() {
        assertEquals(5, tripDayCount(trip))
        assertEquals(listOf(20, 21, 22, 23, 24), tripDays(trip).map { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_MONTH) })
        assertEquals(1, tripDayCount(trip.copy(endDate = trip.startDate)))
    }

    @Test
    fun `days across a month end and DST change stay whole`() {
        // Late October crosses the EU DST change; 1 Nov is still exactly the 13th day after 20 Oct.
        assertEquals(12, daysBetween(day(2026, 10, 20), day(2026, 11, 1)))
        assertEquals(day(2026, 11, 1), addDays(day(2026, 10, 20), 12))
    }

    @Test
    fun `day number is null outside the trip`() {
        assertEquals(1, tripDayNumber(trip, day(2026, 10, 20)))
        assertEquals(5, tripDayNumber(trip, day(2026, 10, 24)))
        assertNull(tripDayNumber(trip, day(2026, 10, 19)))
        assertNull(tripDayNumber(trip, day(2026, 10, 25)))
    }

    @Test
    fun `phase follows today`() {
        assertEquals(TripPhase.Upcoming(9), tripPhase(trip, day(2026, 10, 11)))
        assertEquals(TripPhase.Ongoing(3, 5), tripPhase(trip, day(2026, 10, 22)))
        assertEquals(TripPhase.Past, tripPhase(trip, day(2026, 10, 25)))
    }

    @Test
    fun `amounts parse with either decimal mark`() {
        assertEquals(1200L, parseAmountMinor("12"))
        assertEquals(1250L, parseAmountMinor("12.5"))
        assertEquals(1250L, parseAmountMinor("12,50"))
        assertEquals(123456L, parseAmountMinor("1.234,56"))
        assertEquals(123456L, parseAmountMinor("1,234.56"))
        assertEquals(123400L, parseAmountMinor("1.234"))
        assertEquals(50L, parseAmountMinor(",5"))
        assertNull(parseAmountMinor(""))
        assertNull(parseAmountMinor("-5"))
        assertNull(parseAmountMinor("abc"))
    }

    @Test
    fun `amount prefill drops whole cents`() {
        assertEquals("12", amountInputText(1200, Locale.US))
        assertEquals("12.05", amountInputText(1205, Locale.US))
        assertEquals("12,50", amountInputText(1250, Locale.forLanguageTag("tr-TR")))
    }

    @Test
    fun `money formats with the trip currency`() {
        assertEquals("€12", formatMoney(1200, "EUR", Locale.US))
        assertEquals("€12.50", formatMoney(1250, "EUR", Locale.US))
        assertEquals("1,234.50 XYZ".length, formatMoney(123450, "XYZ", Locale.US).length)
    }

    @Test
    fun `currency codes are validated`() {
        assertEquals("EUR", normalizedCurrencyCode(" eur "))
        assertNull(normalizedCurrencyCode("EURO"))
        assertNull(normalizedCurrencyCode("QQQ"))
    }

    @Test
    fun `budget summary sums by category and per day`() {
        val expenses = listOf(
            TripExpense(tripId = 0, title = "Hotel", amountMinor = 40_000, category = TripExpenseCategory.STAY, date = trip.startDate),
            TripExpense(tripId = 0, title = "Lunch", amountMinor = 2_000, category = TripExpenseCategory.FOOD, date = trip.startDate),
            TripExpense(tripId = 0, title = "Dinner", amountMinor = 4_000, category = TripExpenseCategory.FOOD, date = trip.startDate),
        )
        val summary = budgetSummary(trip, expenses, day(2026, 10, 21))
        assertEquals(46_000L, summary.spentMinor)
        assertEquals(54_000L, summary.remainingMinor)
        assertEquals(0.46f, summary.usedFraction!!, 0.0001f)
        assertEquals(listOf(TripExpenseCategory.STAY to 40_000L, TripExpenseCategory.FOOD to 6_000L), summary.byCategory)
        assertEquals(23_000L, summary.perDayMinor) // day 2 of the trip
        assertEquals(9_200L, budgetSummary(trip, expenses, day(2026, 11, 1)).perDayMinor) // over 5 days once it's over
    }

    @Test
    fun `time ranges wrap past midnight`() {
        assertEquals("23:30 – 00:30", timeRangeText(23 * 60 + 30, 60))
        assertEquals("09:00", timeRangeText(9 * 60, null))
        assertEquals(90, minutesBetween(10 * 60, 11 * 60 + 30))
        assertEquals(60, minutesBetween(23 * 60 + 30, 30))
    }
}
