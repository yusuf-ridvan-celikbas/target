package com.ridvan.target.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.PlannerEvent
import com.ridvan.target.data.local.entity.PlannerEventCategory
import com.ridvan.target.data.local.entity.RecurrenceUnit
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

data class PlannerEventFormResult(
    val title: String,
    val notes: String?,
    val category: PlannerEventCategory,
    val date: LocalDate,
    val startMinuteOfDay: Int?,
    val durationMinutes: Int?,
    val recurrenceUnit: RecurrenceUnit?,
    val recurrenceInterval: Int,
    val recurrenceWeekdays: Set<DayOfWeek>,
    val recurrenceEndDate: LocalDate?,
    val courseId: Long?,
    val topicId: Long?,
)

fun PlannerEventFormResult.toEntity(userId: Long?, id: Long = 0, createdAt: Long = System.currentTimeMillis()): PlannerEvent {
    // Birthday is always implicitly yearly — the dialog hides the recurrence controls
    // entirely for this category rather than showing a locked-but-visible one.
    val effectiveUnit = if (category == PlannerEventCategory.BIRTHDAY) RecurrenceUnit.YEAR else recurrenceUnit
    val effectiveInterval = if (category == PlannerEventCategory.BIRTHDAY) 1 else recurrenceInterval.coerceAtLeast(1)
    return PlannerEvent(
        id = id,
        userId = userId,
        title = title,
        notes = notes,
        category = category,
        startDate = date.toStartOfDayMillis(),
        startMinuteOfDay = startMinuteOfDay,
        durationMinutes = durationMinutes,
        recurrenceUnit = effectiveUnit,
        recurrenceInterval = effectiveInterval,
        recurrenceWeekdays = recurrenceWeekdays.takeIf { effectiveUnit == RecurrenceUnit.WEEK && it.isNotEmpty() }
            ?.toRecurrenceWeekdaysString(),
        recurrenceEndDate = recurrenceEndDate?.toStartOfDayMillis(),
        courseId = courseId.takeIf { category == PlannerEventCategory.STUDY },
        topicId = topicId.takeIf { category == PlannerEventCategory.STUDY },
        createdAt = createdAt,
    )
}

