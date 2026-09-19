package com.ridvan.target.ui.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

@Composable
fun PlannerHomeScreen(
    shellNavigation: ShellNavigation,
    onExamClick: (Long) -> Unit,
    onSectionClick: (Long) -> Unit,
    onCourseClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: PlannerHomeViewModel = viewModel(),
) {
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val anchorDate by viewModel.anchorDate.collectAsStateWithLifecycle()
    val agendaItems by viewModel.agendaItems.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var selectedMonthDate by remember(anchorDate) { mutableStateOf(anchorDate) }
    var editingEvent by remember { mutableStateOf<PlannerEvent?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var courseIdForTopics by remember { mutableStateOf<Long?>(null) }

    val topicsFlow = remember(courseIdForTopics) {
        courseIdForTopics?.let { viewModel.topicsFor(it) } ?: flowOf(emptyList())
    }
    val topicsForSelectedCourse by topicsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    fun handleItemClick(item: PlannerAgendaItem) {
        when (item) {
            is PlannerAgendaItem.EventOccurrence -> {
                courseIdForTopics = item.event.courseId
                editingEvent = item.event
            }
            is PlannerAgendaItem.ExamEntry -> onExamClick(item.examId)
            is PlannerAgendaItem.SectionEntry -> onSectionClick(item.sectionId)
        }
    }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.PLANNER,
        title = stringResource(R.string.label_planner),
        floatingActionButton = {
            AddFab(onClick = {
                courseIdForTopics = null
                showAddDialog = true
            })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(PlannerViewMode.WEEK, stringResource(R.string.planner_view_week)),
                    SegmentedToggleOption(PlannerViewMode.MONTH, stringResource(R.string.planner_view_month)),
                    SegmentedToggleOption(PlannerViewMode.YEAR, stringResource(R.string.planner_view_year)),
                ),
                selected = viewMode,
                onSelect = viewModel::setViewMode,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                IconButton(onClick = viewModel::previousPeriod) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.cd_previous_period))
                }
                Text(
                    periodLabel(viewMode, anchorDate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = viewModel::nextPeriod) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.cd_next_period))
                }
            }

            when (viewMode) {
                PlannerViewMode.WEEK -> PlannerWeekAgenda(
                    weekStart = mondayOf(anchorDate),
                    items = agendaItems,
                    onToggleDone = viewModel::toggleOccurrenceDone,
                    onItemClick = ::handleItemClick,
                    onCourseClick = onCourseClick,
                    onTopicClick = onTopicClick,
                )
                PlannerViewMode.MONTH -> Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    PlannerMonthGrid(
                        anchorDate = anchorDate,
                        items = agendaItems,
                        selectedDate = selectedMonthDate,
                        onDateSelected = { date ->
                            selectedMonthDate = date
                            if (date.month != anchorDate.month || date.year != anchorDate.year) {
                                viewModel.jumpTo(date)
                            }
                        },
                    )
                    DayAgendaCard(
                        date = selectedMonthDate,
                        items = agendaItems.filter { it.date == selectedMonthDate },
                        onToggleDone = viewModel::toggleOccurrenceDone,
                        onItemClick = ::handleItemClick,
                        onCourseClick = onCourseClick,
                        onTopicClick = onTopicClick,
                    )
                }
                PlannerViewMode.YEAR -> PlannerYearList(
                    year = anchorDate.year,
                    items = agendaItems,
                    onItemClick = ::handleItemClick,
                )
            }
        }
    }

    if (showAddDialog) {
        PlannerEventFormDialog(
            courses = courses,
            topicsForSelectedCourse = topicsForSelectedCourse,
            onCourseSelected = { courseIdForTopics = it },
            onConfirm = { form ->
                viewModel.addEvent(form)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingEvent?.let { event ->
        PlannerEventFormDialog(
            courses = courses,
            topicsForSelectedCourse = topicsForSelectedCourse,
            onCourseSelected = { courseIdForTopics = it },
            initial = event,
            onConfirm = { form ->
                viewModel.updateEvent(event, form)
                editingEvent = null
            },
            onDismiss = { editingEvent = null },
            onDelete = {
                viewModel.deleteEvent(event)
                editingEvent = null
            },
        )
    }
}

private fun periodLabel(mode: PlannerViewMode, anchor: LocalDate): String = when (mode) {
    PlannerViewMode.WEEK -> {
        val start = mondayOf(anchor)
        val end = start.plusDays(6)
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val yearFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        "${start.format(formatter)} – ${end.format(yearFormatter)}"
    }
    PlannerViewMode.MONTH -> anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    PlannerViewMode.YEAR -> anchor.year.toString()
}
