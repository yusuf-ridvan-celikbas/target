package com.ridvan.target.ui.focustimer

import android.view.WindowManager
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.reading.BookDialog
import com.ridvan.target.ui.reading.ReadingPagesDialog
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import kotlinx.coroutines.flow.flowOf

/** Flow Timer: a count-up study clock with no rounds — it runs until the user stops it. */
@Composable
fun FlowTimerScreen(
    shellNavigation: ShellNavigation,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: FlowTimerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val runningSession by viewModel.runningSession.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val pendingPageEntry by viewModel.pendingPageEntry.collectAsStateWithLifecycle()
    val getReadyRemaining by viewModel.getReadyRemainingMillis.collectAsStateWithLifecycle()

    var isReadingMode by rememberSaveable { mutableStateOf(false) }
    var linkMode by remember { mutableStateOf(SessionLinkMode.NONE) }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedLanguageId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var showAddBook by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<FlowSessionLink?>(null) }

    val topics by remember(linkMode, selectedCourseId, selectedLanguageId) {
        when (linkMode) {
            SessionLinkMode.COURSE -> selectedCourseId?.let { viewModel.topicsForCourse(it) } ?: flowOf(emptyList())
            SessionLinkMode.LANGUAGE -> selectedLanguageId?.let { viewModel.topicsForLanguage(it) } ?: flowOf(emptyList())
            SessionLinkMode.NONE -> flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Keep the screen on while counting, same as Focus Timer.
    val isRunning = runningSession != null
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(isRunning) {
        val window = context.findActivity()?.window
        if (isRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val undoDelete = rememberUndoDelete(snackbarHostState, viewModel::deleteSession, viewModel::restoreSession)
    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.FLOW_TIMER,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        title = stringResource(R.string.label_flow_timer),
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_focus_timer_settings))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val session = runningSession
            if (session == null) {
                Text(
                    stringResource(R.string.flow_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(false, stringResource(R.string.flow_mode_study)),
                        SegmentedToggleOption(true, stringResource(R.string.label_reading)),
                    ),
                    selected = isReadingMode,
                    onSelect = { isReadingMode = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                )
                if (isReadingMode) {
                    BookCard(
                        books = books,
                        selectedBookId = selectedBookId,
                        onSelectBook = { selectedBookId = it },
                        onAddBook = { showAddBook = true },
                    )
                } else {
                    SessionLinkCard(
                        linkMode = linkMode,
                        onSelectLinkMode = {
                            linkMode = it
                            selectedCourseId = null
                            selectedLanguageId = null
                            selectedTopicId = null
                        },
                        courses = courses,
                        selectedCourseId = selectedCourseId,
                        onSelectCourse = { selectedCourseId = it; selectedTopicId = null },
                        languages = languages,
                        selectedLanguageId = selectedLanguageId,
                        onSelectLanguage = { selectedLanguageId = it; selectedTopicId = null },
                        topics = topics,
                        selectedTopicId = selectedTopicId,
                        onSelectTopic = { selectedTopicId = it },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.focustimer_keep_screen_hint), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val topicName = topics.firstOrNull { it.id == selectedTopicId }?.name
                        pendingStart = if (isReadingMode) {
                            val book = books.firstOrNull { it.book.id == selectedBookId } ?: return@Button
                            FlowSessionLink(
                                bookId = book.book.id,
                                bookTitle = book.book.title,
                                startPage = book.currentPage,
                                totalPages = book.book.totalPages,
                            )
                        } else when (linkMode) {
                            SessionLinkMode.COURSE -> FlowSessionLink(
                                courseId = selectedCourseId,
                                courseName = courses.firstOrNull { it.id == selectedCourseId }?.name,
                                topicId = selectedTopicId,
                                topicName = topicName,
                            )
                            SessionLinkMode.LANGUAGE -> FlowSessionLink(
                                languageId = selectedLanguageId,
                                languageName = languages.firstOrNull { it.id == selectedLanguageId }?.name,
                                topicId = selectedTopicId,
                                topicName = topicName,
                            )
                            SessionLinkMode.NONE -> FlowSessionLink()
                        }
                    },
                    enabled = !isReadingMode || selectedBookId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.focustimer_start))
                }

                Spacer(Modifier.height(24.dp))
                HistorySection(
                    history = recentHistory,
                    onOpenHistory = onOpenHistory,
                    onSessionClick = onSessionClick,
                    linkSource = ManualSessionLinkSource(courses, languages, viewModel::topicsForCourse, viewModel::topicsForLanguage, books),
                    onAddManual = viewModel::addManualSession,
                    onDeleteSession = undoDelete,
                )
            } else {
                FlowRunningContent(
                    session = session,
                    clock = clock,
                    onSwitchPhase = viewModel::switchPhase,
                    onPause = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onStop = viewModel::stopSession,
                    onCourseClick = onCourseClick,
                    onLanguageClick = onLanguageClick,
                    onTopicClick = onTopicClick,
                )
            }
        }
    }

    pendingStart?.let { link ->
        SessionStartDialog(
            timerLine = stringResource(if (link.bookId != null) R.string.flow_start_summary_reading else R.string.flow_start_summary),
            linkLine = if (link.bookTitle != null) {
                stringResource(R.string.reading_running_label, link.bookTitle, link.startPage ?: 0)
            } else {
                listOfNotNull(link.courseName, link.languageName, link.topicName).joinToString(" · ").ifEmpty { null }
            },
            onStart = {
                pendingStart = null
                viewModel.requestStart(link)
            },
            onDismiss = { pendingStart = null },
        )
    }
    getReadyRemaining?.let { remaining ->
        GetReadyDialog(remainingMillis = remaining, onCancel = viewModel::cancelGetReady)
    }

    if (showAddBook) {
        BookDialog(
            initial = null,
            onConfirm = { book ->
                viewModel.addBook(book) { selectedBookId = it }
                showAddBook = false
            },
            onDismiss = { showAddBook = false },
        )
    }

    pendingPageEntry?.let { entry ->
        ReadingPagesDialog(
            bookTitle = entry.bookTitle,
            startPage = entry.startPage,
            endPage = null,
            totalPages = entry.totalPages,
            startEditable = false,
            onConfirm = { start, end -> viewModel.savePageReached(entry, start, end) },
            onDismiss = viewModel::dismissPageEntry,
            dismissLabel = stringResource(R.string.reading_skip),
        )
    }
}

