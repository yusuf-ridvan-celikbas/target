package com.ridvan.target.ui.trips

import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripExpense
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/* Pure helpers for currency choice/conversion, the day timeline and the shareable summary. */

// ---- Currency ----

data class CurrencyOption(val code: String, val name: String, val symbol: String) {
    /** "EUR · Euro · €" */
    val label: String get() = "$code · $name · $symbol"
}

private val COMMON_CURRENCIES = listOf("TRY", "EUR", "USD", "GBP")

/** All currencies the JVM knows (offline), common ones first, the rest by code. */
fun currencyOptions(locale: Locale = Locale.getDefault()): List<CurrencyOption> {
    val all = Currency.getAvailableCurrencies().map {
        CurrencyOption(it.currencyCode, it.getDisplayName(locale), it.getSymbol(locale))
    }.sortedBy { it.code }
    val common = COMMON_CURRENCIES.mapNotNull { c -> all.firstOrNull { it.code == c } }
    return common + all.filter { it.code !in COMMON_CURRENCIES }
}

fun filterCurrencies(options: List<CurrencyOption>, query: String): List<CurrencyOption> {
    val q = query.trim()
    if (q.isEmpty()) return options
    return options.filter { it.code.contains(q, true) || it.name.contains(q, true) }
}

/** Parses a typed rate ("36.5", "36,5"); null unless a positive number. */
fun parseExchangeRate(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }

/** Rate prefill/display text without trailing zeros ("36.5", "40"). */
fun rateText(rate: Double): String = java.math.BigDecimal(rate.toString()).stripTrailingZeros().toPlainString()

/** Converts minor units of the trip currency into home-currency minor units. */
fun convertMinor(minor: Long, rate: Double): Long = (minor * rate).roundToLong()

/** The trip's conversion, only when both parts are set and differ from the trip currency. */
fun Trip.conversion(): Pair<String, Double>? {
    val home = homeCurrency ?: return null
    val rate = exchangeRate ?: return null
    return if (home != currency && rate > 0) home to rate else null
}

/** "€10.00" or "€10.00 (≈ 365 ₺)" when the trip has a home currency. */
fun Trip.moneyWithHome(minor: Long): String {
    val base = formatMoney(minor, currency)
    val (home, rate) = conversion() ?: return base
    return "$base (≈ ${formatMoney(convertMinor(minor, rate), home)})"
}

// ---- Day timeline ----

sealed interface TimelineEntry {
    val minute: Int?

    data class Booking(val booking: TripBooking, val isEnd: Boolean) : TimelineEntry {
        override val minute: Int? get() = if (isEnd) booking.endMinuteOfDay else booking.startMinuteOfDay
    }

    data class Activity(val activity: TripActivity) : TimelineEntry {
        override val minute: Int? get() = activity.startMinuteOfDay
    }
}

/** One day's bookings (start, and end when on a later day) and activities, by time; untimed last (stable). */
fun dayTimeline(day: Long, bookings: List<TripBooking>, activities: List<TripActivity>): List<TimelineEntry> {
    val entries = buildList {
        bookings.filter { it.startDate == day }.forEach { add(TimelineEntry.Booking(it, isEnd = false)) }
        bookings.filter { it.endDate == day && it.endDate != it.startDate }.forEach { add(TimelineEntry.Booking(it, isEnd = true)) }
        activities.filter { it.date == day }.forEach { add(TimelineEntry.Activity(it)) }
    }
    return entries.sortedBy { it.minute ?: Int.MAX_VALUE }
}

// ---- Share summary ----

/** Already-localized words for the summary. */
data class SummaryLabels(
    val dayHeader: (index: Int, date: String) -> String,
    val nothingPlanned: String,
    val bookings: String,
    val confirmation: String,
    val budget: String,
    val spent: String,
    val ofBudget: String,
    val remaining: String,
    val arrival: String,
    val checkOut: String,
    val bookingType: (TripBooking) -> String,
)

fun tripSummaryText(
    trip: Trip,
    activities: List<TripActivity>,
    bookings: List<TripBooking>,
    expenses: List<TripExpense>,
    labels: SummaryLabels,
    date: (Long) -> String,
): String = buildString {
    appendLine(trip.name)
    appendLine(listOfNotNull(trip.destination, "${date(trip.startDate)} - ${date(trip.endDate)}").joinToString(" | "))
    trip.notes?.let { appendLine(it) }
    tripDays(trip).forEachIndexed { index, day ->
        appendLine()
        appendLine(labels.dayHeader(index + 1, date(day)))
        val entries = dayTimeline(day, bookings, activities)
        if (entries.isEmpty()) appendLine("  ${labels.nothingPlanned}")
        entries.forEach { e ->
            val time = e.minute?.let { minuteLabel(it) }
            val line = when (e) {
                is TimelineEntry.Activity -> listOfNotNull(
                    timeRangeText(e.activity.startMinuteOfDay, e.activity.durationMinutes), e.activity.title, e.activity.place.name,
                )
                is TimelineEntry.Booking -> listOfNotNull(
                    time,
                    (if (e.isEnd) "${e.booking.title} (${if (e.booking.type.isTransport) labels.arrival else labels.checkOut})" else e.booking.title),
                    listOfNotNull(e.booking.place.name, e.booking.arrivalPlace.name).joinToString(" -> ").ifBlank { null },
                )
            }
            appendLine("  - " + line.joinToString(" "))
        }
    }
    if (bookings.isNotEmpty()) {
        appendLine()
        appendLine(labels.bookings)
        bookings.forEach { b ->
            val parts = listOfNotNull(
                labels.bookingType(b),
                b.title,
                b.provider,
                date(b.startDate) + (b.startMinuteOfDay?.let { " " + minuteLabel(it) } ?: ""),
                b.confirmationCode?.let { "${labels.confirmation}: $it" },
            )
            appendLine("  - " + parts.joinToString(", "))
        }
    }
    val spent = expenses.sumOf { it.amountMinor }
    if (spent > 0 || trip.budgetMinor != null) {
        appendLine()
        appendLine(labels.budget)
        appendLine("  ${labels.spent}: ${trip.moneyWithHome(spent)}")
        trip.budgetMinor?.let {
            appendLine("  ${labels.ofBudget}: ${trip.moneyWithHome(it)}")
            appendLine("  ${labels.remaining}: ${trip.moneyWithHome(it - spent)}")
        }
    }
}.trimEnd()
