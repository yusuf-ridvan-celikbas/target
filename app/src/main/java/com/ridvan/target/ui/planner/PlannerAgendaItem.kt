package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.dao.PracticeLogPlannerRow
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.ui.reading.isReading
import java.time.LocalDate

/**
 * What actually renders on a given day across Week/Month/Year — a sealed model so tap-routing
 * (open the edit dialog vs. navigate to an existing, read-only Exam/Section screen) is a
 * compiler-checked `when`, not a runtime flag check.
 */
sealed interface PlannerAgendaItem {
    val date: LocalDate

    data class EventOccurrence(
        val event: PlannerEvent,
        override val date: LocalDate,
        val isCompleted: Boolean,
        val courseName: String? = null,
        val topicName: String? = null,
    ) : PlannerAgendaItem

    data class ExamEntry(
        val examId: Long,
        val label: String,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    data class SectionEntry(
        val examId: Long,
        val sectionId: Long,
        val label: String,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    /** Already-studied time, shown read-only on the day it happened. [minuteOfDay] is when it started (Focus)
     *  or was logged (Practice Session), used to order the day's rows. */
    sealed interface Studied : PlannerAgendaItem {
        val minuteOfDay: Int
        val studiedMinutes: Int
        /** A Focus session spent reading a book — kept apart from study time everywhere it's totalled. */
        val isReading: Boolean get() = false
    }

    data class FocusSessionEntry(
        val item: FocusSessionWithLinks,
        override val date: LocalDate,
        override val minuteOfDay: Int,
    ) : Studied {
        override val studiedMinutes: Int get() = item.session.totalWorkMinutes
        override val isReading: Boolean get() = item.session.isReading
    }

    data class PracticeLogEntry(
        val row: PracticeLogPlannerRow,
        override val date: LocalDate,
        override val minuteOfDay: Int,
    ) : Studied {
        override val studiedMinutes: Int get() = row.practiceLog.durationMinutes
    }
}
