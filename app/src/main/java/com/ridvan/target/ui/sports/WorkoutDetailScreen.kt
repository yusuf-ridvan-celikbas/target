package com.ridvan.target.ui.sports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.common.formatTime

/** One workout: when, how long, distance and pace, effort, notes, and a gym workout's exercises. Owns Edit/Delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(onBack: () -> Unit, viewModel: WorkoutDetailViewModel = viewModel()) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.sport?.label() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = workout != null) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.sports_form_edit_title))
                    }
                    IconButton(onClick = { showDelete = true }, enabled = workout != null) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.sports_delete_title))
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = workout ?: return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        stringResource(R.string.focus_session_title, formatDate(current.startedAt), formatTime(current.startedAt)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (current.isLive) {
                        Text(
                            stringResource(R.string.sports_live_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DetailLine(stringResource(R.string.sports_detail_duration), formatDuration(minutesOf(current.durationSeconds).coerceAtLeast(1)))
                    current.distanceMeters?.takeIf { it > 0 }?.let {
                        DetailLine(stringResource(R.string.sports_detail_distance), distanceText(it, current.sport))
                    }
                    paceText(current.sport, current.durationSeconds, current.distanceMeters)?.let {
                        DetailLine(stringResource(R.string.sports_detail_pace), it)
                    }
                    current.effort?.let { DetailLine(stringResource(R.string.sports_field_effort), stringResource(R.string.sports_effort_value, it)) }
                }
            }
            if (exercises.isNotEmpty()) {
                GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.sports_exercises), style = MaterialTheme.typography.titleMedium)
                        exercises.forEach { item ->
                            Text(item.exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp))
                            Text(
                                setsText(item.sets),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            current.notes?.let { notes ->
                GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.sports_field_notes), style = MaterialTheme.typography.titleMedium)
                        Text(notes, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        if (showEdit) {
            WorkoutFormDialog(
                initial = current,
                initialExercises = exercises,
                onConfirm = { form -> viewModel.update(form); showEdit = false },
                onDismiss = { showEdit = false },
            )
        }
        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text(stringResource(R.string.sports_delete_title)) },
                text = { Text(stringResource(R.string.sports_delete_message)) },
                confirmButton = {
                    TextButton(onClick = { showDelete = false; viewModel.delete(onDone = onBack) }) {
                        Text(stringResource(R.string.common_delete))
                    }
                },
                dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
