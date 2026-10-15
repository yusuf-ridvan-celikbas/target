package com.ridvan.target.ui.sports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.ExerciseWithSets
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutSet
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.focustimer.localDayToUtcMillis
import com.ridvan.target.ui.focustimer.utcMillisToLocalDay
import java.util.Calendar

private const val MINUTE_MILLIS = 60_000L

private fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Editable state of one exercise in the editor; numbers stay text while typing. */
data class ExerciseDraft(val key: Long, val name: String, val sets: List<SetDraft>)
data class SetDraft(val reps: String = "", val weight: String = "")

fun List<ExerciseWithSets>.toDrafts(): List<ExerciseDraft> = mapIndexed { index, item ->
    ExerciseDraft(
        key = index.toLong(),
        name = item.exercise.name,
        sets = item.sets.map { SetDraft(it.reps.toString(), if (it.weightKg > 0) formatWeightPlain(it.weightKg) else "") },
    )
}

private fun formatWeightPlain(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()

/** Drafts with a name and at least one valid set (reps >= 1) become real exercises. */
fun List<ExerciseDraft>.toExercises(): List<ExerciseWithSets> = mapNotNull { draft ->
    val name = draft.name.trim()
    val sets = draft.sets.mapNotNull { set ->
        val reps = set.reps.trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        WorkoutSet(exerciseId = 0, reps = reps, weightKg = parseDecimal(set.weight) ?: 0.0)
    }
    if (name.isEmpty() || sets.isEmpty()) null else newExercise(name, sets)
}

/**
 * Log (or, with [initial], edit) a workout by hand: sport, date, start time, duration, distance for
 * distance sports, exercises for Gym, effort and notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFormDialog(
    initial: Workout?,
    initialExercises: List<ExerciseWithSets>,
    onConfirm: (WorkoutForm) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val start = remember { Calendar.getInstance().apply { timeInMillis = initial?.startedAt ?: now.timeInMillis } }
    var sport by remember { mutableStateOf(initial?.sport ?: Sport.RUNNING) }
    var dayStartMillis by remember { mutableStateOf(dayStart(start.timeInMillis)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour = start.get(Calendar.HOUR_OF_DAY),
        initialMinute = start.get(Calendar.MINUTE),
        is24Hour = true,
    )
    val initialMinutes = initial?.let { minutesOf(it.durationSeconds) }
    var hoursText by remember { mutableStateOf(initialMinutes?.let { (it / 60).toString() } ?: "0") }
    var minutesText by remember { mutableStateOf(initialMinutes?.let { (it % 60).toString() } ?: "") }
    var distanceText by remember {
        mutableStateOf(
            initial?.distanceMeters?.let { m ->
                if (initial.sport == Sport.SWIMMING) m.toString() else formatKmPlain(m)
            } ?: "",
        )
    }
    var effort by remember { mutableStateOf(initial?.effort) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var drafts by remember { mutableStateOf(initialExercises.toDrafts()) }

    val totalMinutes = (hoursText.toIntOrNull() ?: 0) * 60 + (minutesText.toIntOrNull() ?: 0)
    val distanceMeters: Int? = parseDecimal(distanceText)?.let { value ->
        if (sport == Sport.SWIMMING) value.toInt() else Math.round(value * 1000).toInt()
    }
    val isValid = totalMinutes > 0
    val startedAtNow = dayStartMillis + (timeState.hour * 60 + timeState.minute) * MINUTE_MILLIS
    val isFuture = startedAtNow > System.currentTimeMillis()
    var confirmFuture by remember { mutableStateOf(false) }
    fun submit() {
        // Unchanged minutes keep the stopwatch's exact seconds.
        val seconds = if (initial != null && totalMinutes == initialMinutes) initial.durationSeconds else totalMinutes * 60
        onConfirm(
            WorkoutForm(
                sport = sport,
                startedAt = startedAtNow,
                durationSeconds = seconds,
                distanceMeters = distanceMeters,
                notes = notes.trim().ifEmpty { null },
                effort = effort,
                exercises = if (sport == Sport.GYM) drafts.toExercises() else emptyList(),
            ),
        )
    }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.sports_form_add_title else R.string.sports_form_edit_title)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).imePadding().verticalScroll(rememberScrollState())) {
                DropdownField(selectedLabel = sport.label(), label = stringResource(R.string.sports_field_sport)) { close ->
                    Sport.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            leadingIcon = { Icon(option.icon(), contentDescription = null) },
                            onClick = { sport = option; close() },
                        )
                    }
                }
                Text(
                    stringResource(R.string.planner_field_date),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    formatDate(dayStartMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(top = 2.dp).clickable { showDatePicker = true },
                )
                Text(
                    stringResource(R.string.planner_field_start_time),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TimeInput(state = timeState)
                if (isFuture) {
                    Text(
                        stringResource(R.string.sports_future_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.sports_field_hours)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.sports_field_minutes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (sport.isDistance) {
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                        label = {
                            Text(stringResource(if (sport == Sport.SWIMMING) R.string.sports_field_distance_m else R.string.sports_field_distance_km))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (sport == Sport.GYM) {
                    ExercisesEditor(drafts = drafts, onChange = { drafts = it }, modifier = Modifier.padding(top = 12.dp))
                }
                EffortRow(effort = effort, onChange = { effort = it })
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.sports_field_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { if (isFuture) confirmFuture = true else submit() },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )

    if (confirmFuture) {
        AlertDialog(
            properties = FormDialogProperties,
            onDismissRequest = { confirmFuture = false },
            title = { Text(stringResource(R.string.sports_future_title)) },
            text = { Text(stringResource(R.string.sports_future_message)) },
            confirmButton = { TextButton(onClick = { confirmFuture = false; submit() }) { Text(stringResource(R.string.sports_future_save_anyway)) } },
            dismissButton = { TextButton(onClick = { confirmFuture = false }) { Text(stringResource(R.string.sports_future_change_date)) } },
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localDayToUtcMillis(dayStartMillis))
        DatePickerDialog(
            properties = FormDialogProperties,
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dayStartMillis = utcMillisToLocalDay(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Plain km text for an edit field (no locale mark, so it parses back). */
private fun formatKmPlain(meters: Int): String =
    java.math.BigDecimal(meters).divide(java.math.BigDecimal(1000), 3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

@Composable
private fun EffortRow(effort: Int?, onChange: (Int?) -> Unit) {
    Text(
        stringResource(R.string.sports_field_effort),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 12.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        (1..5).forEach { level ->
            FilterChip(
                selected = effort == level,
                onClick = { onChange(if (effort == level) null else level) },
                label = { Text(level.toString()) },
            )
        }
    }
}

/** Exercises with their sets: name, then rows of reps x weight. Used by the form and the live finish dialog. */
@Composable
fun ExercisesEditor(drafts: List<ExerciseDraft>, onChange: (List<ExerciseDraft>) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.sports_exercises), style = MaterialTheme.typography.titleSmall)
        drafts.forEachIndexed { index, draft ->
            fun update(new: ExerciseDraft) = onChange(drafts.toMutableList().also { it[index] = new })
            androidx.compose.runtime.key(draft.key) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { update(draft.copy(name = it)) },
                            label = { Text(stringResource(R.string.sports_exercise_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onChange(drafts.filterIndexed { i, _ -> i != index }) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sports_exercise_remove))
                        }
                    }
                    draft.sets.forEachIndexed { setIndex, set ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(stringResource(R.string.sports_set_n, setIndex + 1), style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = set.reps,
                                onValueChange = { v ->
                                    update(draft.copy(sets = draft.sets.toMutableList().also { it[setIndex] = set.copy(reps = v.filter(Char::isDigit).take(3)) }))
                                },
                                label = { Text(stringResource(R.string.sports_set_reps)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = set.weight,
                                onValueChange = { v ->
                                    update(draft.copy(sets = draft.sets.toMutableList().also { it[setIndex] = set.copy(weight = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6)) }))
                                },
                                label = { Text(stringResource(R.string.sports_set_weight)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { update(draft.copy(sets = draft.sets.filterIndexed { i, _ -> i != setIndex })) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sports_set_remove))
                            }
                        }
                    }
                    TextButton(onClick = {
                        // A new set starts from the previous one, the usual case for repeated sets.
                        val last = draft.sets.lastOrNull() ?: SetDraft()
                        update(draft.copy(sets = draft.sets + last))
                    }) { Text(stringResource(R.string.sports_set_add)) }
                }
            }
        }
        TextButton(onClick = {
            val nextKey = (drafts.maxOfOrNull { it.key } ?: -1L) + 1
            onChange(drafts + ExerciseDraft(nextKey, "", listOf(SetDraft())))
        }) { Text(stringResource(R.string.sports_exercise_add)) }
    }
}
