package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.dao.MockExamTopicResultWithTopic
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamStatus
import androidx.compose.material3.Button
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.practiceexam.AddTopicResultDialog
import com.ridvan.target.ui.practiceexam.EditTopicResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamDetailScreen(
    onOpenTimer: (Long) -> Unit,
    onOpenQuestions: (partId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MockExamDetailViewModel = viewModel(),
) {
    val mockExam by viewModel.mockExam.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val topicResults by viewModel.topicResults.collectAsStateWithLifecycle()
    val tytSource by viewModel.tytSource.collectAsStateWithLifecycle()
    val countedPartIds by viewModel.countedPartIds.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showScoringDialog by remember { mutableStateOf(false) }
    var showStartConfirm by remember { mutableStateOf(false) }
    var editingPart by remember { mutableStateOf<MockExamPartWithCourse?>(null) }
    var addingTopicsTo by remember { mutableStateOf<MockExamPartWithCourse?>(null) }
    var editingTopic by remember { mutableStateOf<MockExamTopicResultWithTopic?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mockExam?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mockexam_edit_info)) },
                            onClick = { menuOpen = false; showInfoDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mockexam_scoring)) },
                            onClick = { menuOpen = false; showScoringDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete)) },
                            onClick = { menuOpen = false; showDeleteConfirm = true },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            mockExam?.let { current ->
                val questions = parts.sumOf { it.part.questionCount }
                val correct = parts.sumOf { it.part.correctCount }
                val wrong = parts.sumOf { it.part.wrongCount }
                if (current.status != MockExamStatus.FINISHED) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            if (current.status == MockExamStatus.RUNNING) stringResource(R.string.mockexam_in_progress)
                            else mockExamPlannedText(current.scheduledAt),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(
                                R.string.mockexam_plan_line,
                                countText(R.plurals.count_questions, questions),
                                formatDuration(current.timeLimitMinutes),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        current.publisher?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Button(
                            onClick = {
                                if (current.status == MockExamStatus.RUNNING) onOpenTimer(current.id) else showStartConfirm = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (current.status == MockExamStatus.RUNNING) R.string.mockexam_open_timer else R.string.mockexam_start
                                )
                            )
                        }
                    }
                } else Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.mockexam_total_net, formatNet(mockExamNet(correct, wrong))),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.mockexam_result_line, correct, wrong, mockExamBlank(questions, correct, wrong)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        stringResource(
                            R.string.mockexam_time_line,
                            formatDuration(current.durationMinutes),
                            formatDuration(current.timeLimitMinutes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        listOfNotNull(formatDate(current.takenAt), current.publisher).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.mockexam_tap_course_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                val system = current.scoringSystem
                if (current.status == MockExamStatus.FINISHED && system != null) {
                    ScoreCard(system, parts, tytSource)
                }
            }

            parts.forEach { part ->
                val p = part.part
                val partTopics = topicResults.filter { it.result.mockExamPartId == p.id }
                GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ListItem(
                        leadingContent = { CourseIconAvatar(part.courseIcon) },
                        headlineContent = { Text(partLabel(part)) },
                        supportingContent = {
                            Column {
                                Text(
                                    stringResource(
                                        R.string.mockexam_part_line,
                                        countText(R.plurals.count_questions, p.questionCount),
                                        formatNet(mockExamNet(p.correctCount, p.wrongCount)),
                                    )
                                )
                                Text(
                                    stringResource(
                                        R.string.mockexam_result_line,
                                        p.correctCount,
                                        p.wrongCount,
                                        mockExamBlank(p.questionCount, p.correctCount, p.wrongCount),
                                    )
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onOpenQuestions(p.id) }) {
                                Icon(Icons.Filled.Apps, contentDescription = stringResource(R.string.mockexam_questions))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { editingPart = part },
                    )
                    if (p.courseId != null) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.topic_breakdown_section_title),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { addingTopicsTo = part }) {
                                Text(stringResource(R.string.action_add_topic_breakdown))
                            }
                        }
                        partTopics.forEach { topic ->
                            ListItem(
                                headlineContent = { Text(topic.topicName) },
                                supportingContent = {
                                    Text(
                                        stringResource(
                                            R.string.topic_result_row_summary,
                                            countText(R.plurals.count_questions, topic.result.questionCount),
                                            topic.result.correctCount,
                                            topic.result.wrongCount,
                                        )
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { editingTopic = topic },
                            )
                        }
                    }
                }
            }
        }
    }

    editingPart?.let { part ->
        EditPartDialog(
            part = part,
            countedFromQuestions = part.part.id in countedPartIds,
            onSave = { questions, correct, wrong ->
                viewModel.updatePart(part.part, questions, correct, wrong)
                editingPart = null
            },
            onDismiss = { editingPart = null },
        )
    }

    addingTopicsTo?.let { part ->
        val allTopics by remember(part.part.courseId) { viewModel.topicsForCourse(part.part.courseId ?: 0) }
            .collectAsState(initial = emptyList())
        val added = topicResults.filter { it.result.mockExamPartId == part.part.id }.map { it.result.topicId }.toSet()
        AddTopicResultDialog(
            availableTopics = allTopics.filterNot { it.id in added },
            onConfirm = { ids ->
                viewModel.addTopicResults(part.part.id, ids)
                addingTopicsTo = null
            },
            onDismiss = { addingTopicsTo = null },
        )
    }

    editingTopic?.let { editing ->
        val part = parts.firstOrNull { it.part.id == editing.result.mockExamPartId }?.part
        val others = topicResults.filter { it.result.mockExamPartId == editing.result.mockExamPartId && it.result.id != editing.result.id }
        EditTopicResultDialog(
            topicName = editing.topicName,
            questionCount = editing.result.questionCount,
            correctCount = editing.result.correctCount,
            wrongCount = editing.result.wrongCount,
            maxQuestionCount = ((part?.questionCount ?: 0) - others.sumOf { it.result.questionCount }).coerceAtLeast(0),
            maxCorrectCount = ((part?.correctCount ?: 0) - others.sumOf { it.result.correctCount }).coerceAtLeast(0),
            maxWrongCount = ((part?.wrongCount ?: 0) - others.sumOf { it.result.wrongCount }).coerceAtLeast(0),
            onSave = { questions, correct, wrong ->
                viewModel.updateTopicResult(editing.result, questions, correct, wrong)
                editingTopic = null
            },
            onRemove = {
                viewModel.removeTopicResult(editing.result)
                editingTopic = null
            },
            onDismiss = { editingTopic = null },
        )
    }

    if (showInfoDialog) {
        mockExam?.let { current ->
            EditInfoDialog(
                mockExam = current,
                onSave = { name, publisher, minutes, scheduledAt ->
                    viewModel.updateInfo(name, publisher, minutes, scheduledAt)
                    showInfoDialog = false
                },
                onDismiss = { showInfoDialog = false },
            )
        }
    }

    if (showScoringDialog) {
        mockExam?.let { current ->
            ScoringDialog(
                current = current.scoringSystem,
                parts = parts,
                onSave = { system, tests ->
                    viewModel.updateScoring(system, tests)
                    showScoringDialog = false
                },
                onDismiss = { showScoringDialog = false },
            )
        }
    }

    if (showStartConfirm) {
        mockExam?.let { current ->
            AlertDialog(
                onDismissRequest = { showStartConfirm = false },
                title = { Text(stringResource(R.string.mockexam_start_title, current.name)) },
                text = { Text(stringResource(R.string.mockexam_start_message, formatDuration(current.timeLimitMinutes))) },
                confirmButton = {
                    TextButton(onClick = {
                        showStartConfirm = false
                        onOpenTimer(current.id)
                    }) { Text(stringResource(R.string.mockexam_start)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.mockexam_delete_title)) },
            text = { Text(stringResource(R.string.mockexam_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 4 && input.all(Char::isDigit)) onValueChange(input) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun EditPartDialog(
    part: MockExamPartWithCourse,
    countedFromQuestions: Boolean,
    onSave: (questionCount: Int, correctCount: Int, wrongCount: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var questionsText by remember { mutableStateOf(part.part.questionCount.toString()) }
    var correctText by remember { mutableStateOf(part.part.correctCount.takeIf { it > 0 }?.toString().orEmpty()) }
    var wrongText by remember { mutableStateOf(part.part.wrongCount.takeIf { it > 0 }?.toString().orEmpty()) }
    val questions = questionsText.toIntOrNull() ?: 0
    val correct = correctText.toIntOrNull() ?: 0
    val wrong = wrongText.toIntOrNull() ?: 0
    val valid = questions > 0 && correct + wrong <= questions

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(partLabel(part)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                NumberField(questionsText, { questionsText = it }, stringResource(R.string.label_question_count))
                if (countedFromQuestions) {
                    Text(
                        stringResource(R.string.mockexam_part_counted_from_questions),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else NumberField(
                    correctText,
                    { input -> correctText = minOf(input.toIntOrNull() ?: 0, (questions - wrong).coerceAtLeast(0)).toString().takeIf { input.isNotEmpty() }.orEmpty() },
                    stringResource(R.string.label_correct_count),
                    Modifier.padding(top = 8.dp),
                )
                if (!countedFromQuestions) NumberField(
                    wrongText,
                    { input -> wrongText = minOf(input.toIntOrNull() ?: 0, (questions - correct).coerceAtLeast(0)).toString().takeIf { input.isNotEmpty() }.orEmpty() },
                    stringResource(R.string.label_wrong_count),
                    Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(
                        R.string.mockexam_live_blank_net,
                        mockExamBlank(questions, correct, wrong),
                        formatNet(mockExamNet(correct, wrong)),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(questions, correct, wrong) }, enabled = valid) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun EditInfoDialog(
    mockExam: MockExam,
    onSave: (name: String, publisher: String, durationMinutes: Int, scheduledAt: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var scheduledAt by remember { mutableStateOf(mockExam.scheduledAt) }
    var name by remember { mutableStateOf(mockExam.name) }
    var publisher by remember { mutableStateOf(mockExam.publisher.orEmpty()) }
    var minutesText by remember { mutableStateOf(mockExam.durationMinutes.toString()) }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_edit_info)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text(stringResource(R.string.label_publisher_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (mockExam.status == MockExamStatus.PLANNED) {
                    StartTimeField(scheduledAt, { scheduledAt = it }, Modifier.padding(top = 8.dp))
                } else {
                    NumberField(minutesText, { minutesText = it }, stringResource(R.string.mockexam_minutes_used), Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, publisher, minutesText.toIntOrNull() ?: 0, scheduledAt) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
