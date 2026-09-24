package com.ridvan.target.ui.focustimer

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import kotlinx.coroutines.flow.flowOf

private enum class LinkMode { NONE, COURSE, LANGUAGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    shellNavigation: ShellNavigation,
    onManagePresets: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: FocusTimerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val runningSession by viewModel.runningSession.collectAsStateWithLifecycle()
    val remainingMillis by viewModel.remainingMillis.collectAsStateWithLifecycle()
    val alarmSoundUri by viewModel.focusAlarmSoundUri.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.focusVibrationEnabled.collectAsStateWithLifecycle()

    var selectedPresetId by remember { mutableStateOf<Long?>(null) }
    var linkMode by remember { mutableStateOf(LinkMode.NONE) }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedLanguageId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(presets) {
        if (selectedPresetId == null || presets.none { it.id == selectedPresetId }) {
            selectedPresetId = presets.firstOrNull()?.id
        }
    }

    val topics by remember(linkMode, selectedCourseId, selectedLanguageId) {
        when (linkMode) {
            LinkMode.COURSE -> selectedCourseId?.let { viewModel.topicsForCourse(it) } ?: flowOf(emptyList())
            LinkMode.LANGUAGE -> selectedLanguageId?.let { viewModel.topicsForLanguage(it) } ?: flowOf(emptyList())
            LinkMode.NONE -> flowOf(emptyList())
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

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (uri != null) viewModel.setFocusAlarmSoundUri(uri.toString())
    }
    val audioFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setFocusAlarmSoundUri(uri.toString())
        }
    }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.FOCUS_TIMER,
        title = stringResource(R.string.label_focus_timer),
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
                    alarmSoundUri = alarmSoundUri,
                    vibrationEnabled = vibrationEnabled,
                    onChooseRingtone = {
                        ringtoneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                alarmSoundUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                            },
                        )
                    },
                    onBrowseAudioFile = { audioFileLauncher.launch(arrayOf("audio/*")) },
                    onResetSound = { viewModel.setFocusAlarmSoundUri(null) },
                    onSetVibration = { viewModel.setFocusVibrationEnabled(it) },
                    onStart = {
                        val preset = presets.firstOrNull { it.id == selectedPresetId } ?: return@IdleContent
                        val topicName = topics.firstOrNull { it.id == selectedTopicId }?.name
                        when (linkMode) {
                            LinkMode.COURSE -> {
                                val courseName = courses.firstOrNull { it.id == selectedCourseId }?.name
                                viewModel.startSession(preset, selectedCourseId, courseName, null, null, selectedTopicId, topicName)
                            }
                            LinkMode.LANGUAGE -> {
                                val languageName = languages.firstOrNull { it.id == selectedLanguageId }?.name
                                viewModel.startSession(preset, null, null, selectedLanguageId, languageName, selectedTopicId, topicName)
                            }
                            LinkMode.NONE -> viewModel.startSession(preset, null, null, null, null, null, null)
                        }
                    },
                )
            } else {
                RunningContent(
                    session = session,
                    remainingMillis = remainingMillis,
                    onPause = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onStop = viewModel::stopSession,
                    onCourseClick = onCourseClick,
                    onLanguageClick = onLanguageClick,
                    onTopicClick = onTopicClick,
                )
            }

            Spacer(Modifier.height(24.dp))
            HistorySection(
                history = history,
                onCourseClick = onCourseClick,
                onLanguageClick = onLanguageClick,
                onTopicClick = onTopicClick,
                onDelete = viewModel::deleteHistorySession,
            )
        }
    }
}

