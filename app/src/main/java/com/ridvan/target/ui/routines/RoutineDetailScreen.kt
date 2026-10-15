package com.ridvan.target.ui.routines

import com.ridvan.target.ui.common.FormDialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(onBack: () -> Unit, viewModel: RoutineDetailViewModel = viewModel()) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val doneIds by viewModel.doneStepIds.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var editRoutine by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingSteps by remember { mutableStateOf(false) }
    // A step being added (id 0) or renamed; null = no dialog.
    var stepDialog by remember { mutableStateOf<RoutineStep?>(null) }
    var stepToDelete by remember { mutableStateOf<RoutineStep?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { editRoutine = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.routine_edit_title))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                text = { Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val r = routine ?: return@Scaffold
        val progress = RoutineProgress(steps.count { it.id in doneIds }, steps.size)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoutineIcon(r.category, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text(
                            stringResource(routineCategoryLabelRes(r.category)),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        if (!r.isActive) {
                            Text(stringResource(R.string.routine_paused), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(routineScheduleLine(r), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    r.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.routine_steps_title), style = MaterialTheme.typography.titleSmall)
                    if (viewModel.dayMillis != startOfTodayMillis()) {
                        Text(formatDate(viewModel.dayMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(routineProgressText(progress), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (steps.isNotEmpty()) {
                    TextButton(onClick = { editingSteps = !editingSteps }) {
                        Text(stringResource(if (editingSteps) R.string.routine_done_editing else R.string.routine_edit_steps))
                    }
                }
            }
            if (steps.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            if (!editingSteps) {
                                val done = step.id in doneIds
                                Checkbox(checked = done, onCheckedChange = { viewModel.setStepDone(step, it) })
                                Text(
                                    step.name,
                                    textDecoration = if (done) TextDecoration.LineThrough else null,
                                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                                )
                            } else {
                                Text(step.name, modifier = Modifier.weight(1f).padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                                IconButton(onClick = { viewModel.moveStep(step, -1) }, enabled = index > 0) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_up))
                                }
                                IconButton(onClick = { viewModel.moveStep(step, 1) }, enabled = index < steps.lastIndex) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_down))
                                }
                                IconButton(onClick = { stepDialog = step }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.routine_edit_step))
                                }
                                IconButton(onClick = { stepToDelete = step }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.routine_delete_step), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    if (steps.isEmpty()) {
                        Text(
                            stringResource(R.string.routine_no_steps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            if (editingSteps || steps.isEmpty()) {
                TextButton(onClick = { stepDialog = RoutineStep(routineId = r.id, name = "") }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.routine_add_step), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }

    val r = routine ?: return
    if (editRoutine) {
        RoutineFormDialog(initial = r, onConfirm = { viewModel.updateRoutine(it); editRoutine = false }, onDismiss = { editRoutine = false })
    }
    stepDialog?.let { step ->
        var name by remember(step) { mutableStateOf(step.name) }
        AlertDialog(
            properties = FormDialogProperties,
            onDismissRequest = { stepDialog = null },
            title = { Text(stringResource(if (step.id == 0L) R.string.routine_add_step else R.string.routine_edit_step)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.routine_step_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.saveStep(name.trim(), step.takeIf { it.id != 0L })
                        stepDialog = null
                    },
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { stepDialog = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    stepToDelete?.let { step ->
        AlertDialog(
            onDismissRequest = { stepToDelete = null },
            title = { Text(stringResource(R.string.routine_delete_step)) },
            text = { Text(stringResource(R.string.routine_delete_step_message, step.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStep(step)
                    stepToDelete = null
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { stepToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.routine_delete)) },
            text = { Text(stringResource(R.string.routine_delete_message, r.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteRoutine()
                    onBack()
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
