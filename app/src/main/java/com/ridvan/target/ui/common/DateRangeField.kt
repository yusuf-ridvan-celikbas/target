package com.ridvan.target.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.focustimer.localDayToUtcMillis
import com.ridvan.target.ui.focustimer.utcMillisToLocalDay

/**
 * One field for a start+end pair of local day-starts. Opens Material3's DateRangePicker (first tap = start,
 * second = end; a single tap saves a one-day range). Values are local start-of-day millis, converted to and
 * from the picker's UTC millis here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeField(
    label: String,
    startDay: Long,
    endDay: Long,
    onRangeChange: (startDay: Long, endDay: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Text(label, style = MaterialTheme.typography.labelMedium, modifier = modifier.padding(top = 10.dp))
    OutlinedButton(onClick = { open = true }, modifier = modifier.fillMaxWidth()) {
        Text("${formatDate(startDay)} – ${formatDate(endDay)}")
    }
    if (open) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = localDayToUtcMillis(startDay),
            initialSelectedEndDateMillis = localDayToUtcMillis(endDay),
        )
        DatePickerDialog(
            properties = FormDialogProperties,
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis?.let(::utcMillisToLocalDay)
                        val end = state.selectedEndDateMillis?.let(::utcMillisToLocalDay) ?: start
                        if (start != null && end != null) onRangeChange(start, end)
                        open = false
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DateRangePicker(state = state, modifier = Modifier.weight(1f, fill = false)) }
    }
}
