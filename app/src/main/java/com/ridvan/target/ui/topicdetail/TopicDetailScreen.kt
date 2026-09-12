package com.ridvan.target.ui.topicdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.StudyResourceTopicWithStudyResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    onResourceClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TopicDetailViewModel = viewModel(),
) {
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val contributions by viewModel.contributions.collectAsStateWithLifecycle()
    val totalTestCount by viewModel.totalTestCount.collectAsStateWithLifecycle()
    val totalQuestionCount by viewModel.totalQuestionCount.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_dialog_edit_topic))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_topic))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            Text(
                stringResource(R.string.counts_tests_questions, totalTestCount, totalQuestionCount),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.topicdetail_subtitle),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (contributions.isEmpty()) {
                Text(stringResource(R.string.topicdetail_no_contributions))
            } else {
                contributions.forEach { contribution ->
                    ContributionRow(
                        contribution,
                        onClick = { onResourceClick(contribution.studyResourceTopic.studyResourceId) },
                    )
                }
            }
        }
    }

    if (showEditDialog && topic != null) {
        TopicEditDialog(
            initialName = topic!!.name,
            onConfirm = { name ->
                viewModel.updateTopic(name)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.topicdetail_delete_title)) },
            text = { Text(stringResource(R.string.topicdetail_delete_message, topic?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTopic()
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun ContributionRow(contribution: StudyResourceTopicWithStudyResource, onClick: () -> Unit) {
    val noPublisher = stringResource(R.string.common_no_publisher)
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.contribution_row_title, contribution.studyResourceName, contribution.studyResourcePublisher ?: noPublisher))
        },
        supportingContent = {
            Text(stringResource(R.string.counts_tests_questions, contribution.studyResourceTopic.testCount, contribution.studyResourceTopic.questionCount))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun TopicEditDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_dialog_edit_topic)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.common_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
