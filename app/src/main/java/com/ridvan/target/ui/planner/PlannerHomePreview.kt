package com.ridvan.target.ui.planner

import com.ridvan.target.ui.reading.BookProgress
import com.ridvan.target.ui.reading.pagesRead
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.dao.PlannerEventWithLinks
import com.ridvan.target.data.local.dao.PracticeLogPlannerRow
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import java.time.LocalDate

/**
 * One upcoming occurrence for Home's compact Planner preview card — just enough to render a
 * row and let the user tap into the full Planner. Keeps java.time fully inside ui/planner
 * (see PlannerDates.kt's own boundary note) by resolving down to an epoch-millis date before
 * handing anything back to ui/home.
 */
data class PlannerPreviewOccurrence(
    val event: PlannerEvent,
    val occurrenceDateMillis: Long,
    val isCompleted: Boolean,
    /** "HH:mm – HH:mm", null for an all-day event — resolved here (not left for ui/home to
     *  compute from event.startMinuteOfDay) purely to keep java.time formatting inside ui/planner. */
    val timeRangeLabel: String?,
)

/**
 * The next [maxItems] occurrences — events only, since Home's existing Upcoming card already
 * covers exam/section dates on its own — across the next [windowDays] days from today, soonest
 * first. Windowing (not just an item cap) keeps a single far-future occurrence from ever being
 * a candidate at all, rather than relying on sort+take to hide it.
 */
fun upcomingEventOccurrences(
    events: List<PlannerEventWithLinks>,
    completions: List<PlannerEventCompletion>,
    windowDays: Long,
    maxItems: Int,
): List<PlannerPreviewOccurrence> {
    val today = LocalDate.now()
    val rangeEnd = today.plusDays(windowDays)
    val completedKeys = completions.map { it.plannerEventId to it.occurrenceDate }.toSet()
    return events
        .flatMap { withLinks ->
            val event = withLinks.event
            occurrencesInRange(event, today, rangeEnd).map { date ->
                val millis = date.toStartOfDayMillis()
                PlannerPreviewOccurrence(
                    event = event,
                    occurrenceDateMillis = millis,
                    isCompleted = (event.id to millis) in completedKeys,
                    timeRangeLabel = timeRangeLabel(event),
                )
            }
        }
        .sortedWith(compareBy({ it.occurrenceDateMillis }, { it.event.startMinuteOfDay ?: Int.MAX_VALUE }))
        .take(maxItems)
}

/** Home's "Studied" section: today's sessions (in time order) plus today's and this week's totals. */
data class StudiedPreview(
    val today: List<PlannerAgendaItem.Studied>,
    val todayMinutes: Int,
    val weekMinutes: Int,
    val todayPages: Int = 0,
    val weekPages: Int = 0,
)

fun studiedPreview(
    focusSessions: List<FocusSessionWithLinks>,
    practiceLogs: List<PracticeLogPlannerRow>,
): StudiedPreview {
    val today = LocalDate.now()
    val week = studiedItemsInRange(focusSessions, practiceLogs, mondayOf(today)..today)
    val todayItems = week.filter { it.date == today }.sortedBy { it.minuteOfDay }
    return StudiedPreview(
        today = todayItems,
        todayMinutes = todayItems.sumOf { it.studiedMinutes },
        weekMinutes = week.sumOf { it.studiedMinutes },
        todayPages = todayItems.sumOf { it.pagesRead() },
        weekPages = week.sumOf { it.pagesRead() },
    )
}

private fun PlannerAgendaItem.Studied.pagesRead(): Int =
    (this as? PlannerAgendaItem.FocusSessionEntry)?.item?.session?.pagesRead() ?: 0

/** Home's Reading line: the book being read most recently (unfinished), if any. */
fun currentBook(books: List<BookProgress>, focusSessions: List<FocusSessionWithLinks>): BookProgress? {
    val unfinished = books.filterNot { it.book.isFinished }
    val lastReadAt = focusSessions
        .filter { it.session.bookId != null }
        .groupBy { it.session.bookId }
        .mapValues { (_, sessions) -> sessions.maxOf { it.session.startedAt } }
    // Most recently read first; a book never read yet only shows when nothing has been read.
    return unfinished.maxByOrNull { lastReadAt[it.book.id] ?: Long.MIN_VALUE }
}
