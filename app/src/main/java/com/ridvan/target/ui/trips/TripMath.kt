package com.ridvan.target.ui.trips

import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripExpenseCategory
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/*
 * Plain trip maths over start-of-day millis, with Calendar (java.time stays in ui/planner and
 * data/notifications). Money is always stored as hundredths of the trip's currency ("minor"), whatever
 * that currency's own decimals are, so every sum is exact.
 */

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

fun addDays(dayStart: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

/** Calendar days from [fromDay] to [toDay] (both start-of-day); rounding absorbs a DST hour. */
fun daysBetween(fromDay: Long, toDay: Long): Int = ((toDay - fromDay).toDouble() / DAY_MILLIS).let { Math.round(it).toInt() }

fun tripDayCount(trip: Trip): Int = daysBetween(trip.startDate, trip.endDate).coerceAtLeast(0) + 1

/** Every day of the trip, start-of-day millis, first to last. */
fun tripDays(trip: Trip): List<Long> = (0 until tripDayCount(trip)).map { addDays(trip.startDate, it) }

/** 1-based day number of [dayStart] within the trip, or null when outside it. */
fun tripDayNumber(trip: Trip, dayStart: Long): Int? {
    val index = daysBetween(trip.startDate, dayStart)
    return (index + 1).takeIf { index >= 0 && index < tripDayCount(trip) }
}

sealed interface TripPhase {
    data class Upcoming(val daysUntil: Int) : TripPhase
    data class Ongoing(val dayNumber: Int, val dayCount: Int) : TripPhase
    data object Past : TripPhase
}

fun tripPhase(trip: Trip, todayStart: Long): TripPhase = when {
    todayStart < trip.startDate -> TripPhase.Upcoming(daysBetween(todayStart, trip.startDate))
    todayStart > trip.endDate -> TripPhase.Past
    else -> TripPhase.Ongoing(tripDayNumber(trip, todayStart) ?: 1, tripDayCount(trip))
}

/**
 * Parses what someone types as an amount: "12", "12.5", "12,50", "1.234,56", "1,234.56". The last '.' or ','
 * followed by one or two digits is the decimal mark; other separators are thousands marks. Null when it
 * isn't a non-negative number.
 */
fun parseAmountMinor(text: String): Long? {
    val cleaned = text.trim().replace(" ", "").replace(" ", "")
    if (cleaned.isEmpty() || cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null
    val lastSep = cleaned.indexOfLast { it == '.' || it == ',' }
    val decimals = if (lastSep >= 0) cleaned.substring(lastSep + 1) else ""
    val (whole, fraction) = if (lastSep >= 0 && decimals.length in 1..2) {
        cleaned.substring(0, lastSep) to decimals
    } else {
        cleaned to ""
    }
    val wholeDigits = whole.filter { it.isDigit() }
    if (wholeDigits.isEmpty() && fraction.isEmpty()) return null
    if (wholeDigits.length > 13) return null
    val wholeValue = wholeDigits.ifEmpty { "0" }.toLong()
    val fractionValue = fraction.padEnd(2, '0').toLong()
    return wholeValue * 100 + fractionValue
}

/** Prefill text for an amount field ("12,5" → "12,50"), in the device's decimal mark; whole numbers plain. */
fun amountInputText(minor: Long, locale: Locale = Locale.getDefault()): String {
    val whole = minor / 100
    val cents = minor % 100
    if (cents == 0L) return whole.toString()
    val mark = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return "$whole$mark${cents.toString().padStart(2, '0')}"
}

/** "€1,234.50" / "1.234,50 ₺" — decimals only when there are some. Unknown codes fall back to "12.50 XYZ". */
fun formatMoney(minor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val value = minor / 100.0
    val hasCents = minor % 100 != 0L
    val currency = runCatching { Currency.getInstance(currencyCode.uppercase(Locale.ROOT)) }.getOrNull()
    val format = if (currency != null) {
        NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
    } else {
        NumberFormat.getNumberInstance(locale)
    }
    format.minimumFractionDigits = if (hasCents) 2 else 0
    format.maximumFractionDigits = if (hasCents) 2 else 0
    val text = format.format(value)
    return if (currency != null) text else "$text ${currencyCode.uppercase(Locale.ROOT)}"
}

/** The device's own currency, e.g. "TRY" in Turkey — the default for a new trip. */
fun defaultCurrencyCode(locale: Locale = Locale.getDefault()): String =
    runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: "EUR"

/** A 3-letter code the JVM knows ("eur" → "EUR"), else null. */
fun normalizedCurrencyCode(text: String): String? {
    val code = text.trim().uppercase(Locale.ROOT)
    if (code.length != 3 || !code.all { it in 'A'..'Z' }) return null
    return runCatching { Currency.getInstance(code).currencyCode }.getOrNull()
}

data class TripBudgetSummary(
    val spentMinor: Long,
    val budgetMinor: Long?,
    /** Negative when over budget; null without a budget. */
    val remainingMinor: Long?,
    /** 0..1+, null without a budget (or a zero one). */
    val usedFraction: Float?,
    /** Only categories with spending, biggest first. */
    val byCategory: List<Pair<TripExpenseCategory, Long>>,
    /** Average spend per trip day so far (or over the whole trip once it's over). */
    val perDayMinor: Long,
)

fun budgetSummary(trip: Trip, expenses: List<TripExpense>, todayStart: Long): TripBudgetSummary {
    val spent = expenses.sumOf { it.amountMinor }
    val budget = trip.budgetMinor
    val elapsedDays = when (val phase = tripPhase(trip, todayStart)) {
        is TripPhase.Ongoing -> phase.dayNumber
        else -> tripDayCount(trip)
    }
    return TripBudgetSummary(
        spentMinor = spent,
        budgetMinor = budget,
        remainingMinor = budget?.let { it - spent },
        usedFraction = budget?.takeIf { it > 0 }?.let { spent.toFloat() / it },
        byCategory = expenses.groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amountMinor } }
            .sortedByDescending { it.second },
        perDayMinor = (spent.toDouble() / elapsedDays.coerceAtLeast(1)).roundToLong(),
    )
}

/** "1h 30m" style length between two minutes-of-day (wraps past midnight). */
fun minutesBetween(startMinute: Int, endMinute: Int): Int {
    val diff = endMinute - startMinute
    return if (diff > 0) diff else diff + 24 * 60
}

fun minuteLabel(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

/** "10:00 – 11:30", or just "10:00" without a length. */
fun timeRangeText(startMinute: Int?, durationMinutes: Int?): String? {
    startMinute ?: return null
    if (durationMinutes == null || durationMinutes <= 0) return minuteLabel(startMinute)
    return "${minuteLabel(startMinute)} – ${minuteLabel((startMinute + durationMinutes) % (24 * 60))}"
}
