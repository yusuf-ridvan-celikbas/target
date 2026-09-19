package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.dao.PlannerEventWithLinks
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
                )
            }
        }
        .sortedWith(compareBy({ it.occurrenceDateMillis }, { it.event.startMinuteOfDay ?: Int.MAX_VALUE }))
        .take(maxItems)
}
