package com.ridvan.target.ui.focustimer

import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.reading.BookDialog
import com.ridvan.target.ui.reading.BookProgress
import com.ridvan.target.ui.reading.ReadingPagesDialog
import com.ridvan.target.ui.reading.BookCover
import com.ridvan.target.ui.reading.bookProgressLabel
import androidx.compose.material.icons.filled.Add
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.HelpTooltip
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    shellNavigation: ShellNavigation,
    onManagePresets: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: FocusTimerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val runningSession by viewModel.runningSession.collectAsStateWithLifecycle()
    val remainingMillis by viewModel.remainingMillis.collectAsStateWithLifecycle()
    val promptRemainingMillis by viewModel.promptRemainingMillis.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val pendingPageEntry by viewModel.pendingPageEntry.collectAsStateWithLifecycle()

    var selectedPresetId by remember { mutableStateOf<Long?>(null) }
    var linkMode by remember { mutableStateOf(SessionLinkMode.NONE) }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedLanguageId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var showAddBook by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<PendingFocusStart?>(null) }
    val getReadyRemaining by viewModel.getReadyRemainingMillis.collectAsStateWithLifecycle()

    LaunchedEffect(presets) {
        if (selectedPresetId == null || presets.none { it.id == selectedPresetId }) {
            selectedPresetId = presets.firstOrNull()?.id
        }
    }

    val topics by remember(linkMode, selectedCourseId, selectedLanguageId) {
        when (linkMode) {
            SessionLinkMode.COURSE -> selectedCourseId?.let { viewModel.topicsForCourse(it) } ?: flowOf(emptyList())
            SessionLinkMode.LANGUAGE -> selectedLanguageId?.let { viewModel.topicsForLanguage(it) } ?: flowOf(emptyList())
            SessionLinkMode.NONE -> flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // The screen must not turn off/lock while a session is running, or the in-app alarm could
    // go unseen/unheard — this is foreground-only by design, no background alarm safety net.
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
        currentDestination = ShellDestination.FOCUS_TIMER,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        title = stringResource(R.string.label_focus_timer),
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
                IdleContent(
                    presets = presets,
                    selectedPresetId = selectedPresetId,
                    onSelectPreset = { selectedPresetId = it },
                    onManagePresets = onManagePresets,
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
                    books = books,
                    selectedBookId = selectedBookId,
                    onSelectBook = { selectedBookId = it },
                    onAddBook = { showAddBook = true },
                    onStart = {
                        val preset = presets.firstOrNull { it.id == selectedPresetId } ?: return@IdleContent
                        if (preset.isReading) {
                            val book = books.firstOrNull { it.book.id == selectedBookId } ?: return@IdleContent
                            pendingStart = PendingFocusStart(preset, null, book) {
                                viewModel.requestStart(preset, null, null, null, null, null, null, book)
                            }
                            return@IdleContent
                        }
                        val topicName = topics.firstOrNull { it.id == selectedTopicId }?.name
                        pendingStart = when (linkMode) {
                            SessionLinkMode.COURSE -> {
                                val courseName = courses.firstOrNull { it.id == selectedCourseId }?.name
                                PendingFocusStart(preset, listOfNotNull(courseName, topicName).joinToString(" · ").ifEmpty { null }) {
                                    viewModel.requestStart(preset, selectedCourseId, courseName, null, null, selectedTopicId, topicName)
                                }
                            }
                            SessionLinkMode.LANGUAGE -> {
                                val languageName = languages.firstOrNull { it.id == selectedLanguageId }?.name
                                PendingFocusStart(preset, listOfNotNull(languageName, topicName).joinToString(" · ").ifEmpty { null }) {
                                    viewModel.requestStart(preset, null, null, selectedLanguageId, languageName, selectedTopicId, topicName)
                                }
                            }
                            SessionLinkMode.NONE -> PendingFocusStart(preset, null) {
                                viewModel.requestStart(preset, null, null, null, null, null, null)
                            }
                        }
                    },
                )
            } else {
                RunningContent(
                    session = session,
                    remainingMillis = remainingMillis,
                    promptRemainingMillis = promptRemainingMillis,
                    onConfirmNextPhase = viewModel::confirmNextPhase,
                    onAdjustTime = viewModel::adjustTime,
                    onSkip = viewModel::skipToNextPhase,
                    onPause = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onStop = viewModel::stopSession,
                    onCourseClick = onCourseClick,
                    onLanguageClick = onLanguageClick,
                    onTopicClick = onTopicClick,
                )
            }

            // History is hidden while a session runs, keeping the running view focused on the timer.
            if (session == null) {
                Spacer(Modifier.height(24.dp))
                HistorySection(
                    history = recentHistory,
                    onOpenHistory = onOpenHistory,
                    onSessionClick = onSessionClick,
                    linkSource = ManualSessionLinkSource(courses, languages, viewModel::topicsForCourse, viewModel::topicsForLanguage, books),
                    onAddManual = viewModel::addManualSession,
                    onDeleteSession = undoDelete,
                )
            }
        }
    }

    pendingStart?.let { start ->
        SessionStartDialog(
            timerLine = "${start.preset.name} — ${presetSubtitle(start.preset)}",
            linkLine = start.book?.let { bookLine(it) } ?: start.linkLine,
            onStart = {
                pendingStart = null
                start.action()
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

    // A reading session just ended: ask which page was reached ("Skip" leaves it unrecorded, fixable later).
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
private fun IdleContent(
    presets: List<FocusPreset>,
    selectedPresetId: Long?,
    onSelectPreset: (Long) -> Unit,
    onManagePresets: () -> Unit,
    linkMode: SessionLinkMode,
    onSelectLinkMode: (SessionLinkMode) -> Unit,
    courses: List<Course>,
    selectedCourseId: Long?,
    onSelectCourse: (Long?) -> Unit,
    languages: List<Language>,
    selectedLanguageId: Long?,
    onSelectLanguage: (Long?) -> Unit,
    topics: List<Topic>,
    selectedTopicId: Long?,
    onSelectTopic: (Long?) -> Unit,
    books: List<BookProgress>,
    selectedBookId: Long?,
    onSelectBook: (Long) -> Unit,
    onAddBook: () -> Unit,
    onStart: () -> Unit,
) {
    val isReadingPreset = presets.firstOrNull { it.id == selectedPresetId }?.isReading == true
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.label_preset), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onManagePresets) { Text(stringResource(R.string.focustimer_preset_list_title)) }
    }
    if (presets.isEmpty()) {
        Text(
            stringResource(R.string.focustimer_no_presets_hint),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable(onClick = onManagePresets),
        )
    } else {
        DropdownField(
            selectedLabel = presets.firstOrNull { it.id == selectedPresetId }?.let {
                stringResource(R.string.focustimer_preset_row_subtitle, it.workMinutes, it.breakMinutes).let { subtitle -> "${it.name} — $subtitle" }
            } ?: stringResource(R.string.common_select),
        ) { closeMenu ->
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                presetSubtitle(preset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = { onSelectPreset(preset.id); closeMenu() },
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    if (isReadingPreset) {
        BookCard(books = books, selectedBookId = selectedBookId, onSelectBook = onSelectBook, onAddBook = onAddBook)
    } else {
        SessionLinkCard(
            linkMode = linkMode,
            onSelectLinkMode = onSelectLinkMode,
            courses = courses,
            selectedCourseId = selectedCourseId,
            onSelectCourse = onSelectCourse,
            languages = languages,
            selectedLanguageId = selectedLanguageId,
            onSelectLanguage = onSelectLanguage,
            topics = topics,
            selectedTopicId = selectedTopicId,
            onSelectTopic = onSelectTopic,
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.focustimer_keep_screen_hint), style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onStart,
        enabled = selectedPresetId != null && (!isReadingPreset || selectedBookId != null),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.focustimer_start))
    }
}

@Composable
private fun RunningContent(
    session: RunningFocusSession,
    remainingMillis: Long,
    promptRemainingMillis: Long,
    onConfirmNextPhase: () -> Unit,
    onAdjustTime: (minutes: Int, keepForRestOfSession: Boolean) -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    var showEndConfirm by remember { mutableStateOf(false) }
    var showCustomTime by remember { mutableStateOf(false) }
    var keepForRestOfSession by rememberSaveable { mutableStateOf(false) }
    val awaitingNextPhase = session.awaitingNextPhase
    val isWork = session.phase == FocusPhase.WORK

    // A reading session's "work" rounds are reading rounds — every work-wording below swaps accordingly.
    val isReading = session.bookId != null
    val workPhaseRes = if (isReading) R.string.focustimer_phase_reading else R.string.focustimer_phase_work
    val phaseLabel = if (session.phase == FocusPhase.WORK) stringResource(workPhaseRes) else stringResource(R.string.focustimer_phase_break)
    val phaseColor = if (session.phase == FocusPhase.WORK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    val progress = if (session.phaseDurationMillis > 0) {
        (remainingMillis.toFloat() / session.phaseDurationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CountdownBorder(
            progress = progress,
            color = phaseColor,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Text(phaseLabel, style = MaterialTheme.typography.headlineSmall, color = phaseColor)
            Text(formatCountdown(remainingMillis), style = MaterialTheme.typography.displayLarge)
            Text(stringResource(R.string.focustimer_cycle_count, session.cyclesCompleted), style = MaterialTheme.typography.bodyMedium)
        }

        val linkLabel = if (session.bookTitle != null) {
            stringResource(R.string.reading_running_label, session.bookTitle, session.startPage ?: 0)
        } else {
            listOfNotNull(session.courseName, session.languageName, session.topicName).joinToString(" · ")
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
                            session.topicId != null -> onTopicClick(session.topicId)
                            session.courseId != null -> onCourseClick(session.courseId)
                            session.languageId != null -> onLanguageClick(session.languageId)
                        }
                    },
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            HelpTooltip(R.string.help_tooltip_focustimer_controls, R.string.cd_help_focustimer_controls)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = if (session.isPaused) onResume else onPause,
                enabled = awaitingNextPhase == null,
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

        OutlinedButton(
            onClick = onSkip,
            enabled = awaitingNextPhase == null,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.SkipNext, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (isWork) R.string.focustimer_skip_to_break else if (isReading) R.string.focustimer_skip_to_reading else R.string.focustimer_skip_to_work))
        }

        Spacer(Modifier.height(16.dp))
        GroupedCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(if (isWork) (if (isReading) R.string.focustimer_add_time_reading else R.string.focustimer_add_time_work) else R.string.focustimer_add_time_break),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    TIME_STEPS.forEach { minutes ->
                        FilledTonalButton(
                            onClick = { onAdjustTime(minutes, keepForRestOfSession) },
                            enabled = awaitingNextPhase == null,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.focustimer_add_minutes, minutes)) }
                    }
                    FilledTonalButton(
                        onClick = { showCustomTime = true },
                        enabled = awaitingNextPhase == null,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.4f),
                    ) { Text(stringResource(R.string.focustimer_add_custom), maxLines = 1) }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    TIME_STEPS.forEach { minutes ->
                        OutlinedButton(
                            onClick = { onAdjustTime(-minutes, keepForRestOfSession) },
                            enabled = awaitingNextPhase == null,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.focustimer_subtract_minutes, minutes)) }
                    }
                    // Keeps the minus buttons lined up under the plus buttons.
                    Spacer(Modifier.weight(1.4f))
                }
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(false, stringResource(R.string.focustimer_scope_this_round)),
                        SegmentedToggleOption(true, stringResource(R.string.focustimer_scope_rest_of_session)),
                    ),
                    selected = keepForRestOfSession,
                    onSelect = { keepForRestOfSession = it },
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Text(
                    stringResource(if (isReading) R.string.focustimer_session_durations_reading else R.string.focustimer_session_durations, session.workMinutes, session.breakMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    // A phase just ended: the user has FOCUS_CONFIRM_TIMEOUT_MILLIS to confirm the next one,
    // otherwise the ViewModel ends and saves the session. Not dismissable by tapping outside/Back,
    // so an accidental dismiss can't silently leave the session in limbo.
    if (awaitingNextPhase != null) {
        val startsBreak = awaitingNextPhase == FocusPhase.BREAK
        val secondsLeft = ((promptRemainingMillis + 999) / 1000).toInt()
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(stringResource(if (startsBreak) R.string.focustimer_prompt_start_break_title else if (isReading) R.string.focustimer_prompt_start_reading_title else R.string.focustimer_prompt_start_work_title))
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.focustimer_prompt_message))
                    CountdownBorder(
                        progress = (promptRemainingMillis.toFloat() / FOCUS_CONFIRM_TIMEOUT_MILLIS).coerceIn(0f, 1f),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.6f),
                    ) {
                        Text(stringResource(R.string.focustimer_prompt_seconds, secondsLeft), style = MaterialTheme.typography.displaySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirmNextPhase) {
                    Text(stringResource(if (startsBreak) R.string.focustimer_start_break else if (isReading) R.string.focustimer_start_reading else R.string.focustimer_start_work))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = true }) { Text(stringResource(R.string.focustimer_end_session)) }
            },
        )
    }

    if (showCustomTime) {
        CustomTimeDialog(
            onConfirm = { minutes ->
                onAdjustTime(minutes, keepForRestOfSession)
                showCustomTime = false
            },
            onDismiss = { showCustomTime = false },
        )
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

@Composable
internal fun HistorySection(
    history: List<FocusSessionWithLinks>,
    onOpenHistory: () -> Unit,
    onSessionClick: (Long) -> Unit,
    linkSource: ManualSessionLinkSource,
    onAddManual: (ManualFocusSessionForm) -> Unit,
    onDeleteSession: ((FocusSession) -> Unit)? = null,
) {
    var showManualDialog by remember { mutableStateOf(false) }

    // The whole header row opens the full, filterable history page.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHistory)
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.focustimer_history_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.focustimer_history_recent_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.focustimer_history_see_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    // Studied without the timer? Log it by hand so History (and the Planner) still count it.
    TextButton(onClick = { showManualDialog = true }) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(stringResource(R.string.focus_manual_add_button), modifier = Modifier.padding(start = 8.dp))
    }
    if (showManualDialog) {
        FocusManualSessionDialog(
            linkSource = linkSource,
            onConfirm = {
                onAddManual(it)
                showManualDialog = false
            },
            onDismiss = { showManualDialog = false },
        )
    }
    Spacer(Modifier.height(8.dp))
    if (history.isEmpty()) {
        Text(stringResource(R.string.focustimer_history_recent_empty))
    } else {
        GroupedCard {
            history.forEach { item ->
                FocusHistoryRow(
                    item = item,
                    onClick = { onSessionClick(item.session.id) },
                    onDelete = onDeleteSession,
                )
                HorizontalDivider()
            }
        }
    }

}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private val TIME_STEPS = listOf(2, 5, 10)
private const val MAX_CUSTOM_ADD_MINUTES = 180

