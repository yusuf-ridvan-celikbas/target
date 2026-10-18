package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.ridvan.target.R
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatTime
import java.util.Calendar
import java.util.TimeZone

/** "Planned · Oct 18, 2026 14:00", or "Planned · no start time". */
@Composable
fun mockExamPlannedText(scheduledAt: Long?): String =
    if (scheduledAt == null) {
        stringResource(R.string.mockexam_planned_no_time)
    } else {
        stringResource(R.string.mockexam_planned_at, formatDate(scheduledAt), formatTime(scheduledAt))
    }

/** Optional planned start: pick a date, then a time; Clear removes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTimeField(value: Long?, onValueChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDayUtc by remember { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.mockexam_start_time_optional), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Text(value?.let { "${formatDate(it)} ${formatTime(it)}" } ?: stringResource(R.string.common_tap_to_set))
            }
            if (value != null) {
                TextButton(onClick = { onValueChange(null) }, modifier = Modifier.padding(start = 4.dp)) {
                    Text(stringResource(R.string.common_clear))
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::localToUtcDay) ?: System.currentTimeMillis())
        DatePickerDialog(
            properties = FormDialogProperties,
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDayUtc = state.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    pickedDayUtc?.let { dayUtc ->
        val current = value?.let { Calendar.getInstance().apply { timeInMillis = it } }
        val state = rememberTimePickerState(
            initialHour = current?.get(Calendar.HOUR_OF_DAY) ?: 10,
            initialMinute = current?.get(Calendar.MINUTE) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            properties = FormDialogProperties,
            onDismissRequest = { pickedDayUtc = null },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(combine(dayUtc, state.hour, state.minute))
                    pickedDayUtc = null
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickedDayUtc = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** DatePicker works in UTC midnights; turn one plus a local hour/minute into local epoch millis. */
private fun combine(dayUtc: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dayUtc }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute)
    }.timeInMillis
}

private fun localToUtcDay(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
