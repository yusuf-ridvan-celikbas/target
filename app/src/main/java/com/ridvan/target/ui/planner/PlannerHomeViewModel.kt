package com.ridvan.target.ui.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.PlannerEventWithLinks
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.examDisplayLabel
import java.time.LocalDate
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

enum class PlannerViewMode { WEEK, MONTH, YEAR }

private data class PlannerRawData(
    val events: List<PlannerEventWithLinks>,
    val completions: List<PlannerEventCompletion>,
    val exams: List<ExamWithType>,
    val sections: List<Section>,
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
    private val userId = targetApplication.preferences.currentUserId

    private val _viewMode = MutableStateFlow(PlannerViewMode.WEEK)
    val viewMode: StateFlow<PlannerViewMode> = _viewMode.asStateFlow()

    private val _anchorDate = MutableStateFlow(LocalDate.now())
    val anchorDate: StateFlow<LocalDate> = _anchorDate.asStateFlow()

    private val rawData = combine(
        userId?.let { plannerEventDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { plannerEventCompletionDao.getAllByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { sectionDao.getByUserId(it) } ?: flowOf(emptyList()),
    ) { events, completions, exams, sections -> PlannerRawData(events, completions, exams, sections) }

    private val modeAndDate = combine(_viewMode, _anchorDate) { mode, date -> mode to date }

    val agendaItems: StateFlow<List<PlannerAgendaItem>> = combine(rawData, modeAndDate) { raw, (mode, date) ->
        buildAgendaItems(raw, visibleRange(mode, date))
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

    return (eventItems + examItems + sectionItems).sortedBy { it.date }
}
