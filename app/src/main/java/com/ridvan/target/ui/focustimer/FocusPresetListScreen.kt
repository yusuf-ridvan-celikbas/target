package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.ui.common.AddFab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPresetListScreen(
    onBack: () -> Unit,
    viewModel: FocusPresetListViewModel = viewModel(),
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<FocusPreset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focustimer_preset_list_title)) },
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
        if (presets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.focustimer_preset_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(presets, key = { it.id }) { preset ->
                    PresetRow(preset = preset, onClick = { editingPreset = preset })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        PresetDialog(
            title = stringResource(R.string.dialog_add_preset_title),
            initial = null,
            onConfirm = { name, work, brk ->
                viewModel.addPreset(name, work, brk)
                showAddDialog = false
            },
            onDelete = null,
            onDismiss = { showAddDialog = false },
        )
    }

    editingPreset?.let { preset ->
        PresetDialog(
            title = stringResource(R.string.dialog_edit_preset_title),
            initial = preset,
            onConfirm = { name, work, brk ->
                viewModel.updatePreset(preset, name, work, brk)
                editingPreset = null
            },
            onDelete = {
                viewModel.deletePreset(preset)
                editingPreset = null
            },
            onDismiss = { editingPreset = null },
        )
    }
}

@Composable
private fun PresetRow(preset: FocusPreset, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(preset.name) },
        supportingContent = {
            Text(stringResource(R.string.focustimer_preset_row_subtitle, preset.workMinutes, preset.breakMinutes))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PresetDialog(
    title: String,
    initial: FocusPreset?,
    onConfirm: (name: String, workMinutes: Int, breakMinutes: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var workText by remember { mutableStateOf(initial?.workMinutes?.toString() ?: "") }
    var breakText by remember { mutableStateOf(initial?.breakMinutes?.toString() ?: "") }

    val work = workText.toIntOrNull() ?: 0
    val brk = breakText.toIntOrNull() ?: 0
    val canSave = name.isNotBlank() && work > 0 && brk > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = workText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) workText = input },
                    label = { Text(stringResource(R.string.label_work_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { input -> if (input.all(Char::isDigit)) breakText = input },
                    label = { Text(stringResource(R.string.label_break_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.common_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, work, brk) }, enabled = canSave) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
