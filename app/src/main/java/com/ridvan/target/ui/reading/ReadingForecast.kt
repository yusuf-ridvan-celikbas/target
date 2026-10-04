package com.ridvan.target.ui.reading

import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Finish-date forecasts and daily reading goals, worked out from logged reading sessions.
 * Plain functions over day-start millis (Calendar, not java.time — java.time stays in ui/planner),
 * so they can be unit-tested with a fixed "today".
 */

/** How many days the "recent pace" looks back over. */
const val PACE_WINDOW_DAYS = 14

/** A book's own speed is only trusted after this much reading; before that the overall speed is used. */
private const val MIN_BOOK_MINUTES_FOR_SPEED = 30

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun addDays(dayStartMillis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = dayStartMillis
    add(Calendar.DAY_OF_MONTH, days)
}.timeInMillis

/** Whole calendar days from [from] to [to] (both day starts); rounding keeps a DST shift from losing a day. */
fun daysBetween(from: Long, to: Long): Int = ((to - from).toDouble() / DAY_MILLIS).roundToLong().toInt()

/** [pagesPerDay]: recent pages per calendar day; [pagesPerHour]: reading speed. Either can be unknown. */
data class ReadingPace(val pagesPerDay: Double?, val pagesPerHour: Double?)

/**
 * The pace to forecast with. Pages per day: this book's pages over the last [PACE_WINDOW_DAYS] days
 * (or since its first session, if that's more recent), falling back to all reading in that window.
 * Pages per hour: this book's own speed once it has [MIN_BOOK_MINUTES_FOR_SPEED] minutes, else the overall speed.
 */
fun readingPace(bookSessions: List<FocusSession>, allReadingSessions: List<FocusSession>, today: Long): ReadingPace {
    val windowStart = addDays(today, -(PACE_WINDOW_DAYS - 1))

    fun perDay(sessions: List<FocusSession>): Double? {
        val first = sessions.minOfOrNull { it.startedAt } ?: return null
        val pages = sessions.filter { it.startedAt >= windowStart }.sumOf { it.pagesRead() }
        if (pages <= 0) return null
        val days = (daysBetween(maxOf(dayStart(first), windowStart), today) + 1).coerceIn(1, PACE_WINDOW_DAYS)
        return pages.toDouble() / days
    }

    val bookMinutes = bookSessions.sumOf { it.totalWorkMinutes }
    val speed = if (bookMinutes >= MIN_BOOK_MINUTES_FOR_SPEED) {
        pagesPerHour(bookSessions.sumOf { it.pagesRead() }, bookMinutes)
    } else {
        pagesPerHour(allReadingSessions.sumOf { it.pagesRead() }, allReadingSessions.sumOf { it.totalWorkMinutes })
    }
    return ReadingPace(perDay(bookSessions) ?: perDay(allReadingSessions), speed?.takeIf { it > 0 })
}

/** The day [remainingPages] runs out at [pagesPerDay], counting today as a reading day. */
fun finishDate(remainingPages: Int, pagesPerDay: Double, today: Long): Long {
    if (remainingPages <= 0 || pagesPerDay <= 0) return today
    val days = ceil(remainingPages / pagesPerDay).toInt()
    return addDays(today, days - 1)
}

/** Pages a day needed to finish [remainingPages] by [goalDate] (today included); null once the date has passed. */
fun pagesPerDayNeeded(remainingPages: Int, goalDate: Long, today: Long): Int? {
    val daysLeft = daysBetween(today, dayStart(goalDate)) + 1
    if (daysLeft < 1) return null
    return ceil(remainingPages.coerceAtLeast(0).toDouble() / daysLeft).toInt()
}

/** Roughly how many minutes [pages] take at [pagesPerHour]; null without a speed. */
fun minutesFor(pages: Int, pagesPerHour: Double?): Int? {
    if (pagesPerHour == null || pagesPerHour <= 0) return null
    return ceil(pages * 60 / pagesPerHour).toInt()
}

/** Today's share of a book's goal: [target] pages for today, [readToday] so far. [passed] once the goal date is gone. */
data class DailyGoal(val goalDate: Long, val target: Int, val readToday: Int, val passed: Boolean) {
    val remainingToday: Int get() = (target - readToday).coerceAtLeast(0)
    val done: Boolean get() = !passed && readToday >= target
}

/**
 * Today's goal for a book with a [Book.goalDate] and page count. The target is fixed for the whole day:
 * it's worked out from where the book stood at the start of today, so reading today counts toward it
 * instead of shrinking it.
 */
fun dailyGoal(book: Book, bookSessions: List<FocusSession>, today: Long): DailyGoal? {
    val goalDate = book.goalDate ?: return null
    val total = book.totalPages ?: return null
    if (book.isFinished) return null
    val (todays, earlier) = bookSessions.partition { it.startedAt >= today }
    val pageAtStartOfToday = earlier.mapNotNull { it.endPage }.maxOrNull()
        ?: todays.mapNotNull { it.startPage }.minOrNull()
        ?: 0
    val readToday = todays.sumOf { it.pagesRead() }
    val needed = pagesPerDayNeeded(total - pageAtStartOfToday, goalDate, today)
    return DailyGoal(goalDate = dayStart(goalDate), target = needed ?: 0, readToday = readToday, passed = needed == null)
}
