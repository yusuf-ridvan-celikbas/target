package com.ridvan.target.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCategory
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.courseDisplayName
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
        Text(
            dayHeaderLabel(date),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        GroupedCard {
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.planner_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            } else {
                items.sortedBy { sortMinute(it) }.forEach { item ->
                    AgendaRow(item, onToggleDone, onItemClick, onCourseClick, onTopicClick)
                }
            }
        }
    }
}

private fun sortMinute(item: PlannerAgendaItem): Int =
    (item as? PlannerAgendaItem.EventOccurrence)?.event?.startMinuteOfDay ?: Int.MAX_VALUE

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
    }
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

private fun timeRangeLabel(event: PlannerEvent): String? {
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
