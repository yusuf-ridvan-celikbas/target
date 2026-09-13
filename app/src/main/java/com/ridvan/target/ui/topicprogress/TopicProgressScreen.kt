package com.ridvan.target.ui.topicprogress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.PracticeLog
import com.ridvan.target.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicProgressScreen(
    onBack: () -> Unit,
    viewModel: TopicProgressViewModel = viewModel(),
) {
    val attached by viewModel.attached.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    var showEditTargetDialog by remember { mutableStateOf(false) }
    var showDetachConfirm by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<PracticeLog?>(null) }

    val totalTests = sessions.sumOf { it.testsSolved }
    val totalSolved = sessions.sumOf { it.solvedCount }
    val totalUnsolved = sessions.sumOf { it.unsolvedCount }
    val totalMinutes = sessions.sumOf { it.durationMinutes }
    val netScore = totalSolved - totalUnsolved / 4.0
    val durationText = stringResource(R.string.duration_format, totalMinutes / 60, totalMinutes % 60)

    val otherSessionsTests = sessions.filter { it.id != editingSession?.id }.sumOf { it.testsSolved }
    val otherSessionsQuestions = sessions.filter { it.id != editingSession?.id }.sumOf { it.solvedCount + it.unsolvedCount }
    val maxTestsForDialog = ((attached?.studyResourceTopic?.testCount ?: 0) - otherSessionsTests).coerceAtLeast(0)
    val maxQuestionsForDialog = ((attached?.studyResourceTopic?.questionCount ?: 0) - otherSessionsQuestions).coerceAtLeast(0)
    val remainingTests = ((attached?.studyResourceTopic?.testCount ?: 0) - totalTests).coerceAtLeast(0)
    val remainingQuestions = ((attached?.studyResourceTopic?.questionCount ?: 0) - (totalSolved + totalUnsolved)).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(attached?.topicName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditTargetDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_target_counts))
                    }
                    IconButton(onClick = { showDetachConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_detach_topic))
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
            attached?.let { current ->
                Text(
                    stringResource(R.string.label_target_counts, current.studyResourceTopic.testCount, current.studyResourceTopic.questionCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.label_remaining_counts, remainingTests, remainingQuestions),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                stringResource(R.string.progress_aggregate, totalTests, totalSolved, totalUnsolved, "%.2f".format(netScore), durationText),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            TextButton(onClick = { editingSession = null; showLogDialog = true }) {
                Text(stringResource(R.string.action_log_session))
            }
            if (sessions.isEmpty()) {
                Text(stringResource(R.string.progress_no_sessions), modifier = Modifier.padding(top = 4.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    sessions.forEach { session ->
                        SessionRow(
                            session = session,
                            onClick = { editingSession = session; showLogDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showEditTargetDialog && attached != null) {
        EditTargetCountsDialog(
            initialTestCount = attached!!.studyResourceTopic.testCount,
            initialQuestionCount = attached!!.studyResourceTopic.questionCount,
            onSave = { testCount, questionCount ->
                viewModel.updateTargetCounts(testCount, questionCount)
                showEditTargetDialog = false
            },
            onDismiss = { showEditTargetDialog = false },
        )
    }

    if (showDetachConfirm) {
        AlertDialog(
            onDismissRequest = { showDetachConfirm = false },
            title = { Text(stringResource(R.string.topicprogress_detach_title)) },
            text = { Text(stringResource(R.string.topicprogress_detach_message, sessions.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showDetachConfirm = false
                    viewModel.detachTopic(onBack)
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDetachConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showLogDialog) {
        LogSessionDialog(
            initial = editingSession,
            maxTests = maxTestsForDialog,
            maxQuestions = maxQuestionsForDialog,
            onSave = { testsSolved, solvedCount, unsolvedCount, durationMinutes ->
                val current = editingSession
                if (current == null) {
                    viewModel.logSession(testsSolved, solvedCount, unsolvedCount, durationMinutes)
                } else {
                    viewModel.updateSession(current, testsSolved, solvedCount, unsolvedCount, durationMinutes)
                }
                showLogDialog = false
                editingSession = null
            },
            onDelete = editingSession?.let { session ->
                {
                    viewModel.deleteSession(session)
                    showLogDialog = false
                    editingSession = null
                }
            },
            onDismiss = { showLogDialog = false; editingSession = null },
        )
    }
}

@Composable
private fun SessionRow(session: PracticeLog, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(formatDate(session.loggedAt)) },
        supportingContent = {
            val durationText = stringResource(R.string.duration_format, session.durationMinutes / 60, session.durationMinutes % 60)
            Text(stringResource(R.string.session_row_summary, session.testsSolved, session.solvedCount, session.unsolvedCount, durationText))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun EditTargetCountsDialog(
    initialTestCount: Int,
    initialQuestionCount: Int,
    onSave: (testCount: Int, questionCount: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var testCountText by remember { mutableStateOf(initialTestCount.toString()) }
    var questionCountText by remember { mutableStateOf(initialQuestionCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_edit_target_counts_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = testCountText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) testCountText = input },
                    label = { Text(stringResource(R.string.label_test_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            TextButton(onClick = {
                onSave(testCountText.toIntOrNull() ?: 0, questionCountText.toIntOrNull() ?: 0)
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun LogSessionDialog(
    initial: PracticeLog?,
    maxTests: Int,
    maxQuestions: Int,
    onSave: (testsSolved: Int, solvedCount: Int, unsolvedCount: Int, durationMinutes: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var testsSolvedText by remember { mutableStateOf(initial?.testsSolved?.toString() ?: "") }
    var solvedText by remember { mutableStateOf(initial?.solvedCount?.toString() ?: "") }
    var unsolvedText by remember { mutableStateOf(initial?.unsolvedCount?.toString() ?: "") }
    var hoursText by remember { mutableStateOf(initial?.let { (it.durationMinutes / 60).toString() } ?: "") }
    var minutesText by remember { mutableStateOf(initial?.let { (it.durationMinutes % 60).toString() } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_log_session_title else R.string.dialog_edit_session_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = testsSolvedText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            testsSolvedText = if (n > maxTests) maxTests.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_tests_solved)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.label_tests_remaining_hint, maxTests),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = solvedText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val unsolved = unsolvedText.toIntOrNull() ?: 0
                            val allowed = (maxQuestions - unsolved).coerceAtLeast(0)
                            solvedText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_solved_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = unsolvedText,
                    onValueChange = { input ->
                        if (input.all(Char::isDigit)) {
                            val n = input.toIntOrNull() ?: 0
                            val solved = solvedText.toIntOrNull() ?: 0
                            val allowed = (maxQuestions - solved).coerceAtLeast(0)
                            unsolvedText = if (n > allowed) allowed.toString() else input
                        }
                    },
                    label = { Text(stringResource(R.string.label_unsolved_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.label_questions_remaining_hint, maxQuestions),
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
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.action_delete_session))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hours = hoursText.toIntOrNull() ?: 0
                val minutes = minutesText.toIntOrNull() ?: 0
                onSave(
                    testsSolvedText.toIntOrNull() ?: 0,
                    solvedText.toIntOrNull() ?: 0,
                    unsolvedText.toIntOrNull() ?: 0,
                    hours * 60 + minutes,
                )
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
