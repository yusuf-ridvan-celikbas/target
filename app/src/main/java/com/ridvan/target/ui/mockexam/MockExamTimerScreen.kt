package com.ridvan.target.ui.mockexam

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.focustimer.GetReadyDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamTimerScreen(
    onFinished: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MockExamTimerViewModel = viewModel(),
) {
    val mockExam by viewModel.mockExam.collectAsStateWithLifecycle()
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val remaining by viewModel.remainingMillis.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val getReadyMillis by viewModel.getReadyMillis.collectAsStateWithLifecycle()
    val cancelled by viewModel.cancelled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showFinishConfirm by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(finished) {
        if (finished) mockExam?.let { onFinished(it.id) }
    }
    LaunchedEffect(cancelled) {
        if (cancelled) onBack()
    }
    BackHandler { if (getReadyMillis != null) viewModel.cancelGetReady() else showLeaveDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mockExam?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val limitMillis = (mockExam?.timeLimitMinutes ?: 0) * 60_000L
            val left = remaining ?: limitMillis
            Text(
                stringResource(R.string.mockexam_time_left),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 32.dp),
            )
            Text(
                formatClock(left),
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
            )
            if (limitMillis > 0) {
                LinearProgressIndicator(
                    progress = { (1f - left.toFloat() / limitMillis).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                stringResource(R.string.mockexam_elapsed, formatClock((limitMillis - left).coerceAtLeast(0))),
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                parts.forEach { part ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CourseIconAvatar(part.courseIcon)
                        Text(courseDisplayName(part.courseName), modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Text(countText(R.plurals.count_questions, part.part.questionCount))
                    }
                }
            }
            Button(
                onClick = { showFinishConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.mockexam_finish)) }
        }
    }

    getReadyMillis?.let { GetReadyDialog(remainingMillis = it, onCancel = viewModel::cancelGetReady) }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text(stringResource(R.string.mockexam_finish_title)) },
            text = { Text(stringResource(R.string.mockexam_finish_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirm = false
                    viewModel.finishEarly()
                }) { Text(stringResource(R.string.mockexam_finish)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.mockexam_leave_title)) },
            text = { Text(stringResource(R.string.mockexam_leave_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onBack()
                }) { Text(stringResource(R.string.mockexam_leave_keep_running)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showLeaveDialog = false
                        viewModel.discard(onBack)
                    }) { Text(stringResource(R.string.mockexam_discard)) }
                    TextButton(onClick = { showLeaveDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            },
        )
    }
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
}
