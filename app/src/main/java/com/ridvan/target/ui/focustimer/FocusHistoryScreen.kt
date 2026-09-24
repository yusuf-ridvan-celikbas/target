package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

private enum class FocusHistoryPeriod(val daysBeforeToday: Int?) {
    LAST_7_DAYS(6),
    LAST_30_DAYS(29),
    ALL(null),
}

// Subject filter keys — plain strings so they survive rememberSaveable.
private const val SUBJECT_ALL = "all"
private const val SUBJECT_NONE = "none"
private fun courseSubjectKey(courseId: Long) = "course:$courseId"
private fun languageSubjectKey(languageId: Long) = "language:$languageId"

private fun subjectKeyOf(item: FocusSessionWithLinks): String = when {
    item.session.courseId != null -> courseSubjectKey(item.session.courseId)
    item.session.languageId != null -> languageSubjectKey(item.session.languageId)
    else -> SUBJECT_NONE
}

/** Reached from Focus Timer's History header — a back-arrow drill-down. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHistoryScreen(
    onBack: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focustimer_history_page_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        FocusHistoryBody(innerPadding, onCourseClick, onLanguageClick, onTopicClick)
    }
}

/** Reached from the drawer's Focus group — its own shell-level route, so it keeps the drawer chrome. */
@Composable
fun FocusHistoryHomeScreen(
    shellNavigation: ShellNavigation,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
) {
    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.STUDY_HISTORY,
        title = stringResource(R.string.focustimer_history_page_title),
    ) { innerPadding ->
        FocusHistoryBody(innerPadding, onCourseClick, onLanguageClick, onTopicClick)
    }
}

@Composable
private fun FocusHistoryBody(
    innerPadding: PaddingValues,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit,
    viewModel: FocusHistoryViewModel = viewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(FocusHistoryPeriod.ALL) }
    var subjectKey by rememberSaveable { mutableStateOf(SUBJECT_ALL) }
    var presetName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<FocusSessionWithLinks?>(null) }

    // Subject options come from what's actually in history, so every choice has at least one match.
    val subjectOptions: List<Pair<String, String>> = history
        .mapNotNull { item ->
            when {
                item.session.courseId != null && item.courseName != null ->
                    courseSubjectKey(item.session.courseId) to courseDisplayName(item.courseName)
                item.session.languageId != null && item.languageName != null ->
                    languageSubjectKey(item.session.languageId) to item.languageName
                else -> null
            }
        }
        .distinctBy { it.first }
        .sortedBy { it.second.lowercase() }
    val presetOptions = history.map { it.session.presetName }.distinct().sortedBy { it.lowercase() }

    // Search matches the preset name plus the linked course (raw and translated), language and topic.
    val searchText: Map<Long, String> = history.associate { item ->
        item.session.id to listOfNotNull(
            item.session.presetName,
            item.courseName,
            item.courseName?.let { courseDisplayName(it) },
            item.languageName,
            item.topicName,
        ).joinToString(" ").lowercase()
    }

    val cutoff = period.daysBeforeToday?.let { startOfTodayMillis() - it * 24L * 60 * 60 * 1000 }
    val trimmedQuery = query.trim().lowercase()
    val filtered = history.filter { item ->
        (cutoff == null || item.session.startedAt >= cutoff) &&
            (subjectKey == SUBJECT_ALL || subjectKeyOf(item) == subjectKey) &&
            (presetName == null || item.session.presetName == presetName) &&
            (trimmedQuery.isEmpty() || searchText[item.session.id].orEmpty().contains(trimmedQuery))
    }
    val filtersActive = query.isNotBlank() || period != FocusHistoryPeriod.ALL || subjectKey != SUBJECT_ALL || presetName != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.focustimer_history_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SegmentedToggle(
            options = listOf(
                SegmentedToggleOption(FocusHistoryPeriod.LAST_7_DAYS, stringResource(R.string.focustimer_history_period_7_days)),
                SegmentedToggleOption(FocusHistoryPeriod.LAST_30_DAYS, stringResource(R.string.focustimer_history_period_30_days)),
                SegmentedToggleOption(FocusHistoryPeriod.ALL, stringResource(R.string.focustimer_history_period_all)),
            ),
            selected = period,
            onSelect = { period = it },
        )
        FilterField(
            label = stringResource(R.string.focustimer_history_filter_subject),
            selectedLabel = when (subjectKey) {
                SUBJECT_ALL -> stringResource(R.string.focustimer_history_all_subjects)
                SUBJECT_NONE -> stringResource(R.string.focustimer_history_not_linked)
                else -> subjectOptions.firstOrNull { it.first == subjectKey }?.second
                    ?: stringResource(R.string.focustimer_history_all_subjects)
            },
            options = listOf(
                SUBJECT_ALL to stringResource(R.string.focustimer_history_all_subjects),
                SUBJECT_NONE to stringResource(R.string.focustimer_history_not_linked),
            ) + subjectOptions,
            onSelect = { subjectKey = it },
        )
        FilterField(
            label = stringResource(R.string.focustimer_history_filter_preset),
            selectedLabel = presetName ?: stringResource(R.string.focustimer_history_all_presets),
            options = listOf<Pair<String?, String>>(null to stringResource(R.string.focustimer_history_all_presets)) +
                presetOptions.map { it to it },
            onSelect = { presetName = it },
        )

        Spacer(Modifier.height(16.dp))
        val totalWork = filtered.sumOf { it.session.totalWorkMinutes }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    R.string.focustimer_history_summary,
                    filtered.size,
                    stringResource(R.string.duration_format, totalWork / 60, totalWork % 60),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (filtersActive) {
                TextButton(onClick = {
                    query = ""
                    period = FocusHistoryPeriod.ALL
                    subjectKey = SUBJECT_ALL
                    presetName = null
                }) { Text(stringResource(R.string.focustimer_history_clear_filters)) }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            history.isEmpty() -> Text(stringResource(R.string.focustimer_history_empty))
            filtered.isEmpty() -> Text(stringResource(R.string.focustimer_history_no_matches))
            else -> GroupedCard {
                filtered.forEach { item ->
                    FocusHistoryRow(
                        item = item,
                        onCourseClick = onCourseClick,
                        onLanguageClick = onLanguageClick,
                        onTopicClick = onTopicClick,
                        onDeleteClick = { pendingDelete = item },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { item ->
        FocusSessionDeleteDialog(
            item = item,
            onConfirm = {
                viewModel.deleteSession(item.session)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Same label-over-value dropdown look as Statistics' filter fields. */
@Composable
private fun <T> FilterField(label: String, selectedLabel: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(selectedLabel, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(text = { Text(optionLabel) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