// Material3's DatePicker communicates in UTC-midnight millis regardless of device timezone —
// distinct from PlannerDates.kt's toLocalDate()/toStartOfDayMillis(), which use the system
// default zone for every other Planner date (storage, occurrence math). These two are only
// for reading/writing the DatePicker's own selectedDateMillis correctly.
private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerEventFormDialog(
    courses: List<Course>,
    topicsForSelectedCourse: List<Topic>,
    onCourseSelected: (Long?) -> Unit,
    initial: PlannerEvent? = null,
    onConfirm: (PlannerEventFormResult) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: PlannerEventCategory.GENERAL) }
    var date by remember { mutableStateOf(initial?.startDate?.toLocalDate() ?: LocalDate.now()) }
    var hasTime by remember { mutableStateOf(initial?.startMinuteOfDay != null) }
    var durationText by remember { mutableStateOf(initial?.durationMinutes?.toString() ?: "") }
    var recurrenceUnit by remember { mutableStateOf(initial?.recurrenceUnit?.takeIf { initial.category != PlannerEventCategory.BIRTHDAY }) }
    var intervalText by remember { mutableStateOf((initial?.recurrenceInterval ?: 1).toString()) }
    var weekdays by remember { mutableStateOf(parseRecurrenceWeekdays(initial?.recurrenceWeekdays) ?: emptySet()) }
    var endDate by remember { mutableStateOf(initial?.recurrenceEndDate?.toLocalDate()) }
    var selectedCourseId by remember { mutableStateOf(initial?.courseId) }
    var selectedTopicId by remember { mutableStateOf(initial?.topicId) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val initialMinuteOfDay = initial?.startMinuteOfDay ?: (9 * 60)
    val timePickerState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )

    val isValid = title.isNotBlank() && (category != PlannerEventCategory.STUDY || selectedCourseId != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.planner_dialog_new_title else R.string.planner_dialog_edit_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.planner_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.planner_field_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(PlannerEventCategory.GENERAL, stringResource(R.string.planner_category_general)),
                        SegmentedToggleOption(PlannerEventCategory.BIRTHDAY, stringResource(R.string.planner_category_birthday)),
                        SegmentedToggleOption(PlannerEventCategory.STUDY, stringResource(R.string.planner_category_study)),
                    ),
                    selected = category,
                    onSelect = {
                        category = it
                        if (it != PlannerEventCategory.STUDY) {
                            selectedCourseId = null
                            selectedTopicId = null
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )

                PlannerDateField(
                    label = stringResource(
                        if (category == PlannerEventCategory.BIRTHDAY) R.string.planner_field_birthday_date else R.string.planner_field_date
                    ),
                    value = date,
                    onClick = { showDatePicker = true },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.planner_field_set_time), modifier = Modifier.weight(1f))
                    Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                }
                if (hasTime) {
                    TimeInput(state = timePickerState, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { if (it.all(Char::isDigit)) durationText = it },
                        label = { Text(stringResource(R.string.planner_field_duration_minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                if (category != PlannerEventCategory.BIRTHDAY) {
                    RecurrenceUnitField(selected = recurrenceUnit, onSelect = { recurrenceUnit = it })
                    if (recurrenceUnit != null) {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { if (it.all(Char::isDigit)) intervalText = it },
                            label = { Text(stringResource(R.string.planner_field_interval)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        if (recurrenceUnit == RecurrenceUnit.WEEK) {
                            Text(
                                stringResource(R.string.planner_field_weekdays),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                DayOfWeek.entries.sortedBy { it.value }.forEach { day ->
                                    FilterChip(
                                        selected = day in weekdays,
                                        onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day },
                                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            PlannerDateField(
                                label = stringResource(R.string.planner_field_end_date),
                                value = endDate,
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.weight(1f),
                            )
                            if (endDate != null) {
                                IconButton(onClick = { endDate = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_end_date))
                                }
                            }
                        }
                    }
                }

                if (category == PlannerEventCategory.STUDY) {
                    CourseField(
                        courses = courses,
                        selectedId = selectedCourseId,
                        onSelect = {
                            selectedCourseId = it
                            selectedTopicId = null
                            onCourseSelected(it)
                        },
                    )
                    if (selectedCourseId != null) {
                        TopicField(
                            topics = topicsForSelectedCourse,
                            selectedId = selectedTopicId,
                            onSelect = { selectedTopicId = it },
                        )
                    }
                }

                if (onDelete != null) {
                    Text(
                        stringResource(
                            if (initial?.recurrenceUnit != null) R.string.planner_delete_series else R.string.planner_delete_event
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable(onClick = onDelete),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        PlannerEventFormResult(
                            title = title.trim(),
                            notes = notes.trim().ifBlank { null },
                            category = category,
                            date = date,
                            startMinuteOfDay = if (hasTime) timePickerState.hour * 60 + timePickerState.minute else null,
                            durationMinutes = durationText.toIntOrNull(),
                            recurrenceUnit = recurrenceUnit,
                            recurrenceInterval = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            recurrenceWeekdays = weekdays,
                            recurrenceEndDate = endDate,
                            courseId = selectedCourseId,
                            topicId = selectedTopicId,
                        )
                    )
                },
            ) { Text(stringResource(if (initial == null) R.string.common_add else R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it.toLocalDateUtc() }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate?.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDate = it.toLocalDateUtc() }
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun PlannerDateField(label: String, value: LocalDate?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onClick)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value?.let { formatDate(it.toStartOfDayMillis()) } ?: stringResource(R.string.common_tap_to_set),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RecurrenceUnitField(selected: RecurrenceUnit?, onSelect: (RecurrenceUnit?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.planner_field_repeats), style = MaterialTheme.typography.labelSmall)
            Text(recurrenceUnitLabel(selected), style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.planner_recurrence_none)) },
                onClick = { onSelect(null); expanded = false },
            )
            RecurrenceUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(recurrenceUnitLabel(unit)) },
                    onClick = { onSelect(unit); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun recurrenceUnitLabel(unit: RecurrenceUnit?): String = when (unit) {
    null -> stringResource(R.string.planner_recurrence_none)
    RecurrenceUnit.DAY -> stringResource(R.string.planner_recurrence_day)
    RecurrenceUnit.WEEK -> stringResource(R.string.planner_recurrence_week)
    RecurrenceUnit.MONTH -> stringResource(R.string.planner_recurrence_month)
    RecurrenceUnit.YEAR -> stringResource(R.string.planner_recurrence_year)
}

@Composable
private fun CourseField(courses: List<Course>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.planner_field_course), style = MaterialTheme.typography.labelSmall)
            Text(
                courses.firstOrNull { it.id == selectedId }?.let { courseDisplayName(it.name) } ?: stringResource(R.string.common_select),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text(courseDisplayName(course.name)) },
                    onClick = { onSelect(course.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun TopicField(topics: List<Topic>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.planner_field_topic), style = MaterialTheme.typography.labelSmall)
            Text(
                topics.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.common_not_set),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_not_set)) },
                onClick = { onSelect(null); expanded = false },
            )
            topics.forEach { topic ->
                DropdownMenuItem(
                    text = { Text(topic.name) },
                    onClick = { onSelect(topic.id); expanded = false },
                )
            }
        }
    }
}
