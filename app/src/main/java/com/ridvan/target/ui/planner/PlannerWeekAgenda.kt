package com.ridvan.target.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCategory
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatTime
import com.ridvan.target.ui.focustimer.focusSessionLinkLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerWeekAgenda(
    weekStart: LocalDate,
    items: List<PlannerAgendaItem>,
    onToggleDone: (PlannerAgendaItem.EventOccurrence) -> Unit,
    onItemClick: (PlannerAgendaItem) -> Unit,
    onCourseClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (offset in 0..6) {
            val date = weekStart.plusDays(offset.toLong())
            DayAgendaCard(
                date = date,
                items = items.filter { it.date == date },
                onToggleDone = onToggleDone,
                onItemClick = onItemClick,
                onCourseClick = onCourseClick,
                onTopicClick = onTopicClick,
            )
        }
    }
}

/** One GroupedCard per day (not per week) — an empty day collapses independently, and the
 *  same composable backs Monthly's tap-a-day panel, matching what that day shows in Week view. */
@Composable
fun DayAgendaCard(
    date: LocalDate,
    items: List<PlannerAgendaItem>,
    onToggleDone: (PlannerAgendaItem.EventOccurrence) -> Unit,
    onItemClick: (PlannerAgendaItem) -> Unit,
    onCourseClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        ) {
            Text(dayHeaderLabel(date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            // How much was actually studied that day (Focus work time + Practice Session time).
            val studied = items.filterIsInstance<PlannerAgendaItem.Studied>()
            if (studied.isNotEmpty()) {
                val minutes = studied.sumOf { it.studiedMinutes }
                Text(
                    stringResource(
                        R.string.planner_day_studied_total,
                        stringResource(R.string.duration_format, minutes / 60, minutes % 60),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        GroupedCard {
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.planner_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            } else {
                // Each activity type folds under its own header, in a fixed order; all start folded.
                val expanded = remember(date) { mutableStateMapOf<AgendaGroup, Boolean>() }
                val byGroup = items.groupBy { agendaGroupOf(it) }
                AgendaGroup.entries.forEach { group ->
                    val groupItems = byGroup[group] ?: return@forEach
                    val isExpanded = expanded[group] == true
                    AgendaGroupHeader(
                        group = group,
                        items = groupItems,
                        expanded = isExpanded,
                        onToggle = { expanded[group] = !isExpanded },
                    )
                    if (isExpanded) {
                        groupItems.sortedBy { sortMinute(it) }.forEach { item ->
                            AgendaRow(item, onToggleDone, onItemClick, onCourseClick, onTopicClick)
                        }
                    }
                }
            }
        }
    }
}

/** Declaration order is display order. */
private enum class AgendaGroup(val labelRes: Int, val icon: ImageVector) {
    EXAMS(R.string.planner_group_exams, Icons.AutoMirrored.Filled.Assignment),
    PLANS(R.string.planner_group_plans, Icons.Filled.Event),
    FOCUS(R.string.planner_group_focus, Icons.Filled.Timer),
    PRACTICE(R.string.planner_group_practice, Icons.Filled.Quiz),
}

private fun agendaGroupOf(item: PlannerAgendaItem): AgendaGroup = when (item) {
    is PlannerAgendaItem.ExamEntry, is PlannerAgendaItem.SectionEntry -> AgendaGroup.EXAMS
    is PlannerAgendaItem.EventOccurrence -> AgendaGroup.PLANS
    is PlannerAgendaItem.FocusSessionEntry -> AgendaGroup.FOCUS
    is PlannerAgendaItem.PracticeLogEntry -> AgendaGroup.PRACTICE
}

@Composable
private fun AgendaGroupHeader(
    group: AgendaGroup,
    items: List<PlannerAgendaItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val label = stringResource(group.labelRes)
    val studiedGroup = group == AgendaGroup.FOCUS || group == AgendaGroup.PRACTICE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            group.icon,
            contentDescription = null,
            tint = if (studiedGroup) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            stringResource(R.string.planner_group_header, label, items.size),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        if (studiedGroup) {
            val minutes = items.filterIsInstance<PlannerAgendaItem.Studied>().sumOf { it.studiedMinutes }
            Text(
                stringResource(R.string.duration_format, minutes / 60, minutes % 60),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(if (expanded) R.string.cd_collapse_x else R.string.cd_expand_x, label),
        )
    }
}

private fun sortMinute(item: PlannerAgendaItem): Int = when (item) {
    is PlannerAgendaItem.EventOccurrence -> item.event.startMinuteOfDay ?: Int.MAX_VALUE
    is PlannerAgendaItem.Studied -> item.minuteOfDay
    else -> Int.MAX_VALUE
}

@Composable
private fun AgendaRow(
    item: PlannerAgendaItem,
    onToggleDone: (PlannerAgendaItem.EventOccurrence) -> Unit,
    onItemClick: (PlannerAgendaItem) -> Unit,
    onCourseClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    when (item) {
        is PlannerAgendaItem.EventOccurrence -> {
            val event = item.event
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item) },
                leadingContent = {
                    Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleDone(item) })
                },
                headlineContent = { Text(event.title) },
                supportingContent = {
                    Column {
                        timeRangeLabel(event)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (event.category == PlannerEventCategory.BIRTHDAY) {
                            Text(birthdayAgeLabel(event, item.date), style = MaterialTheme.typography.bodySmall)
                        }
                        if (event.category == PlannerEventCategory.STUDY && event.courseId != null) {
                            StudyLinkLine(item, onCourseClick, onTopicClick)
                        }
                    }
                },
                trailingContent = if (event.recurrenceUnit != null) {
                    {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.cd_recurring_event),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else null,
            )
        }
        is PlannerAgendaItem.ExamEntry -> {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                headlineContent = { Text(item.label) },
            )
        }
        is PlannerAgendaItem.SectionEntry -> {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                headlineContent = { Text(item.label) },
            )
        }
        is PlannerAgendaItem.FocusSessionEntry -> {
            val session = item.item.session
            val workText = stringResource(R.string.duration_format, session.totalWorkMinutes / 60, session.totalWorkMinutes % 60)
            val breakText = stringResource(R.string.duration_format, session.totalBreakMinutes / 60, session.totalBreakMinutes % 60)
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item) },
                leadingContent = { StudiedIcon(Icons.Filled.Timer) },
                headlineContent = { Text(focusSessionLinkLabel(item.item) ?: session.presetName) },
                supportingContent = {
                    Column {
                        Text(
                            stringResource(
                                R.string.planner_studied_focus,
                                "${formatTime(session.startedAt)} – ${formatTime(session.endedAt)}",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.focustimer_history_row_subtitle, session.cyclesCompleted, workText, breakText),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
            )
        }
        is PlannerAgendaItem.PracticeLogEntry -> {
            val row = item.row
            val log = row.practiceLog
            val owner = row.courseName?.let { courseDisplayName(it) } ?: row.languageName
            val durationText = stringResource(R.string.duration_format, log.durationMinutes / 60, log.durationMinutes % 60)
            val blank = log.questionCount?.let { (it - log.solvedCount - log.unsolvedCount).coerceAtLeast(0) } ?: 0
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onItemClick(item) },
                leadingContent = { StudiedIcon(Icons.Filled.Quiz) },
                headlineContent = { Text(listOfNotNull(owner, row.topicName).joinToString(" · ")) },
                supportingContent = {
                    Column {
                        Text(
                            stringResource(R.string.planner_studied_practice, row.studyResourceName, formatTime(log.loggedAt)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.session_row_summary, log.testsSolved, log.solvedCount, log.unsolvedCount, blank, durationText),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
            )
        }
    }
}

