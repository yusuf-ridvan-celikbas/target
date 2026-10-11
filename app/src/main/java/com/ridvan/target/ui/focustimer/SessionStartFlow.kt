package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ridvan.target.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Neither timer starts the moment Start is tapped: a "Start session?" summary first, then a
// short get-ready countdown. Countdown time is never counted toward the session.

const val GET_READY_MILLIS = 10_000L

/** Lives in a timer's ViewModel, so the countdown keeps running across drawer navigation. */
class GetReadyCountdown(private val scope: CoroutineScope) {
    private val _remainingMillis = MutableStateFlow<Long?>(null)

    /** Null when no countdown is running. */
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    private var job: Job? = null

    fun start(onFinished: () -> Unit) {
        job?.cancel()
        val endsAt = System.currentTimeMillis() + GET_READY_MILLIS
        _remainingMillis.value = GET_READY_MILLIS
        job = scope.launch {
            while (true) {
                val remaining = endsAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingMillis.value = null
                    onFinished()
                    break
                }
                _remainingMillis.value = remaining
                delay(250)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMillis.value = null
    }
}

/** "Start session?" with what is about to run: the timer line and, when set, what it's linked to. */
@Composable
internal fun SessionStartDialog(
    timerLine: String,
    linkLine: String?,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_start_title)) },
        text = {
            Column {
                Text(timerLine, style = MaterialTheme.typography.bodyLarge)
                Text(
                    linkLine ?: stringResource(R.string.focustimer_history_not_linked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (linkLine != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.session_start_get_ready_hint, (GET_READY_MILLIS / 1000).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = { Button(onClick = onStart) { Text(stringResource(R.string.focustimer_start)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** The 10…1 get-ready countdown; only Cancel closes it early. */
@Composable
internal fun GetReadyDialog(remainingMillis: Long, onCancel: () -> Unit) {
    val secondsLeft = ((remainingMillis + 999) / 1000).toInt()
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.session_get_ready_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.session_get_ready_message))
                CountdownBorder(
                    progress = (remainingMillis.toFloat() / GET_READY_MILLIS).coerceIn(0f, 1f),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.6f),
                ) {
                    Text(stringResource(R.string.focustimer_prompt_seconds, secondsLeft), style = MaterialTheme.typography.displayMedium)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) } },
    )
}
