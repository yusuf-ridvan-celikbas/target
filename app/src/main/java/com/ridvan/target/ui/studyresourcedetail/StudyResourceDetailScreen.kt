package com.ridvan.target.ui.studyresourcedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.ridvan.target.data.local.dao.StudyResourceTopicWithTopic
import com.ridvan.target.data.local.entity.StudyResourceTopic
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.studyresource.StudyResourceFormDialog
import com.ridvan.target.ui.studyresource.studyResourceTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyResourceDetailScreen(
    onBack: () -> Unit,
    viewModel: StudyResourceDetailViewModel = viewModel(),
) {
    val studyResource by viewModel.studyResource.collectAsStateWithLifecycle()
    val subjectName by viewModel.subjectName.collectAsStateWithLifecycle()
    val attachedTopics by viewModel.attachedTopics.collectAsStateWithLifecycle()
    val availableTopicsToAdd by viewModel.availableTopicsToAdd.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<StudyResourceTopicWithTopic?>(null) }

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
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_study_resource))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            Text(stringResource(R.string.srdetail_subject, subjectName))
            Text(stringResource(R.string.srdetail_type, studyResourceTypeLabel(studyResource?.type)))
            studyResource?.publisher?.let { publisher ->
                Text(stringResource(R.string.srdetail_publisher, publisher))
            }

            if (studyResource?.type == StudyResourceType.QUESTION_BANK && studyResource?.courseId != null) {
                TopicsSectionHeader(onAddClick = { showAddTopicDialog = true })
                if (attachedTopics.isEmpty()) {
                    Text(stringResource(R.string.srdetail_no_topics), modifier = Modifier.padding(top = 4.dp))
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        attachedTopics.forEach { attached ->
                            TopicRow(attached, onClick = { editingTopic = attached })
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && studyResource != null) {
        StudyResourceFormDialog(
            title = stringResource(R.string.dialog_edit_study_resource_title),
            subjectLabel = subjectName,
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

    editingTopic?.let { attached ->
        EditTopicCountsDialog(
            attached = attached,
            onSave = { testCount, questionCount ->
                viewModel.updateTopicCounts(attached.studyResourceTopic, testCount, questionCount)
                editingTopic = null
            },
            onRemove = {
                viewModel.removeTopic(attached.studyResourceTopic)
                editingTopic = null
            },
            onDismiss = { editingTopic = null },
        )
    }
}

@Composable
private fun TopicsSectionHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.label_topics), modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) { Text(stringResource(R.string.action_add_prefixed)) }
    }
}

@Composable
private fun TopicRow(attached: StudyResourceTopicWithTopic, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(attached.topicName) },
        supportingContent = {
            Text(stringResource(R.string.counts_tests_questions, attached.studyResourceTopic.testCount, attached.studyResourceTopic.questionCount))
        },
        modifier = Modifier.clickable(onClick = onClick),
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
            Column {
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

@Composable
private fun EditTopicCountsDialog(
    attached: StudyResourceTopicWithTopic,
    onSave: (testCount: Int, questionCount: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var testCountText by remember { mutableStateOf(attached.studyResourceTopic.testCount.toString()) }
    var questionCountText by remember { mutableStateOf(attached.studyResourceTopic.questionCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(attached.topicName) },
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
                TextButton(onClick = onRemove, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.action_remove_topic_from_resource))
                }
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