/** [onConfirm] receives a signed minute count — negative when Subtract is selected. */
@Composable
private fun CustomTimeDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var subtract by remember { mutableStateOf(false) }
    val minutes = text.toIntOrNull()
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focustimer_add_custom_title)) },
        text = {
            Column {
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(false, stringResource(R.string.common_add)),
                        SegmentedToggleOption(true, stringResource(R.string.focustimer_subtract)),
                    ),
                    selected = subtract,
                    onSelect = { subtract = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(3)
                        text = digits.toIntOrNull()?.coerceAtMost(MAX_CUSTOM_ADD_MINUTES)?.toString() ?: digits
                    },
                    label = { Text(stringResource(R.string.focustimer_add_custom_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let { onConfirm(if (subtract) -it else it) } }, enabled = minutes != null && minutes > 0) {
                Text(stringResource(R.string.focustimer_apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun presetSubtitle(preset: FocusPreset): String {
    val times = stringResource(R.string.focustimer_preset_row_subtitle, preset.workMinutes, preset.breakMinutes)
    return if (preset.isReading) "$times · ${stringResource(R.string.label_reading)}" else times
}

/** What the "Start session?" pop-up shows, and the start it runs once confirmed. */
private class PendingFocusStart(
    val preset: FocusPreset,
    val linkLine: String?,
    val book: BookProgress? = null,
    val action: () -> Unit,
)

/** "Book title · from page 12" for the start pop-up. */
@Composable
internal fun bookLine(book: BookProgress): String =
    stringResource(R.string.reading_running_label, book.book.title, book.currentPage)
