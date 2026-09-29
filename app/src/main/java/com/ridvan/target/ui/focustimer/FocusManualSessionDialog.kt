package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val MINUTE_MILLIS = 60_000L
private const val MAX_BREAK_DIGITS = 3

/** What the manual-session dialog produced. [startedAt]/[endedAt] are real instants; an end
 *  time at or before the start time is taken to run past midnight into the next day. */
data class ManualFocusSessionForm(
    val startedAt: Long,
    val endedAt: Long,
    val workMinutes: Int,
    val breakMinutes: Int,
    val courseId: Long?,
    val languageId: Long?,
    val topicId: Long?,
    val notes: String?,
)

/** A new history row for a session studied without the timer. No preset, no cycles. */
fun ManualFocusSessionForm.toNewSession(userId: Long): FocusSession = FocusSession(
    userId = userId,
    presetName = "",
    workMinutes = 0,
    breakMinutes = 0,
    courseId = courseId,
    topicId = topicId,
    languageId = languageId,
    startedAt = startedAt,
    endedAt = endedAt,
    cyclesCompleted = 0,
    totalWorkMinutes = workMinutes,
    totalBreakMinutes = breakMinutes,
    notes = notes,
    isManual = true,
)

/** Owner/topic pickers for the dialog's link section — each caller already has these on its ViewModel. */
data class ManualSessionLinkSource(
    val courses: List<Course>,
    val languages: List<Language>,
    val topicsForCourse: (Long) -> Flow<List<Topic>>,
    val topicsForLanguage: (Long) -> Flow<List<Topic>>,
)

