package com.ridvan.target.ui.studysource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ridvan.target.data.local.entity.StudySource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudySourceListContent(
    title: String,
    studySources: List<StudySource>,
    onAdd: (String) -> Unit,
    onUpdate: (StudySource, String) -> Unit,
    onDelete: (StudySource) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<StudySource?>(null) }
    var deletingSource by remember { mutableStateOf<StudySource?>(null) }

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
                    StudySourceRow(
                        source = source,
                        onEdit = { editingSource = source },
                        onDelete = { deletingSource = source },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        StudySourceNameDialog(
            title = "Add study source",
            initialName = "",
            onConfirm = { name ->
                onAdd(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingSource?.let { source ->
        StudySourceNameDialog(
            title = "Edit study source",
            initialName = source.name,
            onConfirm = { name ->
                onUpdate(source, name)
                editingSource = null
            },
            onDismiss = { editingSource = null },
        )
    }

    deletingSource?.let { source ->
        AlertDialog(
            onDismissRequest = { deletingSource = null },
            title = { Text("Delete study source?") },
            text = { Text("This removes \"${source.name}\".") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(source)
                    deletingSource = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingSource = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StudySourceRow(source: StudySource, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(source.name) },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit study source")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete study source")
                }
            }
        },
    )
}

@Composable
private fun StudySourceNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
