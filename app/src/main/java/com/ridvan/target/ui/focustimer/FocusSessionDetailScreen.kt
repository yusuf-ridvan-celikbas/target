package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatTime
import kotlinx.coroutines.flow.flowOf

private enum class SessionLinkMode { NONE, COURSE, LANGUAGE }

/** One Focus Timer session: when it ran, what it covered, what it's linked to, and the user's notes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionDetailScreen(
    onBack: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: FocusSessionDetailViewModel = viewModel(),
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var showLinkDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.session?.presetName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }, enabled = item != null) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_focus_session))
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = item ?: return@Scaffold
        val session = current.session
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // When it ran — date plus the exact start and end times.
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.focus_session_time_section), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.focus_session_date, formatDate(session.startedAt)), modifier = Modifier.padding(top = 8.dp))
                    val endLabel = if (formatDate(session.endedAt) == formatDate(session.startedAt)) {
                        formatTime(session.endedAt)
                    } else {
                        "${formatDate(session.endedAt)} ${formatTime(session.endedAt)}"
                    }
                    Text(stringResource(R.string.focus_session_time_range, formatTime(session.startedAt), endLabel))
                    val elapsedMinutes = ((session.endedAt - session.startedAt) / 60_000L).toInt().coerceAtLeast(0)
                    Text(
                        stringResource(
                            R.string.focus_session_total,
                            stringResource(R.string.duration_format, elapsedMinutes / 60, elapsedMinutes % 60),
                        ),
                    )
                }
            }

            // What the timer recorded.
            GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.focus_session_summary_section), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.focus_session_preset,
                            session.presetName,
                            stringResource(R.string.focustimer_preset_row_subtitle, session.workMinutes, session.breakMinutes),
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        stringResource(
                            R.string.focustimer_history_row_subtitle,
                            session.cyclesCompleted,
                            stringResource(R.string.duration_format, session.totalWorkMinutes / 60, session.totalWorkMinutes % 60),
                            stringResource(R.string.duration_format, session.totalBreakMinutes / 60, session.totalBreakMinutes % 60),
                        ),
                    )
                }
            }

            // What it's linked to — tappable through to the real course/language/topic.
            GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.focus_session_link_section),
                        action = stringResource(R.string.focus_session_change),
                        onAction = { showLinkDialog = true },
                    )
                    val linkLabel = focusSessionLinkLabel(current)
                    if (linkLabel == null) {
                        Text(stringResource(R.string.focustimer_history_not_linked), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            linkLabel,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                when {
                                    session.topicId != null -> onTopicClick(session.topicId)
                                    session.courseId != null -> onCourseClick(session.courseId)
                                    session.languageId != null -> onLanguageClick(session.languageId)
                                }
                            },
                        )
                    }
                }
            }

            // The user's own annotation, e.g. "Studied Dativ".
            GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.focus_session_notes_section),
                        action = stringResource(R.string.focus_session_edit),
                        onAction = { showNotesDialog = true },
                    )
                    val notes = session.notes
                    if (notes.isNullOrBlank()) {
                        Text(stringResource(R.string.focus_session_no_notes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(notes)
                    }
                }
            }
        }
    }

    val current = item
    if (showLinkDialog && current != null) {
        LinkSessionDialog(
            item = current,
            courses = courses,
            languages = languages,
            viewModel = viewModel,
            onConfirm = { courseId, languageId, topicId ->
                viewModel.updateLink(courseId, languageId, topicId)
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false },
        )
    }

    if (showNotesDialog && current != null) {
        NotesDialog(
            initial = current.session.notes.orEmpty(),
            onConfirm = {
                viewModel.updateNotes(it)
                showNotesDialog = false
            },
            onDismiss = { showNotesDialog = false },
        )
    }

    if (showDeleteConfirm && current != null) {
        FocusSessionDeleteDialog(
            item = current,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun LinkSessionDialog(
    item: FocusSessionWithLinks,
    courses: List<Course>,
    languages: List<Language>,
    viewModel: FocusSessionDetailViewModel,
    onConfirm: (courseId: Long?, languageId: Long?, topicId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = item.session
    var mode by remember {
        mutableStateOf(
            when {
                session.courseId != null -> SessionLinkMode.COURSE
                session.languageId != null -> SessionLinkMode.LANGUAGE
                else -> SessionLinkMode.NONE
            },
        )
    }
    var courseId by remember { mutableStateOf(session.courseId) }
    var languageId by remember { mutableStateOf(session.languageId) }
    var topicId by remember { mutableStateOf(session.topicId) }

    val topics by remember(mode, courseId, languageId) {
        when (mode) {
            SessionLinkMode.COURSE -> courseId?.let { viewModel.topicsForCourse(it) } ?: flowOf(emptyList())
            SessionLinkMode.LANGUAGE -> languageId?.let { viewModel.topicsForLanguage(it) } ?: flowOf(emptyList())
            SessionLinkMode.NONE -> flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_session_link_dialog_title)) },
        text = {
            Column {
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(SessionLinkMode.NONE, stringResource(R.string.common_none)),
                        SegmentedToggleOption(SessionLinkMode.COURSE, stringResource(R.string.label_course)),
                        SegmentedToggleOption(SessionLinkMode.LANGUAGE, stringResource(R.string.label_language)),
                    ),
                    selected = mode,
                    onSelect = {
                        mode = it
                        courseId = null
                        languageId = null
                        topicId = null
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                when (mode) {
                    SessionLinkMode.COURSE -> PickerField(
                        label = stringResource(R.string.label_course),
                        selectedLabel = courses.firstOrNull { it.id == courseId }?.let { courseDisplayName(it.name) }
                            ?: stringResource(R.string.common_select),
                        options = courses.map { it.id to courseDisplayName(it.name) },
                        onSelect = { courseId = it; topicId = null },
                    )
                    SessionLinkMode.LANGUAGE -> PickerField(
                        label = stringResource(R.string.label_language),
                        selectedLabel = languages.firstOrNull { it.id == languageId }?.name ?: stringResource(R.string.common_select),
                        options = languages.map { it.id to it.name },
                        onSelect = { languageId = it; topicId = null },
                    )
                    SessionLinkMode.NONE -> Unit
                }
                if (courseId != null || languageId != null) {
                    PickerField(
                        label = stringResource(R.string.label_link_to_topic),
                        selectedLabel = topics.firstOrNull { it.id == topicId }?.name ?: stringResource(R.string.common_none),
                        options = listOf<Pair<Long?, String>>(null to stringResource(R.string.common_none)) + topics.map { it.id to it.name },
                        onSelect = { topicId = it },
                    )
                }
            }
        },
        confirmButton = {
            // Picking Course/Language without choosing one yet would silently unlink — keep Save off until chosen.
            val complete = when (mode) {
                SessionLinkMode.NONE -> true
                SessionLinkMode.COURSE -> courseId != null
                SessionLinkMode.LANGUAGE -> languageId != null
            }
            TextButton(onClick = { onConfirm(courseId, languageId, topicId) }, enabled = complete) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun <T> PickerField(label: String, selectedLabel: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(selectedLabel, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(text = { Text(optionLabel) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

@Composable
private fun NotesDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_session_notes_section)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.focus_session_notes_hint)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).imePadding(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
