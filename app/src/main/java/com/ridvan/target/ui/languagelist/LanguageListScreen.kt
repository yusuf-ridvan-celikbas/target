package com.ridvan.target.ui.languagelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun LanguageListScreen(
    shellNavigation: ShellNavigation,
    viewModel: LanguageListViewModel = viewModel(),
) {
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLanguage by remember { mutableStateOf<Language?>(null) }
    var deletingLanguage by remember { mutableStateOf<Language?>(null) }

    AppShell(
        navigation = shellNavigation,
        title = "Languages",
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        if (languages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No languages yet. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(languages, key = { it.id }) { language ->
                    LanguageRow(
                        language = language,
                        onEdit = { editingLanguage = language },
                        onDelete = { deletingLanguage = language },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = "Add language",
            initialName = "",
            onConfirm = { name ->
                viewModel.addLanguage(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingLanguage?.let { language ->
        NameDialog(
            title = "Edit language",
            initialName = language.name,
            onConfirm = { name ->
                viewModel.updateLanguage(language, name)
                editingLanguage = null
            },
            onDismiss = { editingLanguage = null },
        )
    }

    deletingLanguage?.let { language ->
        AlertDialog(
            onDismissRequest = { deletingLanguage = null },
            title = { Text("Delete language?") },
            text = { Text("This removes \"${language.name}\" from any exams it's assigned to.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLanguage(language)
                    deletingLanguage = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingLanguage = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LanguageRow(language: Language, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(language.name) },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit language")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete language")
                }
            }
        },
    )
}

@Composable
private fun NameDialog(title: String, initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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
