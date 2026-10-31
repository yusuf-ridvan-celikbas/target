package com.ridvan.target.ui.planner

import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.dao.PracticeLogPlannerRow
import com.ridvan.target.data.local.dao.TripActivityWithTrip
import com.ridvan.target.data.local.dao.TripBookingWithTrip
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

    /** A routine on one of its days, with how many of its steps were ticked that day. */
    data class RoutineEntry(
        val routine: com.ridvan.target.data.local.entity.Routine,
        val done: Int,
        val total: Int,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    /** A logged workout on the day it started; [minuteOfDay] orders the day's rows. */
    data class WorkoutEntry(
        val workout: com.ridvan.target.data.local.entity.Workout,
        override val date: LocalDate,
        val minuteOfDay: Int,
    ) : PlannerAgendaItem

    data class ExamEntry(
        val examId: Long,
        val label: String,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    /** A planned mock exam on its scheduled day; [label] is the exam (and section) it belongs to. */
    data class MockExamPlannedEntry(
        val mockExam: com.ridvan.target.data.local.entity.MockExam,
        val label: String,
        override val date: LocalDate,
        val minuteOfDay: Int,
    ) : PlannerAgendaItem

    /** A goal on its deadline day. */
    data class GoalDeadlineEntry(
        val goal: com.ridvan.target.data.local.entity.Goal,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    data class SectionEntry(
        val examId: Long,
        val sectionId: Long,
        val label: String,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    /** One day of a trip (every day it covers), so the trip itself is visible on the calendar. */
    data class TripDayEntry(
        val tripId: Long,
        val tripName: String,
        val destination: String?,
        val dayNumber: Int,
        val dayCount: Int,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    data class TripActivityEntry(
        val item: TripActivityWithTrip,
        override val date: LocalDate,
    ) : PlannerAgendaItem

    /** A booking on its departure / check-in day. */
    data class TripBookingEntry(
        val item: TripBookingWithTrip,
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

    /** A finished mock exam on the day it was taken, counted as studied time. */
    data class MockExamFinishedEntry(
        val summary: com.ridvan.target.data.local.dao.MockExamSummary,
        val label: String,
        override val date: LocalDate,
        override val minuteOfDay: Int,
    ) : Studied {
        override val studiedMinutes: Int get() = summary.mockExam.durationMinutes
    }

    data class PracticeLogEntry(
        val row: PracticeLogPlannerRow,
        override val date: LocalDate,
        override val minuteOfDay: Int,
    ) : Studied {
        override val studiedMinutes: Int get() = row.practiceLog.durationMinutes
    }
}