/**
 * Logs (or, with [initial], re-times) a session studied without the timer: date, start/end time and
 * break minutes; work time is what's left. Adding also offers the Course/Language + Topic link and
 * notes; editing leaves those to the detail screen's own Change/Edit actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusManualSessionDialog(
    linkSource: ManualSessionLinkSource?,
    onConfirm: (ManualFocusSessionForm) -> Unit,
    onDismiss: () -> Unit,
    initial: FocusSession? = null,
    initialCourseId: Long? = null,
    initialLanguageId: Long? = null,
) {
    val now = remember { Calendar.getInstance() }
    val initialStart = remember { Calendar.getInstance().apply { timeInMillis = initial?.startedAt ?: (now.timeInMillis - 60 * MINUTE_MILLIS) } }
    val initialEnd = remember { Calendar.getInstance().apply { timeInMillis = initial?.endedAt ?: now.timeInMillis } }

    var dayStartMillis by remember { mutableStateOf(startOfDay(initialStart.timeInMillis)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val startState = rememberTimePickerState(
        initialHour = initialStart.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialStart.get(Calendar.MINUTE),
        is24Hour = true,
    )
    val endState = rememberTimePickerState(
        initialHour = initialEnd.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialEnd.get(Calendar.MINUTE),
        is24Hour = true,
    )
    var breakText by remember { mutableStateOf((initial?.totalBreakMinutes ?: 0).toString()) }

    var mode by remember {
        mutableStateOf(
            when {
                initialCourseId != null -> SessionLinkMode.COURSE
                initialLanguageId != null -> SessionLinkMode.LANGUAGE
                else -> SessionLinkMode.NONE
            },
        )
    }
    var courseId by remember { mutableStateOf(initialCourseId) }
    var languageId by remember { mutableStateOf(initialLanguageId) }
    var topicId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    val startMinute = startState.hour * 60 + startState.minute
    val endMinute = endState.hour * 60 + endState.minute
    val elapsedMinutes = (endMinute - startMinute).let { if (it <= 0) it + 24 * 60 else it }
    val breakMinutes = breakText.toIntOrNull() ?: 0
    val workMinutes = elapsedMinutes - breakMinutes
    val linkComplete = when (mode) {
        SessionLinkMode.NONE -> true
        SessionLinkMode.COURSE -> courseId != null
        SessionLinkMode.LANGUAGE -> languageId != null
    }
    val isValid = workMinutes > 0 && linkComplete

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.focus_manual_add_title else R.string.focus_manual_edit_title))
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.planner_field_date), style = MaterialTheme.typography.labelSmall)
                Text(
                    formatDate(dayStartMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(top = 2.dp).clickable { showDatePicker = true },
                )
                Text(
                    stringResource(R.string.planner_field_start_time),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TimeInput(state = startState)
                Text(stringResource(R.string.planner_field_end_time), style = MaterialTheme.typography.labelSmall)
                TimeInput(state = endState)
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { value -> breakText = value.filter { it.isDigit() }.take(MAX_BREAK_DIGITS) },
                    label = { Text(stringResource(R.string.focus_manual_break_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (workMinutes > 0) {
                        stringResource(
                            R.string.focus_manual_work_hint,
                            stringResource(R.string.duration_format, workMinutes / 60, workMinutes % 60),
                        )
                    } else {
                        stringResource(R.string.focus_manual_break_too_long)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (workMinutes > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )

                if (initial == null && linkSource != null) {
                    ManualLinkSection(
                        linkSource = linkSource,
                        mode = mode,
                        courseId = courseId,
                        languageId = languageId,
                        topicId = topicId,
                        onModeChange = {
                            mode = it
                            courseId = null
                            languageId = null
                            topicId = null
                        },
                        onCourseChange = { courseId = it; topicId = null },
                        onLanguageChange = { languageId = it; topicId = null },
                        onTopicChange = { topicId = it },
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.focus_session_notes_section)) },
                        placeholder = { Text(stringResource(R.string.focus_session_notes_hint)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val startedAt = dayStartMillis + startMinute * MINUTE_MILLIS
                    onConfirm(
                        ManualFocusSessionForm(
                            startedAt = startedAt,
                            endedAt = startedAt + elapsedMinutes * MINUTE_MILLIS,
                            workMinutes = workMinutes,
                            breakMinutes = breakMinutes,
                            courseId = courseId,
                            languageId = languageId,
                            topicId = topicId,
                            notes = notes.trim().ifEmpty { null },
                        ),
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )

    if (showDatePicker) {
        // Material3's DatePicker speaks UTC-midnight millis; convert to/from the local day here.
        val state = rememberDatePickerState(initialSelectedDateMillis = localDayToUtcMillis(dayStartMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dayStartMillis = utcMillisToLocalDay(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ManualLinkSection(
    linkSource: ManualSessionLinkSource,
    mode: SessionLinkMode,
    courseId: Long?,
    languageId: Long?,
    topicId: Long?,
    onModeChange: (SessionLinkMode) -> Unit,
    onCourseChange: (Long) -> Unit,
    onLanguageChange: (Long) -> Unit,
    onTopicChange: (Long?) -> Unit,
) {
    val topics by remember(mode, courseId, languageId) {
        when (mode) {
            SessionLinkMode.COURSE -> courseId?.let(linkSource.topicsForCourse) ?: flowOf(emptyList())
            SessionLinkMode.LANGUAGE -> languageId?.let(linkSource.topicsForLanguage) ?: flowOf(emptyList())
            SessionLinkMode.NONE -> flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Text(
        stringResource(R.string.focus_session_link_section),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
    SegmentedToggle(
        options = listOf(
            SegmentedToggleOption(SessionLinkMode.NONE, stringResource(R.string.common_none)),
            SegmentedToggleOption(SessionLinkMode.COURSE, stringResource(R.string.label_course)),
            SegmentedToggleOption(SessionLinkMode.LANGUAGE, stringResource(R.string.label_language)),
        ),
        selected = mode,
        onSelect = onModeChange,
        textStyle = MaterialTheme.typography.bodySmall,
    )
    when (mode) {
        SessionLinkMode.COURSE -> PickerField(
            label = stringResource(R.string.label_course),
            selectedLabel = linkSource.courses.firstOrNull { it.id == courseId }?.let { courseDisplayName(it.name) }
                ?: stringResource(R.string.common_select),
            options = linkSource.courses.map { it.id to courseDisplayName(it.name) },
            onSelect = onCourseChange,
        )
        SessionLinkMode.LANGUAGE -> PickerField(
            label = stringResource(R.string.label_language),
            selectedLabel = linkSource.languages.firstOrNull { it.id == languageId }?.name ?: stringResource(R.string.common_select),
            options = linkSource.languages.map { it.id to it.name },
            onSelect = onLanguageChange,
        )
        SessionLinkMode.NONE -> Unit
    }
    if (courseId != null || languageId != null) {
        PickerField(
            label = stringResource(R.string.label_link_to_topic),
            selectedLabel = topics.firstOrNull { it.id == topicId }?.name ?: stringResource(R.string.common_none),
            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.common_none)) + topics.map { it.id to it.name },
            onSelect = onTopicChange,
        )
    }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun localDayToUtcMillis(localDayStart: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localDayStart }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcMillisToLocalDay(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
