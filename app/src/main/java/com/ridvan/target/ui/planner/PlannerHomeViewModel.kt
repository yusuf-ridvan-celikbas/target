package com.ridvan.target.ui.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.ui.shell.AgendaGroup
import com.ridvan.target.ui.shell.agendaGroupVisible
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.dao.PracticeLogPlannerRow
import com.ridvan.target.data.local.dao.PlannerEventWithLinks
import com.ridvan.target.data.local.dao.TripActivityWithTrip
import com.ridvan.target.data.local.dao.TripBookingWithTrip
import com.ridvan.target.data.local.dao.MockExamSummary
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.Trip
import java.time.temporal.ChronoUnit
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import com.ridvan.target.ui.routines.routineProgress
import com.ridvan.target.ui.routines.runsOn
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.examDisplayLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ridvan.target.ui.common.launchCreateTopic

enum class PlannerViewMode { WEEK, MONTH, YEAR }

private data class PlannerRawData(
    val events: List<PlannerEventWithLinks>,
    val completions: List<PlannerEventCompletion>,
    val exams: List<ExamWithType>,
    val sections: List<Section>,
    val plannedMockExams: List<MockExam>,
    val finishedMockExams: List<MockExamSummary>,
    val focusSessions: List<FocusSessionWithLinks>,
    val practiceLogs: List<PracticeLogPlannerRow>,
    val trips: List<Trip>,
    val tripActivities: List<TripActivityWithTrip>,
    val tripBookings: List<TripBookingWithTrip>,
    val routines: List<Routine>,
    val routineSteps: List<RoutineStep>,
    val routineCompletions: List<RoutineStepCompletion>,
    val workouts: List<com.ridvan.target.data.local.entity.Workout>,
)

private data class ExamData(
    val exams: List<ExamWithType>,
    val sections: List<Section>,
    val plannedMockExams: List<MockExam>,
    val finishedMockExams: List<MockExamSummary>,
)

class PlannerHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val database = targetApplication.database
    private val plannerEventDao = database.plannerEventDao()
    private val plannerEventCompletionDao = database.plannerEventCompletionDao()
    private val examDao = database.examDao()
    private val sectionDao = database.sectionDao()
    private val courseDao = database.courseDao()
    private val topicDao = database.topicDao()

    fun createTopic(name: String, courseId: Long, onCreated: (Long) -> Unit) =
        viewModelScope.launchCreateTopic(topicDao, name, courseId, null, onCreated)
    private val focusSessionDao = database.focusSessionDao()
    private val practiceLogDao = database.practiceLogDao()
    private val tripDao = database.tripDao()
    private val tripActivityDao = database.tripActivityDao()
    private val tripBookingDao = database.tripBookingDao()
    private val userId = targetApplication.preferences.currentUserId

    private val _viewMode = MutableStateFlow(PlannerViewMode.WEEK)
    val viewMode: StateFlow<PlannerViewMode> = _viewMode.asStateFlow()

    private val _anchorDate = MutableStateFlow(LocalDate.now())
    val anchorDate: StateFlow<LocalDate> = _anchorDate.asStateFlow()

    // What was actually studied (Focus Timer sessions + logged Practice Sessions), shown alongside the plans.
    private val studiedData = combine(
        userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { practiceLogDao.getAllForPlanner(it) } ?: flowOf(emptyList()),
        userId?.let { database.workoutDao().getByUserId(it) } ?: flowOf(emptyList()),
    ) { focus, practice, workouts -> Triple(focus, practice, workouts) }

    private val mockExamDao = database.mockExamDao()
    private val examData = combine(
        userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { sectionDao.getByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { mockExamDao.getScheduledForUser(it) } ?: flowOf(emptyList()),
        userId?.let { mockExamDao.getSummariesForUser(it) } ?: flowOf(emptyList()),
    ) { exams, sections, planned, finished -> ExamData(exams, sections, planned, finished) }

    private val tripData = combine(
        userId?.let { tripDao.getByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { tripActivityDao.getAllWithTripByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { tripBookingDao.getAllWithTripByUserId(it) } ?: flowOf(emptyList()),
    ) { trips, activities, bookings -> Triple(trips, activities, bookings) }

    private val routineDao = database.routineDao()
    private val routineData = combine(
        userId?.let { routineDao.getByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { routineDao.getAllStepsByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { routineDao.getAllCompletionsByUserId(it) } ?: flowOf(emptyList()),
    ) { routines, steps, completions -> Triple(routines, steps, completions) }

    private val tripAndRoutineData = combine(tripData, routineData) { trip, routine -> trip to routine }

    private val rawData = combine(
        userId?.let { plannerEventDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { plannerEventCompletionDao.getAllByUserId(it) } ?: flowOf(emptyList()),
        examData,
        studiedData,
        tripAndRoutineData,
    ) { events, completions, (exams, sections, planned, finished), (focus, practice, workouts), (trip, routine) ->
        val (trips, activities, bookings) = trip
        val (routines, routineSteps, routineCompletions) = routine
        PlannerRawData(events, completions, exams, sections, planned, finished, focus, practice, trips, activities, bookings, routines, routineSteps, routineCompletions, workouts)
    }

    private val modeAndDate = combine(_viewMode, _anchorDate) { mode, date -> mode to date }

    val agendaItems: StateFlow<List<PlannerAgendaItem>> = combine(rawData, modeAndDate, targetApplication.modules.enabled) { raw, (mode, date), modules ->
        buildAgendaItems(raw, visibleRange(mode, date)).filter { item -> item.agendaGroup()?.let { agendaGroupVisible(it, modules) } ?: true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun topicsFor(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)

    fun setViewMode(mode: PlannerViewMode) {
        _viewMode.value = mode
    }

    fun jumpTo(date: LocalDate) {
        _anchorDate.value = date
    }

    fun nextPeriod() {
        _anchorDate.value = when (_viewMode.value) {
            PlannerViewMode.WEEK -> _anchorDate.value.plusWeeks(1)
            PlannerViewMode.MONTH -> _anchorDate.value.plusMonths(1)
            PlannerViewMode.YEAR -> _anchorDate.value.plusYears(1)
        }
    }

    fun previousPeriod() {
        _anchorDate.value = when (_viewMode.value) {
            PlannerViewMode.WEEK -> _anchorDate.value.minusWeeks(1)
            PlannerViewMode.MONTH -> _anchorDate.value.minusMonths(1)
            PlannerViewMode.YEAR -> _anchorDate.value.minusYears(1)
        }
    }

    fun addEvent(form: PlannerEventFormResult) {
        val uid = userId ?: return
        viewModelScope.launch { plannerEventDao.insert(form.toEntity(uid)) }
    }

    fun updateEvent(existing: PlannerEvent, form: PlannerEventFormResult) {
        viewModelScope.launch {
            plannerEventDao.update(form.toEntity(existing.userId, id = existing.id, createdAt = existing.createdAt))
        }
    }

    fun deleteEvent(event: PlannerEvent) {
        viewModelScope.launch { plannerEventDao.delete(event) }
    }

    fun toggleOccurrenceDone(item: PlannerAgendaItem.EventOccurrence) {
        viewModelScope.launch {
            val millis = item.date.toStartOfDayMillis()
            if (item.isCompleted) {
                plannerEventCompletionDao.deleteByEventAndDate(item.event.id, millis)
            } else {
                plannerEventCompletionDao.insert(PlannerEventCompletion(plannerEventId = item.event.id, occurrenceDate = millis))
            }
        }
    }
}

private fun visibleRange(mode: PlannerViewMode, anchor: LocalDate): ClosedRange<LocalDate> = when (mode) {
    PlannerViewMode.WEEK -> {
        val start = mondayOf(anchor)
        start..start.plusDays(6)
    }
    PlannerViewMode.MONTH -> {
        val monthStart = mondayOf(anchor.withDayOfMonth(1))
        val monthEnd = mondayOf(YearMonth.from(anchor).atEndOfMonth()).plusDays(6)
        monthStart..monthEnd
    }
    PlannerViewMode.YEAR -> LocalDate.of(anchor.year, 1, 1)..LocalDate.of(anchor.year, 12, 31)
}

private fun buildAgendaItems(raw: PlannerRawData, range: ClosedRange<LocalDate>): List<PlannerAgendaItem> {
    val completedKeys = raw.completions.map { it.plannerEventId to it.occurrenceDate }.toSet()
    val eventItems = raw.events.flatMap { withLinks ->
        val event = withLinks.event
        occurrencesInRange(event, range.start, range.endInclusive).map { date ->
            PlannerAgendaItem.EventOccurrence(
                event = event,
                date = date,
                isCompleted = (event.id to date.toStartOfDayMillis()) in completedKeys,
                courseName = withLinks.courseName,
                topicName = withLinks.topicName,
            )
        }
    }

    val examLabels = raw.exams.associate { it.exam.id to examDisplayLabel(it.exam) }
    val examItems = raw.exams
        .filter { !it.exam.hasSections && it.exam.examDate != null }
        .mapNotNull { item ->
            val date = item.exam.examDate!!.toLocalDate()
            if (date !in range) return@mapNotNull null
            PlannerAgendaItem.ExamEntry(item.exam.id, examLabels[item.exam.id] ?: item.exam.name, date)
        }

    val sectionItems = raw.sections
        .filter { it.date != null }
        .mapNotNull { section ->
            val date = section.date!!.toLocalDate()
            if (date !in range) return@mapNotNull null
            val examLabel = examLabels[section.examId] ?: return@mapNotNull null
            PlannerAgendaItem.SectionEntry(section.examId, section.id, "$examLabel – ${section.name}", date)
        }

    val studiedItems = studiedItemsInRange(raw.focusSessions, raw.practiceLogs, range)

    return (eventItems + examItems + sectionItems + mockExamItemsInRange(raw, examLabels, range) + studiedItems + tripItemsInRange(raw, range) + routineItemsInRange(raw, range) + workoutItemsInRange(raw, range))
        .sortedBy { it.date }
}

/** Planned mock exams on their scheduled day (also in Year), finished ones on the day taken (Week/Month only, like workouts). */
private fun mockExamItemsInRange(raw: PlannerRawData, examLabels: Map<Long, String>, range: ClosedRange<LocalDate>): List<PlannerAgendaItem> {
    val sectionNames = raw.sections.associate { it.id to it.name }
    fun label(mock: MockExam): String {
        val exam = examLabels[mock.examId].orEmpty()
        return mock.sectionId?.let { sectionNames[it] }?.let { "$exam – $it" } ?: exam
    }
    val planned = raw.plannedMockExams.mapNotNull { mock ->
        val at = Instant.ofEpochMilli(mock.scheduledAt ?: return@mapNotNull null).atZone(ZoneId.systemDefault())
        if (at.toLocalDate() !in range) null
        else PlannerAgendaItem.MockExamPlannedEntry(mock, label(mock), at.toLocalDate(), at.hour * 60 + at.minute)
    }
    if (range.endInclusive.toEpochDay() - range.start.toEpochDay() > 60) return planned
    val finished = raw.finishedMockExams.mapNotNull { summary ->
        val at = Instant.ofEpochMilli(summary.mockExam.takenAt).atZone(ZoneId.systemDefault())
        if (at.toLocalDate() !in range) null
        else PlannerAgendaItem.MockExamFinishedEntry(summary, label(summary.mockExam), at.toLocalDate(), at.hour * 60 + at.minute)
    }
    return planned + finished
}

/** Workouts starting inside [range]; Week/Month only, so a busy year doesn't flood the Year list. */
private fun workoutItemsInRange(raw: PlannerRawData, range: ClosedRange<LocalDate>): List<PlannerAgendaItem> {
    if (raw.workouts.isEmpty() || range.endInclusive.toEpochDay() - range.start.toEpochDay() > 60) return emptyList()
    return raw.workouts.mapNotNull { workout ->
        val started = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault())
        val date = started.toLocalDate()
        if (date !in range) null else PlannerAgendaItem.WorkoutEntry(workout, date, started.hour * 60 + started.minute)
    }
}

/** Each active routine on each of its days inside [range], with that day's ticked-step count. */
private fun routineItemsInRange(raw: PlannerRawData, range: ClosedRange<LocalDate>): List<PlannerAgendaItem> {
    // Week/Month only — a daily routine would otherwise flood the Year list.
    if (raw.routines.isEmpty() || range.endInclusive.toEpochDay() - range.start.toEpochDay() > 60) return emptyList()
    val stepsBy = raw.routineSteps.groupBy { it.routineId }
    val days = generateSequence(range.start) { it.plusDays(1) }.takeWhile { it <= range.endInclusive }.toList()
    return days.flatMap { date ->
        val millis = date.toStartOfDayMillis()
        raw.routines.filter { it.runsOn(millis, date.dayOfWeek.value) }.map { routine ->
            val progress = routineProgress(stepsBy[routine.id].orEmpty(), raw.routineCompletions, millis)
            PlannerAgendaItem.RoutineEntry(routine, progress.done, progress.total, date)
        }
    }
}

/** Each day a trip covers inside [range], plus its activities and bookings (by departure / check-in day). */
private fun tripItemsInRange(raw: PlannerRawData, range: ClosedRange<LocalDate>): List<PlannerAgendaItem> {
    val days = raw.trips.flatMap { trip ->
        val start = trip.startDate.toLocalDate()
        val end = trip.endDate.toLocalDate()
        val dayCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
        generateSequence(maxOf(start, range.start)) { it.plusDays(1) }
            .takeWhile { it <= minOf(end, range.endInclusive) }
            .map { date ->
                PlannerAgendaItem.TripDayEntry(
                    trip.id, trip.name, trip.destination, ChronoUnit.DAYS.between(start, date).toInt() + 1, dayCount, date,
                )
            }
            .toList()
    }
    val activities = raw.tripActivities.mapNotNull { item ->
        item.activity.date.toLocalDate().takeIf { it in range }?.let { PlannerAgendaItem.TripActivityEntry(item, it) }
    }
    val bookings = raw.tripBookings.mapNotNull { item ->
        item.booking.startDate.toLocalDate().takeIf { it in range }?.let { PlannerAgendaItem.TripBookingEntry(item, it) }
    }
    return days + activities + bookings
}

/** Focus sessions (by start time) and Practice Sessions (by log time) falling in [range] —
 *  shared by the Planner agenda and Home's Planner card. */
internal fun studiedItemsInRange(
    focusSessions: List<FocusSessionWithLinks>,
    practiceLogs: List<PracticeLogPlannerRow>,
    range: ClosedRange<LocalDate>,
): List<PlannerAgendaItem.Studied> {
    val focusItems = focusSessions.mapNotNull { item ->
        val started = Instant.ofEpochMilli(item.session.startedAt).atZone(ZoneId.systemDefault())
        val date = started.toLocalDate()
        if (date !in range) return@mapNotNull null
        PlannerAgendaItem.FocusSessionEntry(item, date, started.hour * 60 + started.minute)
    }
    val practiceItems = practiceLogs.mapNotNull { row ->
        val logged = Instant.ofEpochMilli(row.practiceLog.loggedAt).atZone(ZoneId.systemDefault())
        val date = logged.toLocalDate()
        if (date !in range) return@mapNotNull null
        PlannerAgendaItem.PracticeLogEntry(row, date, logged.hour * 60 + logged.minute)
    }
    return focusItems + practiceItems
}

/** Which module owns an agenda row; null for the planner's own events. */
private fun PlannerAgendaItem.agendaGroup(): AgendaGroup? = when (this) {
    is PlannerAgendaItem.EventOccurrence -> null
    is PlannerAgendaItem.RoutineEntry -> AgendaGroup.ROUTINES
    is PlannerAgendaItem.WorkoutEntry -> AgendaGroup.SPORTS
    is PlannerAgendaItem.ExamEntry, is PlannerAgendaItem.SectionEntry, is PlannerAgendaItem.MockExamPlannedEntry -> AgendaGroup.EXAMS
    is PlannerAgendaItem.MockExamFinishedEntry -> AgendaGroup.PRACTICE
    is PlannerAgendaItem.TripDayEntry, is PlannerAgendaItem.TripActivityEntry, is PlannerAgendaItem.TripBookingEntry -> AgendaGroup.TRIPS
    is PlannerAgendaItem.FocusSessionEntry -> if (isReading) AgendaGroup.READING else AgendaGroup.FOCUS
    is PlannerAgendaItem.PracticeLogEntry -> AgendaGroup.PRACTICE
}
