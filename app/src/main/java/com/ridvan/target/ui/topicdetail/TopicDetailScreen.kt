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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit topic")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete topic")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            Text(
                "$totalTestCount tests · $totalQuestionCount questions",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Across every study resource covering this topic",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (contributions.isEmpty()) {
                Text("No study resources cover this topic yet.")
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
            title = { Text("Delete topic?") },
            text = { Text("This removes \"${topic?.name}\" and its counts from every study resource that covers it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTopic()
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ContributionRow(contribution: StudyResourceTopicWithStudyResource, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("${contribution.studyResourceName} — ${contribution.studyResourcePublisher ?: "No publisher"}") },
        supportingContent = {
            Text("${contribution.studyResourceTopic.testCount} tests · ${contribution.studyResourceTopic.questionCount} questions")
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun TopicEditDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit topic") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