/** Studied rows use the tertiary color, so work already done reads apart from plans at a glance. */
@Composable
private fun StudiedIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
}

@Composable
private fun StudyLinkLine(
    item: PlannerAgendaItem.EventOccurrence,
    onCourseClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    val courseId = item.event.courseId ?: return
    val topicId = item.event.topicId
    val label = listOfNotNull(item.courseName?.let { courseDisplayName(it) }, item.topicName).joinToString(" · ")
    if (label.isBlank()) return
    Text(
        label,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = Modifier.clickable {
            if (topicId != null) onTopicClick(topicId) else onCourseClick(courseId)
        },
    )
}

/** Not private — reused by PlannerHomePreview.kt's compact Home card so its "Today"/"Tomorrow"
 *  rows can show a time too, without duplicating this formatting or leaking java.time past ui/planner. */
internal fun timeRangeLabel(event: PlannerEvent): String? {
    val startMinute = event.startMinuteOfDay ?: return null
    val duration = (event.durationMinutes ?: 60).toLong()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = LocalTime.of(startMinute / 60, startMinute % 60)
    val end = start.plusMinutes(duration)
    return "${start.format(formatter)} – ${end.format(formatter)}"
}

@Composable
private fun birthdayAgeLabel(event: PlannerEvent, occurrenceDate: LocalDate): String {
    val age = occurrenceDate.year - event.startDate.toLocalDate().year
    return stringResource(R.string.planner_turns_age, age)
}

private fun dayHeaderLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