@Composable
private fun FlowRunningContent(
    session: RunningFlowSession,
    clock: FlowClock,
    onSwitchPhase: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    var showEndConfirm by remember { mutableStateOf(false) }
    val link = session.link
    val isReading = link.bookId != null
    val isWork = session.phase == FocusPhase.WORK
    val phaseLabel = when {
        !isWork -> stringResource(R.string.focustimer_phase_break)
        isReading -> stringResource(R.string.focustimer_phase_reading)
        else -> stringResource(R.string.focustimer_phase_work)
    }
    val phaseColor = if (isWork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Nothing runs out here, so the outline stays whole; it just frames the clock like Focus Timer's.
        CountdownBorder(progress = 1f, color = phaseColor, modifier = Modifier.fillMaxWidth(0.85f)) {
            Text(phaseLabel, style = MaterialTheme.typography.headlineSmall, color = phaseColor)
            Text(formatElapsed(clock.phaseMillis), style = MaterialTheme.typography.displayMedium)
            Text(
                pluralStringResource(R.plurals.flow_breaks_taken, session.breaksTaken, session.breaksTaken),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val linkLabel = if (link.bookTitle != null) {
            stringResource(R.string.reading_running_label, link.bookTitle, link.startPage ?: 0)
        } else {
            listOfNotNull(link.courseName, link.languageName, link.topicName).joinToString(" · ")
        }
        if (linkLabel.isNotEmpty()) {
            Text(
                linkLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        when {
                            link.topicId != null -> onTopicClick(link.topicId)
                            link.courseId != null -> onCourseClick(link.courseId)
                            link.languageId != null -> onLanguageClick(link.languageId)
                        }
                    },
            )
        }

        val workMinutes = (clock.workMillis / 60_000L).toInt()
        val breakMinutes = (clock.breakMillis / 60_000L).toInt()
        Text(
            stringResource(
                if (isReading) R.string.flow_totals_reading else R.string.flow_totals,
                formatDuration(workMinutes),
                formatDuration(breakMinutes),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = if (session.isPaused) onResume else onPause,
                modifier = Modifier.weight(1f).height(88.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (session.isPaused) R.string.focustimer_resume else R.string.focustimer_pause),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Button(
                onClick = { showEndConfirm = true },
                modifier = Modifier.weight(1f).height(88.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.focustimer_stop), style = MaterialTheme.typography.titleLarge)
            }
        }
        FilledTonalButton(
            onClick = onSwitchPhase,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(64.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(if (isWork) Icons.Filled.Coffee else Icons.Filled.Work, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    when {
                        isWork -> R.string.flow_take_break
                        isReading -> R.string.flow_back_to_reading
                        else -> R.string.flow_back_to_work
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text(stringResource(R.string.focustimer_end_session_confirm_title)) },
            text = { Text(stringResource(R.string.focustimer_end_session_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm = false
                    onStop()
                }) { Text(stringResource(R.string.focustimer_end_session)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** "0:04:31" — hours always shown, since a Flow stretch can run past an hour. */
private fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
}
