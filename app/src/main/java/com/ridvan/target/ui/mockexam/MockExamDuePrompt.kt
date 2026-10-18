package com.ridvan.target.ui.mockexam

import android.app.Application
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.shell.rememberEnabledModules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** How long after its planned start a deneme still asks "start now?". */
private const val DUE_WINDOW_MILLIS = 6 * 60 * 60 * 1000L
private const val TICK_MILLIS = 15_000L

@OptIn(ExperimentalCoroutinesApi::class)
class MockExamDueViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val mockExamDao = app.database.mockExamDao()

    private val ticks = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS)
        }
    }

    // Re-read each tick: the user can sign in or switch accounts while this lives at the app root.
    private val userId = flow {
        while (true) {
            emit(app.preferences.currentUserId)
            delay(TICK_MILLIS)
        }
    }.distinctUntilChanged()

    /** Asked-and-declined this app session; they stay planned and can be started from their page. */
    private val dismissed = MutableStateFlow(emptySet<Long>())

    val due: StateFlow<MockExam?> = combine(
        userId.flatMapLatest { id -> id?.let { mockExamDao.getScheduledForUser(it) } ?: flowOf(emptyList()) },
        ticks,
        dismissed,
    ) { planned, now, skipped ->
        planned.firstOrNull { mock ->
            val at = mock.scheduledAt ?: return@firstOrNull false
            mock.id !in skipped && at <= now && now - at < DUE_WINDOW_MILLIS
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismiss(id: Long) {
        dismissed.value = dismissed.value + id
    }
}

/** App-wide "It's time for <deneme>. Start now?" — Yes opens the timer (10 s get-ready, then the countdown). */
@Composable
fun MockExamDuePrompt(onStart: (Long) -> Unit, viewModel: MockExamDueViewModel = viewModel()) {
    if (AppModule.EXAMS_STUDY !in rememberEnabledModules()) return
    val due by viewModel.due.collectAsStateWithLifecycle()
    val mock = due ?: return
    AlertDialog(
        onDismissRequest = { viewModel.dismiss(mock.id) },
        title = { Text(stringResource(R.string.mockexam_due_title, mock.name)) },
        text = { Text(stringResource(R.string.mockexam_start_message, formatDuration(mock.timeLimitMinutes))) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.dismiss(mock.id)
                onStart(mock.id)
            }) { Text(stringResource(R.string.mockexam_start)) }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismiss(mock.id) }) { Text(stringResource(R.string.mockexam_not_now)) }
        },
    )
}
