package com.ridvan.target.ui.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineCategory
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption

/** Add ([initial] null) or edit a routine; the caller supplies userId/id on save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineFormDialog(initial: Routine?, onConfirm: (Routine) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: RoutineCategory.MORNING) }
    var hasTime by remember { mutableStateOf(initial?.startMinuteOfDay != null) }
    var days by remember { mutableStateOf(parseRepeatDays(initial?.repeatDays ?: Routine.ALL_DAYS)) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }
    val startMinute = initial?.startMinuteOfDay ?: (7 * 60)
    val timeState = rememberTimePickerState(initialHour = startMinute / 60, initialMinute = startMinute % 60, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.routine_new_title else R.string.routine_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.routine_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SegmentedToggle(
                    options = RoutineCategory.entries.map { SegmentedToggleOption(it, stringResource(routineCategoryLabelRes(it))) },
                    selected = category,
                    onSelect = { category = it },
                    textStyle = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.routine_field_start_time), modifier = Modifier.weight(1f))
                    Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                }
                if (hasTime) TimeInput(state = timeState)
                Text(stringResource(R.string.routine_field_days), style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(shortWeekdayName(day)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.routine_field_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(R.string.routine_field_active), modifier = Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && days.isNotEmpty(),
                onClick = {
                    val base = initial ?: Routine(userId = 0, name = "")
                    onConfirm(
                        base.copy(
                            name = name.trim(),
                            category = category,
                            startMinuteOfDay = if (hasTime) timeState.hour * 60 + timeState.minute else null,
                            repeatDays = repeatDaysToString(days),
                            notes = notes.trim().ifEmpty { null },
                            isActive = isActive,
                        )
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
