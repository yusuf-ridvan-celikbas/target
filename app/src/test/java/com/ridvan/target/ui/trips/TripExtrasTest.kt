package com.ridvan.target.ui.trips

import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripExpense
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripExtrasTest {
    private val day1 = 1_700_000_000_000L - 1_700_000_000_000L % 86_400_000L
    private val day2 = addDays(day1, 1)
    private val trip = Trip(name = "Berlin", destination = "Germany", startDate = day1, endDate = day2, currency = "EUR", budgetMinor = 100_00, homeCurrency = "TRY", exchangeRate = 36.5)

    @Before fun locale() = Locale.setDefault(Locale.US)

    @Test fun parsesRates() {
        assertEquals(36.5, parseExchangeRate("36,5")!!, 0.0)
        assertEquals(40.0, parseExchangeRate(" 40 ")!!, 0.0)
        assertNull(parseExchangeRate("0"))
        assertNull(parseExchangeRate("abc"))
        assertEquals("36.5", rateText(36.5))
        assertEquals("40", rateText(40.0))
    }

    @Test fun convertsMinorUnits() {
        assertEquals(36_500L, convertMinor(10_00, 36.5))
        assertEquals(2L, convertMinor(3, 0.5))
    }

    @Test fun conversionNeedsDifferentCurrencyAndRate() {
        assertEquals("TRY", trip.conversion()!!.first)
        assertNull(trip.copy(homeCurrency = "EUR").conversion())
        assertNull(trip.copy(exchangeRate = null).conversion())
        assertTrue(trip.moneyWithHome(10_00).contains("≈"))
        assertTrue(!trip.copy(homeCurrency = null, exchangeRate = null).moneyWithHome(10_00).contains("≈"))
    }

    @Test fun currenciesCommonFirstAndFilterable() {
        val options = currencyOptions(Locale.US)
        assertEquals(listOf("TRY", "EUR", "USD", "GBP"), options.take(4).map { it.code })
        assertEquals(1, options.count { it.code == "JPY" })
        assertEquals(listOf("EUR"), filterCurrencies(options, "euro").map { it.code }.filter { it == "EUR" })
        assertTrue(options.first { it.code == "EUR" }.label.startsWith("EUR · "))
    }

    private fun act(title: String, minute: Int?, date: Long = day1) = TripActivity(tripId = 1, title = title, date = date, startMinuteOfDay = minute)
    private fun booking(title: String, minute: Int?, end: Long? = null, endMinute: Int? = null) =
        TripBooking(tripId = 1, type = TripBookingType.FLIGHT, title = title, startDate = day1, startMinuteOfDay = minute, endDate = end, endMinuteOfDay = endMinute)

    @Test fun timelineOrdersByTimeUntimedLast() {
        val entries = dayTimeline(
            day1,
            listOf(booking("Late flight", 20 * 60), booking("No time", null)),
            listOf(act("Breakfast", 8 * 60), act("Wander", null), act("Other day", 9 * 60, day2)),
        )
        val names = entries.map { (it as? TimelineEntry.Activity)?.activity?.title ?: (it as TimelineEntry.Booking).booking.title }
        assertEquals(listOf("Breakfast", "Late flight", "No time", "Wander"), names)
    }

    @Test fun timelineIncludesCheckOutOnLaterDay() {
        val b = booking("Hotel", 14 * 60, end = day2, endMinute = 11 * 60)
        val d2 = dayTimeline(day2, listOf(b), listOf(act("Museum", 10 * 60, day2)))
        assertEquals(2, d2.size)
        assertTrue((d2[1] as TimelineEntry.Booking).isEnd)
    }

    @Test fun summaryContainsEverything() {
        val labels = SummaryLabels(
            dayHeader = { i, d -> "Day $i - $d" }, nothingPlanned = "Nothing planned", bookings = "Bookings",
            confirmation = "Conf", budget = "Budget", spent = "Spent", ofBudget = "Budget total", remaining = "Left",
            arrival = "Arrival", checkOut = "Check-out", bookingType = { it.type.name },
        )
        val text = tripSummaryText(
            trip,
            listOf(act("Breakfast", 8 * 60)),
            listOf(booking("TK 1823", 9 * 60).copy(confirmationCode = "ABC123")),
            listOf(TripExpense(tripId = 1, title = "Taxi", amountMinor = 20_00, date = day1)),
            labels,
        ) { "D$it" }
        assertTrue(text.startsWith("Berlin\nGermany | "))
        assertTrue(text.contains("Day 1 - "))
        assertTrue(text.contains("Nothing planned"))
        assertTrue(text.indexOf("08:00") < text.indexOf("09:00"))
        assertTrue(text.contains("Conf: ABC123"))
        assertTrue(text.contains("Spent: €20"))
        assertTrue(text.contains("Left: €80"))
    }
}
