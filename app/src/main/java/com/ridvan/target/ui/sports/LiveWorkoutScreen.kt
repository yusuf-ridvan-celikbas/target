package com.ridvan.target.ui.sports

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.focustimer.GetReadyDialog
import com.ridvan.target.ui.focustimer.SessionStartDialog
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

/** Live workout: pick a sport, Start (confirm + get-ready), count up, Pause/Resume, Stop, then add distance or exercises. */
@Composable
fun LiveWorkoutScreen(shellNavigation: ShellNavigation, viewModel: LiveWorkoutViewModel = viewModel()) {
    val context = LocalContext.current
    val running by viewModel.running.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedMillis.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val savedMessage by viewModel.savedMessage.collectAsStateWithLifecycle()
    val getReadyRemaining by viewModel.getReadyRemainingMillis.collectAsStateWithLifecycle()

    var sport by rememberSaveable { mutableStateOf(Sport.RUNNING) }
    var pendingStart by remember { mutableStateOf<Sport?>(null) }
    var showStopConfirm by remember { mutableStateOf(false) }

    // Keep the screen on while counting, same as the Flow Timer.
    val isRunning = running != null
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(isRunning) {
        val window = context.findActivity()?.window
        if (isRunning) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.sports_live_saved)
    LaunchedEffect(savedMessage) {
        if (savedMessage) {
            snackbarHostState.showSnackbar(savedText)
            viewModel.acknowledgeSaved()
        }
    }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.SPORTS,
        title = stringResource(R.string.sports_live_title),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            val current = running
            if (current == null) {
                Text(
                    stringResource(R.string.sports_live_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DropdownField(
                    selectedLabel = sport.label(),
                    label = stringResource(R.string.sports_field_sport),
                    modifier = Modifier.padding(top = 16.dp),
                ) { close ->
                    Sport.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            leadingIcon = { Icon(option.icon(), contentDescription = null) },
                            onClick = { sport = option; close() },
                        )
                    }
                }
                Text(
                    stringResource(R.string.focustimer_keep_screen_hint),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Button(onClick = { pendingStart = sport }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.focustimer_start))
                }
            } else {
                GroupedCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Icon(current.sport.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(current.sport.label(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(formatHms(elapsed / 1000), style = MaterialTheme.typography.displayMedium)
                        if (current.isPaused) {
                            Text(
                                stringResource(R.string.sports_live_paused),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = if (current.isPaused) viewModel::resume else viewModel::pause,
                        modifier = Modifier.weight(1f).height(88.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(if (current.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(if (current.isPaused) R.string.focustimer_resume else R.string.focustimer_pause),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Button(
                        onClick = { showStopConfirm = true },
                        modifier = Modifier.weight(1f).height(88.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.focustimer_stop), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }

    pendingStart?.let { chosen ->
        SessionStartDialog(
            timerLine = stringResource(R.string.sports_live_start_summary, chosen.label()),
            linkLine = null,
            onStart = {
                pendingStart = null
                viewModel.requestStart(chosen)
            },
            onDismiss = { pendingStart = null },
        )
    }
    getReadyRemaining?.let { remaining -> GetReadyDialog(remainingMillis = remaining, onCancel = viewModel::cancelGetReady) }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text(stringResource(R.string.focustimer_end_session_confirm_title)) },
            text = { Text(stringResource(R.string.focustimer_end_session_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showStopConfirm = false; viewModel.stop() }) { Text(stringResource(R.string.focustimer_end_session)) }
            },
            dismissButton = { TextButton(onClick = { showStopConfirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    finished?.let { done -> FinishDialog(done, onSave = viewModel::finish) }
}

/** After Stop: distance for distance sports, exercises for Gym. Skip saves the workout as it is. */
@Composable
private fun FinishDialog(done: FinishedWorkout, onSave: (Int?, List<com.ridvan.target.data.local.dao.ExerciseWithSets>) -> Unit) {
    var distanceText by remember { mutableStateOf("") }
    var drafts by remember { mutableStateOf(emptyList<ExerciseDraft>()) }
    val isSwim = done.sport == Sport.SWIMMING
    val distance = parseDecimal(distanceText)?.let { if (isSwim) it.toInt() else Math.round(it * 1000).toInt() }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = {},
        title = { Text(stringResource(R.string.sports_live_finish_title, done.sport.label())) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(formatHms(done.durationSeconds.toLong()), style = MaterialTheme.typography.titleLarge)
                if (done.sport.isDistance) {
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                        label = { Text(stringResource(if (isSwim) R.string.sports_field_distance_m else R.string.sports_field_distance_km)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                } else {
                    ExercisesEditor(drafts = drafts, onChange = { drafts = it }, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(distance, drafts.toExercises()) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = { onSave(null, emptyList()) }) { Text(stringResource(R.string.sports_live_skip)) } },
    )
}