@Composable
private fun IdleContent(
    presets: List<FocusPreset>,
    selectedPresetId: Long?,
    onSelectPreset: (Long) -> Unit,
    onManagePresets: () -> Unit,
    linkMode: LinkMode,
    onSelectLinkMode: (LinkMode) -> Unit,
    courses: List<Course>,
    selectedCourseId: Long?,
    onSelectCourse: (Long?) -> Unit,
    languages: List<Language>,
    selectedLanguageId: Long?,
    onSelectLanguage: (Long?) -> Unit,
    topics: List<Topic>,
    selectedTopicId: Long?,
    onSelectTopic: (Long?) -> Unit,
    alarmSoundUri: String?,
    vibrationEnabled: Boolean,
    onChooseRingtone: () -> Unit,
    onBrowseAudioFile: () -> Unit,
    onResetSound: () -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
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
                    text = { Text(preset.name) },
                    onClick = { onSelectPreset(preset.id); closeMenu() },
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.focustimer_link_section_title), style = MaterialTheme.typography.titleMedium)
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(LinkMode.NONE, stringResource(R.string.common_none)),
                    SegmentedToggleOption(LinkMode.COURSE, stringResource(R.string.label_course)),
                    SegmentedToggleOption(LinkMode.LANGUAGE, stringResource(R.string.label_language)),
                ),
                selected = linkMode,
                onSelect = onSelectLinkMode,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            if (linkMode == LinkMode.COURSE) {
                Text(stringResource(R.string.label_course), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                DropdownField(
                    selectedLabel = courses.firstOrNull { it.id == selectedCourseId }?.name ?: stringResource(R.string.common_select),
                ) { closeMenu ->
                    courses.forEach { course ->
                        DropdownMenuItem(text = { Text(course.name) }, onClick = { onSelectCourse(course.id); closeMenu() })
                    }
                }
            } else if (linkMode == LinkMode.LANGUAGE) {
                Text(stringResource(R.string.label_language), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                DropdownField(
                    selectedLabel = languages.firstOrNull { it.id == selectedLanguageId }?.name ?: stringResource(R.string.common_select),
                ) { closeMenu ->
                    languages.forEach { language ->
                        DropdownMenuItem(text = { Text(language.name) }, onClick = { onSelectLanguage(language.id); closeMenu() })
                    }
                }
            }

            if (linkMode != LinkMode.NONE && (selectedCourseId != null || selectedLanguageId != null)) {
                Text(stringResource(R.string.label_link_to_topic), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                DropdownField(
                    selectedLabel = topics.firstOrNull { it.id == selectedTopicId }?.name ?: stringResource(R.string.common_none),
                ) { closeMenu ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.common_none)) }, onClick = { onSelectTopic(null); closeMenu() })
                    topics.forEach { topic ->
                        DropdownMenuItem(text = { Text(topic.name) }, onClick = { onSelectTopic(topic.id); closeMenu() })
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.focustimer_alarm_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                if (alarmSoundUri == null) stringResource(R.string.focustimer_sound_default) else stringResource(R.string.focustimer_sound_custom),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            OutlinedButton(onClick = onChooseRingtone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.focustimer_choose_ringtone))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBrowseAudioFile, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.focustimer_browse_audio))
            }
            if (alarmSoundUri != null) {
                TextButton(onClick = onResetSound, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.focustimer_reset_sound))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.label_vibration), modifier = Modifier.weight(1f))
                Switch(checked = vibrationEnabled, onCheckedChange = onSetVibration)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.focustimer_keep_screen_hint), style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(8.dp))
    Button(onClick = onStart, enabled = selectedPresetId != null, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.focustimer_start))
    }
}

@Composable
private fun RunningContent(
    session: RunningFocusSession,
    remainingMillis: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    val phaseLabel = if (session.phase == FocusPhase.WORK) stringResource(R.string.focustimer_phase_work) else stringResource(R.string.focustimer_phase_break)
    val phaseColor = if (session.phase == FocusPhase.WORK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(phaseLabel, style = MaterialTheme.typography.headlineSmall, color = phaseColor)
        Text(formatCountdown(remainingMillis), style = MaterialTheme.typography.displayLarge)
        Text(stringResource(R.string.focustimer_cycle_count, session.cyclesCompleted), style = MaterialTheme.typography.bodyMedium)

        val linkLabel = listOfNotNull(session.courseName, session.languageName, session.topicName).joinToString(" · ")
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

        Spacer(Modifier.height(24.dp))
        Row {
            OutlinedButton(onClick = if (session.isPaused) onResume else onPause) {
                Text(stringResource(if (session.isPaused) R.string.focustimer_resume else R.string.focustimer_pause))
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onStop) { Text(stringResource(R.string.focustimer_stop)) }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<FocusSession>,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    onDelete: (FocusSession) -> Unit,
) {
    var pendingDeleteSession by remember { mutableStateOf<FocusSession?>(null) }

    Text(stringResource(R.string.focustimer_history_title), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    if (history.isEmpty()) {
        Text(stringResource(R.string.focustimer_history_empty))
    } else {
        GroupedCard {
            history.forEach { session ->
                HistoryRow(
                    session = session,
                    onCourseClick = onCourseClick,
                    onLanguageClick = onLanguageClick,
                    onTopicClick = onTopicClick,
                    onDeleteClick = { pendingDeleteSession = session },
                )
                HorizontalDivider()
            }
        }
    }

    pendingDeleteSession?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSession = null },
            title = { Text(stringResource(R.string.focustimer_delete_session_title)) },
            text = { Text(stringResource(R.string.focustimer_delete_session_message, session.presetName, formatDate(session.startedAt))) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(session)
                    pendingDeleteSession = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSession = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    session: FocusSession,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
) {
    val workText = stringResource(R.string.duration_format, session.totalWorkMinutes / 60, session.totalWorkMinutes % 60)
    val breakText = stringResource(R.string.duration_format, session.totalBreakMinutes / 60, session.totalBreakMinutes % 60)
    val hasLink = session.courseId != null || session.languageId != null
    ListItem(
        headlineContent = { Text("${session.presetName} — ${formatDate(session.startedAt)}") },
        supportingContent = {
            Text(stringResource(R.string.focustimer_history_row_subtitle, session.cyclesCompleted, workText, breakText))
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_focus_session))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (hasLink) {
            Modifier.clickable {
                when {
                    session.topicId != null -> onTopicClick(session.topicId)
                    session.courseId != null -> onCourseClick(session.courseId)
                    session.languageId != null -> onLanguageClick(session.languageId)
                }
            }
        } else {
            Modifier
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(selectedLabel: String, items: @Composable (closeMenu: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(selectedLabel, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items { expanded = false }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
