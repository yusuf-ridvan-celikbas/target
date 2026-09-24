package com.ridvan.target.ui.studyresourcedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.StudyResourceTopicWithProgress
import com.ridvan.target.data.local.entity.PracticeExamEntry
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.HelpTooltip
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.studyresource.StudyResourceFormDialog
import com.ridvan.target.ui.studyresource.studyResourceTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyResourceDetailScreen(
    onBack: () -> Unit,
    onDuplicated: (newStudyResourceId: Long) -> Unit,
    onOpenTopicProgress: (studyResourceTopicId: Long) -> Unit,
    onOpenPracticeExamEntry: (entryId: Long) -> Unit,
    viewModel: StudyResourceDetailViewModel = viewModel(),
) {
    val studyResource by viewModel.studyResource.collectAsStateWithLifecycle()
    val subjectName by viewModel.subjectName.collectAsStateWithLifecycle()
    val displaySubjectName = if (studyResource?.courseId != null) courseDisplayName(subjectName) else subjectName
    val attachedTopics by viewModel.attachedTopics.collectAsStateWithLifecycle()
    val availableTopicsToAdd by viewModel.availableTopicsToAdd.collectAsStateWithLifecycle()
    val practiceExamEntries by viewModel.practiceExamEntries.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studyResource?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.dialog_edit_study_resource_title))
                    }
                    val copySuffix = stringResource(R.string.sr_copy_suffix)
                    IconButton(onClick = { viewModel.duplicateStudyResource(copySuffix, onDuplicated) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_duplicate_study_resource))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_study_resource))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.srdetail_subject, displaySubjectName))
            Text(stringResource(R.string.srdetail_type, studyResourceTypeLabel(studyResource?.type)))
            studyResource?.publisher?.let { publisher ->
                Text(stringResource(R.string.srdetail_publisher, publisher))
            }
            if (studyResource?.type == StudyResourceType.PRACTICE_EXAM) {
                PracticeExamEntriesSection(
                    entries = practiceExamEntries,
                    onAddClick = { showAddEntryDialog = true },
                    onEntryClick = { onOpenPracticeExamEntry(it.id) },
                )
            }

            if (studyResource?.type == StudyResourceType.QUESTION_BANK &&
                (studyResource?.courseId != null || studyResource?.languageId != null)
            ) {
                GroupedCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    TopicsSectionHeader(onAddClick = { showAddTopicDialog = true })
                    if (attachedTopics.isEmpty()) {
                        Text(
                            stringResource(R.string.srdetail_no_topics),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        ReorderableTopicsList(
                            attachedTopics = attachedTopics,
                            onRowClick = { onOpenTopicProgress(it.studyResourceTopic.id) },
                            onReorder = { orderedIds -> viewModel.reorderTopics(orderedIds) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && studyResource != null) {
        StudyResourceFormDialog(
            title = stringResource(R.string.dialog_edit_study_resource_title),
            subjectLabel = displaySubjectName,
            initialName = studyResource!!.name,
            initialType = studyResource!!.type,
            initialPublisher = studyResource!!.publisher.orEmpty(),
            onConfirm = { name, type, publisher ->
                viewModel.updateStudyResource(name, type, publisher)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.srdetail_delete_title)) },
            text = { Text(stringResource(R.string.delete_confirm_generic, studyResource?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudyResource()
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            availableTopics = availableTopicsToAdd,
            onConfirm = { name, selectedIds ->
                if (name.isNotBlank()) viewModel.addTopic(name)
                if (selectedIds.isNotEmpty()) viewModel.addExistingTopics(selectedIds)
                showAddTopicDialog = false
            },
            onDismiss = { showAddTopicDialog = false },
        )
    }

    if (showAddEntryDialog) {
        AddEntryDialog(
            onConfirm = { name, questionCount ->
                viewModel.addPracticeExamEntry(name, questionCount)
                showAddEntryDialog = false
            },
            onDismiss = { showAddEntryDialog = false },
        )
    }
}

@Composable
private fun PracticeExamEntriesSection(
    entries: List<PracticeExamEntry>,
    onAddClick: () -> Unit,
    onEntryClick: (PracticeExamEntry) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.practice_exam_entries_button), modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) { Text(stringResource(R.string.action_add_entry)) }
    }
    if (entries.isEmpty()) {
        Text(stringResource(R.string.practice_exam_no_entries), modifier = Modifier.padding(top = 4.dp))
    } else {
        val totalCorrect = entries.sumOf { it.correctCount }
        val totalWrong = entries.sumOf { it.wrongCount }
        val totalQuestions = totalCorrect + totalWrong
        val accuracy = if (totalQuestions == 0) 0 else totalCorrect * 100 / totalQuestions
        val totalMinutes = entries.sumOf { it.durationMinutes }
        val durationText = stringResource(R.string.duration_format, totalMinutes / 60, totalMinutes % 60)
        Text(
            stringResource(R.string.practice_exam_entries_summary, entries.size, totalCorrect, totalWrong, accuracy, durationText),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            entries.forEach { entry ->
                EntryRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun EntryRow(entry: PracticeExamEntry, onClick: () -> Unit) {
    val blank = (entry.questionCount - entry.correctCount - entry.wrongCount).coerceAtLeast(0)
    ListItem(
        headlineContent = { Text(entry.name) },
        supportingContent = { Text(stringResource(R.string.entry_row_summary, entry.correctCount, entry.wrongCount, blank)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AddEntryDialog(
    onConfirm: (name: String, questionCount: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var questionCountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_entry_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_entry_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = questionCountText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) questionCountText = input },
                    label = { Text(stringResource(R.string.label_question_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, questionCountText.toIntOrNull() ?: 0) },
                enabled = name.isNotBlank() && (questionCountText.toIntOrNull() ?: 0) > 0,
            ) { Text(stringResource(R.string.common_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun TopicsSectionHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.label_topics))
        HelpTooltip(R.string.help_tooltip_topics_practice, R.string.cd_help_topics_practice, modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) { Text(stringResource(R.string.action_add_prefixed)) }
    }
}

@Composable
private fun ReorderableTopicsList(
    attachedTopics: List<StudyResourceTopicWithProgress>,
    onRowClick: (StudyResourceTopicWithProgress) -> Unit,
    onReorder: (orderedStudyResourceTopicIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var orderedIds by remember { mutableStateOf(attachedTopics.map { it.studyResourceTopic.id }) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val itemHeightPx = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(attachedTopics) {
        if (draggingId == null) {
            orderedIds = attachedTopics.map { it.studyResourceTopic.id }
        }
    }

    val byId = attachedTopics.associateBy { it.studyResourceTopic.id }

    Column(modifier = modifier) {
        orderedIds.forEach { id ->
            val attached = byId[id] ?: return@forEach
            key(id) {
                val isDragging = draggingId == id
                TopicRow(
                    attached = attached,
                    onClick = { onRowClick(attached) },
                    modifier = Modifier
                        .onSizeChanged { size -> itemHeightPx[id] = size.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f },
                    dragHandleModifier = Modifier.pointerInput(id) {
                        detectDragHandleGesture(
                            onDragStart = {
                                draggingId = id
                                dragOffsetY = 0f
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffsetY = 0f
                                onReorder(orderedIds)
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffsetY = 0f
                            },
                            onDrag = { dragAmount ->
                                dragOffsetY += dragAmount.y
                                val currentIndex = orderedIds.indexOf(id)
                                if (dragOffsetY > 0) {
                                    val belowIndex = currentIndex + 1
                                    if (belowIndex < orderedIds.size) {
                                        val belowHeight = itemHeightPx[orderedIds[belowIndex]]
                                        if (belowHeight != null && dragOffsetY > belowHeight / 2f) {
                                            orderedIds = orderedIds.toMutableList().apply { add(belowIndex, removeAt(currentIndex)) }
                                            dragOffsetY -= belowHeight
                                        }
                                    }
                                } else if (dragOffsetY < 0) {
                                    val aboveIndex = currentIndex - 1
                                    if (aboveIndex >= 0) {
                                        val aboveHeight = itemHeightPx[orderedIds[aboveIndex]]
                                        if (aboveHeight != null && -dragOffsetY > aboveHeight / 2f) {
                                            orderedIds = orderedIds.toMutableList().apply { add(aboveIndex, removeAt(currentIndex)) }
                                            dragOffsetY += aboveHeight
                                        }
                                    }
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

/**
 * A long-press-then-drag detector for a small drag handle nested inside a scrollable container.
 * Claims the touch (consumes it) from the very first down, so the ancestor's `verticalScroll`
 * never sees an unconsumed move and starts scrolling the page out from under the drag.
 */
private suspend fun PointerInputScope.detectDragHandleGesture(
    onDragStart: () -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val pointerId = down.id
        val releasedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var released = false
            while (!released) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) {
                    released = true
                } else {
                    change.consume()
                }
            }
        } != null
        if (!releasedBeforeLongPress) {
            onDragStart()
            val success = drag(pointerId) { change ->
                onDrag(change.positionChange())
                change.consume()
            }
            if (success) onDragEnd() else onDragCancel()
        }
    }
}

@Composable
private fun TopicRow(
    attached: StudyResourceTopicWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    val remainingTests = (attached.studyResourceTopic.testCount - attached.loggedTests).coerceAtLeast(0)
    val remainingQuestions = (attached.studyResourceTopic.questionCount - attached.loggedQuestions).coerceAtLeast(0)
    ListItem(
        headlineContent = { Text(attached.topicName) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.counts_remaining_of_target,
                    remainingTests,
                    attached.studyResourceTopic.testCount,
                    remainingQuestions,
                    attached.studyResourceTopic.questionCount,
                )
            )
        },
        trailingContent = {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.cd_drag_topic_handle),
                modifier = dragHandleModifier,
            )
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AddTopicDialog(
    availableTopics: List<Topic>,
    onConfirm: (newName: String, selectedIds: Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_topic_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_new_topic_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (availableTopics.isNotEmpty()) {
                    Text(
                        stringResource(R.string.label_pick_existing_topics),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        availableTopics.forEach { topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (topic.id in selected) selected - topic.id else selected + topic.id
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = topic.id in selected, onCheckedChange = null)
                                Text(topic.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selected) },
                enabled = name.isNotBlank() || selected.isNotEmpty(),
            ) { Text(stringResource(R.string.common_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
