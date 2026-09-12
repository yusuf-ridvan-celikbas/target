package com.ridvan.target.ui.studyresource

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceType

@Composable
internal fun studyResourceTypeLabel(type: StudyResourceType?): String = when (type) {
    StudyResourceType.QUESTION_BANK -> stringResource(R.string.sr_type_question_bank)
    StudyResourceType.LECTURE_TEXTBOOK -> stringResource(R.string.sr_type_lecture_textbook)
    StudyResourceType.PRACTICE_EXAM -> stringResource(R.string.sr_type_practice_exam)
    StudyResourceType.LECTURE_NOTES -> stringResource(R.string.sr_type_lecture_notes)
    null -> stringResource(R.string.common_not_set)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyResourceListContent(
    title: String,
    studyResources: List<StudyResource>,
    onAdd: (name: String, type: StudyResourceType, publisher: String?) -> Unit,
    onResourceClick: (Long) -> Unit,
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        if (studyResources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.sr_list_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(studyResources, key = { it.id }) { resource ->
                    StudyResourceRow(resource = resource, onClick = { onResourceClick(resource.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        StudyResourceFormDialog(
            title = stringResource(R.string.dialog_add_study_resource_title),
            subjectLabel = title,
            initialName = "",
            initialType = null,
            initialPublisher = "",
            onConfirm = { name, type, publisher ->
                onAdd(name, type, publisher)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun StudyResourceRow(resource: StudyResource, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(resource.name) },
        supportingContent = { Text(studyResourceTypeLabel(resource.type)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun StudyResourceFormDialog(
    title: String,
    subjectLabel: String,
    initialName: String,
    initialType: StudyResourceType?,
    initialPublisher: String,
    onConfirm: (name: String, type: StudyResourceType, publisher: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var publisher by remember { mutableStateOf(initialPublisher) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.label_subject), style = MaterialTheme.typography.labelSmall)
                Text(subjectLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                StudyResourceTypeField(
                    selectedType = type,
                    onSelect = { type = it },
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text(stringResource(R.string.label_publisher_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { type?.let { onConfirm(name, it, publisher.trim().ifBlank { null }) } },
                enabled = name.isNotBlank() && type != null,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
internal fun StudyResourceTypeField(selectedType: StudyResourceType?, onSelect: (StudyResourceType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.label_type), style = MaterialTheme.typography.labelSmall)
            Text(studyResourceTypeLabel(selectedType), style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StudyResourceType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(studyResourceTypeLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
