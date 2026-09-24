package com.ridvan.target.ui.topiclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.TopicWithTotals
import com.ridvan.target.ui.common.AddFab

/**
 * Presentational-only list of a Course's or a Language's Topics — has no owner-type awareness
 * of its own, mirroring StudyResourceListContent.kt's split for the same Course-vs-Language
 * duality. `title` is already resolved by the caller (translated for a Course, plain for a
 * Language).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicListContent(
    title: String,
    topics: List<TopicWithTotals>,
    onAdd: (name: String) -> Unit,
    onTopicClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        floatingActionButton = {
            AddFab(onClick = { showAddDialog = true })
        },
    ) { innerPadding ->
        if (topics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.topiclist_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(topics, key = { it.topic.id }) { topic ->
                    TopicRow(topic = topic, onClick = { onTopicClick(topic.topic.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddTopicDialog(
            onConfirm = { name ->
                onAdd(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun TopicRow(topic: TopicWithTotals, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(topic.topic.name) },
        supportingContent = { Text(stringResource(R.string.counts_tests_questions, topic.totalTestCount, topic.totalQuestionCount)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AddTopicDialog(onConfirm: (name: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_topic_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
