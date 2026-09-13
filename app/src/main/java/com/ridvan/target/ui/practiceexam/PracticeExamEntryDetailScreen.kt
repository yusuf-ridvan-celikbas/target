package com.ridvan.target.ui.practiceexam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.PracticeExamEntryTopicResultWithTopic
import com.ridvan.target.data.local.entity.PracticeExamEntry
import com.ridvan.target.data.local.entity.PracticeExamEntryTopicResult
import com.ridvan.target.data.local.entity.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeExamEntryDetailScreen(
    onBack: () -> Unit,
    viewModel: PracticeExamEntryDetailViewModel = viewModel(),
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val studyResource by viewModel.studyResource.collectAsStateWithLifecycle()
    val topicResults by viewModel.topicResults.collectAsStateWithLifecycle()
    val availableTopicsToAdd by viewModel.availableTopicsToAdd.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var editingTopicResult by remember { mutableStateOf<PracticeExamEntryTopicResultWithTopic?>(null) }

    val blank = ((entry?.questionCount ?: 0) - (entry?.correctCount ?: 0) - (entry?.wrongCount ?: 0)).coerceAtLeast(0)
    val durationText = stringResource(
        R.string.duration_format,
        (entry?.durationMinutes ?: 0) / 60,
        (entry?.durationMinutes ?: 0) % 60,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.dialog_edit_entry_result_title))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_entry))
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
            entry?.let { current ->
                Text(
                    stringResource(R.string.entry_question_count, current.questionCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.entry_result_summary, current.correctCount, current.wrongCount, blank, durationText),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (studyResource?.courseId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.topic_breakdown_section_title), modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddTopicDialog = true }) { Text(stringResource(R.string.action_add_topic_breakdown)) }
                }
                if (topicResults.isEmpty()) {
                    Text(stringResource(R.string.topic_breakdown_empty), modifier = Modifier.padding(top = 4.dp))
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        topicResults.forEach { result ->
                            TopicResultRow(result = result, onClick = { editingTopicResult = result })
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && entry != null) {
        EditEntryResultDialog(
            entry = entry!!,
            onSave = { correct, wrong, durationMinutes ->
                viewModel.updateResult(correct, wrong, durationMinutes)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_entry_title)) },
            text = { Text(stringResource(R.string.dialog_delete_entry_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteEntry(onBack)
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showAddTopicDialog) {
        AddTopicResultDialog(
            availableTopics = availableTopicsToAdd,
            onConfirm = { selectedIds ->
                viewModel.addTopicResults(selectedIds)
                showAddTopicDialog = false
            },
            onDismiss = { showAddTopicDialog = false },
        )
    }

    editingTopicResult?.let { editing ->
        val otherTopicResults = topicResults.filter { it.topicResult.id != editing.topicResult.id }
        val remainingQuestionCount = ((entry?.questionCount ?: 0) - otherTopicResults.sumOf { it.topicResult.questionCount }).coerceAtLeast(0)
        val remainingCorrect = ((entry?.correctCount ?: 0) - otherTopicResults.sumOf { it.topicResult.correctCount }).coerceAtLeast(0)
        val remainingWrong = ((entry?.wrongCount ?: 0) - otherTopicResults.sumOf { it.topicResult.wrongCount }).coerceAtLeast(0)
        EditTopicResultDialog(
            topicResultWithTopic = editing,
            maxQuestionCount = remainingQuestionCount,
            maxCorrectCount = remainingCorrect,
            maxWrongCount = remainingWrong,
            onSave = { questionCount, correctCount, wrongCount ->
                viewModel.updateTopicResult(editing.topicResult, questionCount, correctCount, wrongCount)
                editingTopicResult = null
            },
            onRemove = {
                viewModel.removeTopicResult(editing.topicResult)
                editingTopicResult = null
            },
            onDismiss = { editingTopicResult = null },
        )
    }
}

@Composable
private fun TopicResultRow(result: PracticeExamEntryTopicResultWithTopic, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(result.topicName) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.topic_result_row_summary,
                    result.topicResult.questionCount,
                    result.topicResult.correctCount,
                    result.topicResult.wrongCount,
                )
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun EditEntryResultDialog(
    entry: PracticeExamEntry,
    onSave: (correctCount: Int, wrongCount: Int, durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var correctText by remember { mutableStateOf(entry.correctCount.toString()) }
    var wrongText by remember { mutableStateOf(entry.wrongCount.toString()) }
    var hoursText by remember { mutableStateOf((entry.durationMinutes / 60).toString()) }
    var minutesText by remember { mutableStateOf((entry.durationMinutes % 60).toString()) }
    val maxQuestions = entry.questionCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_edit_entry_result_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = correctText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val wrong = wrongText.toIntOrNull() ?: 0
                            val allowed = (maxQuestions - wrong).coerceAtLeast(0)
                            correctText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_correct_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wrongText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val correct = correctText.toIntOrNull() ?: 0
                            val allowed = (maxQuestions - correct).coerceAtLeast(0)
                            wrongText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_wrong_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                val blank = (maxQuestions - (correctText.toIntOrNull() ?: 0) - (wrongText.toIntOrNull() ?: 0)).coerceAtLeast(0)
                Text(
                    stringResource(R.string.label_blank_count, blank),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) hoursText = input },
                    label = { Text(stringResource(R.string.label_hours)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) minutesText = input },
                    label = { Text(stringResource(R.string.label_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hours = hoursText.toIntOrNull() ?: 0
                val minutes = minutesText.toIntOrNull() ?: 0
                onSave(correctText.toIntOrNull() ?: 0, wrongText.toIntOrNull() ?: 0, hours * 60 + minutes)
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun AddTopicResultDialog(
    availableTopics: List<Topic>,
    onConfirm: (selectedIds: Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_topic_breakdown)) },
        text = {
            if (availableTopics.isEmpty()) {
                Text(stringResource(R.string.topic_breakdown_no_more_topics))
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun EditTopicResultDialog(
    topicResultWithTopic: PracticeExamEntryTopicResultWithTopic,
    maxQuestionCount: Int,
    maxCorrectCount: Int,
    maxWrongCount: Int,
    onSave: (questionCount: Int, correctCount: Int, wrongCount: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val topicResult = topicResultWithTopic.topicResult
    var questionCountText by remember { mutableStateOf(topicResult.questionCount.toString()) }
    var correctText by remember { mutableStateOf(topicResult.correctCount.toString()) }
    var wrongText by remember { mutableStateOf(topicResult.wrongCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(topicResultWithTopic.topicName) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = questionCountText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            questionCountText = if (n > maxQuestionCount) maxQuestionCount.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_question_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.label_tests_remaining_hint, maxQuestionCount),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = correctText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val rowQuestionCount = questionCountText.toIntOrNull() ?: 0
                            val wrong = wrongText.toIntOrNull() ?: 0
                            val allowed = minOf(maxCorrectCount, rowQuestionCount - wrong).coerceAtLeast(0)
                            correctText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_correct_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = wrongText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val rowQuestionCount = questionCountText.toIntOrNull() ?: 0
                            val correct = correctText.toIntOrNull() ?: 0
                            val allowed = minOf(maxWrongCount, rowQuestionCount - correct).coerceAtLeast(0)
                            wrongText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_wrong_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.topic_result_correct_wrong_remaining_hint, maxCorrectCount, maxWrongCount),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = onRemove, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.action_remove_topic_result))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(questionCountText.toIntOrNull() ?: 0, correctText.toIntOrNull() ?: 0, wrongText.toIntOrNull() ?: 0)
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
