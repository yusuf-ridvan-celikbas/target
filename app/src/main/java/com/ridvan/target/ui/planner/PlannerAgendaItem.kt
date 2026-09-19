package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.entity.PlannerEvent
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
}
