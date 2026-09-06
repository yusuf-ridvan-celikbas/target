package com.ridvan.target.ui.studysource

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
import androidx.compose.ui.unit.dp
import com.ridvan.target.data.local.entity.StudySource
import com.ridvan.target.data.local.entity.StudySourceType

internal fun studySourceTypeLabel(type: StudySourceType?): String = when (type) {
    StudySourceType.QUESTION_BANK -> "Question Bank"
    StudySourceType.LECTURE_TEXTBOOK -> "Lecture/Textbook"
    StudySourceType.PRACTICE_EXAM -> "Practice Exam"
    StudySourceType.LECTURE_NOTES -> "Lecture Notes"
    null -> "Not set"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudySourceListContent(
    title: String,
    studySources: List<StudySource>,
    onAdd: (name: String, type: StudySourceType, publisher: String?) -> Unit,
    onSourceClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        if (studySources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No study sources yet. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(studySources, key = { it.id }) { source ->
                    StudySourceRow(source = source, onClick = { onSourceClick(source.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        StudySourceFormDialog(
            title = "Add study source",
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
private fun StudySourceRow(source: StudySource, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(source.name) },
        supportingContent = { Text(studySourceTypeLabel(source.type)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun StudySourceFormDialog(
    title: String,
    subjectLabel: String,
    initialName: String,
    initialType: StudySourceType?,
    initialPublisher: String,
    onConfirm: (name: String, type: StudySourceType, publisher: String?) -> Unit,
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
                Text("Subject", style = MaterialTheme.typography.labelSmall)
                Text(subjectLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                StudySourceTypeField(
                    selectedType = type,
                    onSelect = { type = it },
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("Publisher (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { type?.let { onConfirm(name, it, publisher.trim().ifBlank { null }) } },
                enabled = name.isNotBlank() && type != null,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
internal fun StudySourceTypeField(selectedType: StudySourceType?, onSelect: (StudySourceType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text("Type", style = MaterialTheme.typography.labelSmall)
            Text(studySourceTypeLabel(selectedType), style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StudySourceType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(studySourceTypeLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
