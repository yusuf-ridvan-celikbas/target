package com.ridvan.target.ui.home

import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextOverflow
import com.ridvan.target.ui.reading.bookProgressLabel
import com.ridvan.target.ui.reading.BookCover
import com.ridvan.target.ui.reading.goalTodayLine
import com.ridvan.target.ui.reading.minutesFor
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.routines.RoutineIcon
import com.ridvan.target.ui.routines.routineProgressText
import com.ridvan.target.ui.trips.minuteLabel
import com.ridvan.target.ui.common.formatTime
import com.ridvan.target.ui.focustimer.focusSessionTitle
import com.ridvan.target.ui.planner.PlannerAgendaItem
import com.ridvan.target.ui.planner.StudiedPreview
import androidx.compose.material.icons.automirrored.filled.Assignment
import com.ridvan.target.ui.mockexam.formatNet
import com.ridvan.target.ui.mockexam.mockExamNet
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.home.HomeCard
import com.ridvan.target.data.home.HomeItem
import com.ridvan.target.data.home.groupHomeBlocks
import com.ridvan.target.data.home.isEnabled
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.sports.goalProgressText
import com.ridvan.target.ui.sports.label
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.daysUntil
import com.ridvan.target.ui.common.daysUntilLabel
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.planner.PlannerPreviewOccurrence
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    shellNavigation: ShellNavigation,
    onExamClick: (Long) -> Unit,
    onSectionClick: (Long) -> Unit,
    onMockExamClick: (Long) -> Unit,
    onFocusSessionClick: (Long) -> Unit,
    onPracticeSessionClick: (Long) -> Unit,
    onBookClick: (Long) -> Unit,
    onOpenReading: () -> Unit,
    onTopicClick: (Long) -> Unit,
    onRoutineClick: (Long) -> Unit,
    onShortcut: (HomeItem.Shortcut) -> Unit,
    onOpenMoney: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val homeLayout by viewModel.homeLayout.collectAsStateWithLifecycle()
    val shortcutTargets by viewModel.shortcutTargets.collectAsStateWithLifecycle()
    val money by viewModel.money.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    val todaysRoutines by viewModel.todaysRoutines.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val upcomingPlannerEvents by viewModel.upcomingPlannerEvents.collectAsStateWithLifecycle()
    val studied by viewModel.studied.collectAsStateWithLifecycle()
    val reading by viewModel.reading.collectAsStateWithLifecycle()
    val workOnNext by viewModel.workOnNext.collectAsStateWithLifecycle()
    val modules = com.ridvan.target.ui.shell.rememberEnabledModules()
    val examsOn = com.ridvan.target.data.modules.AppModule.EXAMS_STUDY in modules
    val focusOn = com.ridvan.target.data.modules.AppModule.FOCUS in modules
    val readingOn = com.ridvan.target.data.modules.AppModule.READING in modules
    val sportsState by viewModel.sports.collectAsStateWithLifecycle()
    val shownSports = sportsState.takeIf { com.ridvan.target.data.modules.AppModule.SPORTS in modules }

    val prefix = stringResource(R.string.home_welcome_prefix)
    val suffix = stringResource(R.string.home_welcome_suffix)
    val dateText = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.HOME,
        titleContent = {
            Column {
                Text(
                    buildAnnotatedString {
                        append(prefix)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(currentUser?.preferredName ?: "") }
                        append(suffix)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = { editing = !editing }) {
                Icon(
                    if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                    contentDescription = stringResource(if (editing) R.string.home_edit_done else R.string.home_edit),
                )
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (editing) {
                HomeEditor(
                    items = homeLayout,
                    modules = modules,
                    targets = shortcutTargets,
                    onChange = viewModel::saveHomeLayout,
                    onReset = viewModel::resetHomeLayout,
                )
                return@Column
            }
            val blocks = groupHomeBlocks(homeLayout.filter { it.isEnabled(modules) })
            if (blocks.isEmpty()) {
                Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyMedium)
            }
            blocks.forEach { block ->
                val first = block.first()
                if (first is HomeItem.Shortcut) {
                    ShortcutGrid(block.filterIsInstance<HomeItem.Shortcut>(), shortcutTargets, onShortcut)
                    return@forEach
                }
                when ((first as HomeItem.Card).card) {
                    HomeCard.COUNTDOWNS -> {
                        val shown = upcomingEvents.take(HOME_COUNTDOWNS_MAX)
                        if (shown.isNotEmpty()) {
                            UpcomingEventsCard(
                                events = shown,
                                showSeeAll = upcomingEvents.size > shown.size,
                                onSeeAll = shellNavigation.onNavigateExams,
                                onEventClick = { event ->
                                    when {
                                        event.mockExamId != null -> onMockExamClick(event.mockExamId)
                                        event.sectionId != null -> onSectionClick(event.sectionId)
                                        else -> onExamClick(event.examId)
                                    }
                                },
                            )
                        }
                    }
                    HomeCard.PLANS -> {
                        // Today's plans, or tomorrow's when today has none.
                        val todays = upcomingPlannerEvents.filter { daysUntil(it.occurrenceDateMillis) == 0L }
                        val plans = todays.ifEmpty { upcomingPlannerEvents.filter { daysUntil(it.occurrenceDateMillis) == 1L } }
                        if (plans.isNotEmpty()) {
                            PlansCard(
                                title = stringResource(if (todays.isEmpty()) R.string.home_plans_tomorrow else R.string.home_plans_today),
                                occurrences = plans.take(4),
                                onToggleDone = viewModel::togglePlannerOccurrenceDone,
                                onOpenPlanner = shellNavigation.onNavigatePlanner,
                            )
                        }
                    }
                    HomeCard.ROUTINES -> if (todaysRoutines.isNotEmpty()) RoutinesCard(todaysRoutines, onRoutineClick)
                    HomeCard.TODAY_SO_FAR -> {
                        val hasStudied = (focusOn || examsOn) && (studied.todayMinutes > 0 || studied.weekMinutes > 0 || studied.today.isNotEmpty() || studied.studyStreak.current > 0)
                        val shownReading = reading.takeIf { readingOn }
                        if (hasStudied || shownReading != null || shownSports != null) {
                            TodaySoFarCard(
                                studied = studied,
                                reading = shownReading,
                                sports = shownSports,
                                onOpenSports = shellNavigation.onNavigateSports,
                                showStudied = hasStudied,
                                onBookClick = onBookClick,
                                onOpenReading = onOpenReading,
                                onStudiedClick = { item ->
                                    when (item) {
                                        is PlannerAgendaItem.FocusSessionEntry -> onFocusSessionClick(item.item.session.id)
                                        is PlannerAgendaItem.PracticeLogEntry -> onPracticeSessionClick(item.row.practiceLog.studyResourceTopicId)
                                        is PlannerAgendaItem.MockExamFinishedEntry -> onMockExamClick(item.summary.mockExam.id)
                                    }
                                },
                            )
                        }
                    }
                    HomeCard.WORK_ON_NEXT -> if (workOnNext.isNotEmpty()) {
                        WorkOnNextCard(workOnNext, onItemClick = { item ->
                            if (item.studyResourceTopicId != null) onPracticeSessionClick(item.studyResourceTopicId) else onTopicClick(item.topicId)
                        })
                    }
                    HomeCard.QUICK_START -> QuickStartCard(shellNavigation.onNavigateFocusTimer, shellNavigation.onNavigateFlowTimer)
                    HomeCard.MONEY_MONTH -> money?.let { MoneyMonthCard(it, onOpenMoney) }
                }
            }
        }
    }
}

private const val HOME_COUNTDOWNS_MAX = 3


@Composable
private fun PlansCard(
    title: String,
    occurrences: List<PlannerPreviewOccurrence>,
    onToggleDone: (PlannerPreviewOccurrence) -> Unit,
    onOpenPlanner: () -> Unit,
) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.home_open_planner),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onOpenPlanner),
                )
            }
            occurrences.forEach { occurrence ->
                PlannerPreviewRow(occurrence, onToggleDone = { onToggleDone(occurrence) }, onOpenPlanner = onOpenPlanner)
            }
        }
    }
}

/** One row per routine on today: "Morning routine · 07:00 · 2/5 done"; tap opens its checklist. */
@Composable
private fun RoutinesCard(routines: List<TodayRoutine>, onRoutineClick: (Long) -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_routines_title), style = MaterialTheme.typography.titleMedium)
            routines.forEach { item ->
                val routine = item.routine
                val allDone = item.progress.total > 0 && item.progress.done == item.progress.total
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onRoutineClick(routine.id) }.padding(top = 12.dp),
                ) {
                    RoutineIcon(
                        routine.category,
                        tint = if (allDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        listOfNotNull(
                            routine.name,
                            routine.startMinuteOfDay?.let { minuteLabel(it) },
                            routineProgressText(item.progress),
                            item.streak.current.takeIf { it > 0 }?.let { stringResource(R.string.streak_short, it) },
                        )
                            .joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (allDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySoFarCard(
    studied: StudiedPreview,
    reading: HomeReading?,
    sports: HomeSports?,
    onOpenSports: () -> Unit,
    showStudied: Boolean,
    onBookClick: (Long) -> Unit,
    onOpenReading: () -> Unit,
    onStudiedClick: (PlannerAgendaItem.Studied) -> Unit,
) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_today_so_far), style = MaterialTheme.typography.titleMedium)
            if (showStudied) StudiedSection(studied, onStudiedClick)
            reading?.let { ReadingSection(studied, it, onBookClick, onOpenReading, onStudiedClick) }
            sports?.let { SportsSection(it, onOpenSports) }
        }
    }
}

/** Today's workout time, then each Sports goal as "Running: 12 / 20 km this week". */
@Composable
private fun SportsSection(sports: HomeSports, onOpenSports: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSports).padding(top = 12.dp)) {
        if (sports.todayCount > 0) {
            Text(
                stringResource(
                    R.string.home_sports_today,
                    countText(R.plurals.count_workouts, sports.todayCount),
                    durationText(sports.todayMinutes.coerceAtLeast(1)),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        sports.goals.take(3).forEach { line ->
            val goal = line.goal
            Text(
                stringResource(
                    if (goal.period == com.ridvan.target.data.local.entity.GoalPeriod.WEEK) R.string.home_sports_goal_week else R.string.home_sports_goal_month,
                    goal.sport?.label() ?: stringResource(R.string.sports_goal_any),
                    goalProgressText(goal, line.progress),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (line.progress.reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun WorkOnNextCard(items: List<WorkOnNextItem>, onItemClick: (WorkOnNextItem) -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_work_next_title), style = MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                val owner = if (item.isCourse) courseDisplayName(item.ownerName) else item.ownerName
                val reason = item.accuracyPercent?.let { stringResource(R.string.home_work_accuracy, it) }
                    ?: stringResource(R.string.home_work_left, countText(R.plurals.count_questions, item.questionsLeft))
                Column(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }.padding(top = 10.dp)) {
                    Text(item.topicName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$owner · $reason",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.accuracyPercent != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartCard(onFocus: () -> Unit, onFlow: () -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_quick_start), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onFocus, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.label_focus_timer), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onFlow, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.label_flow_timer), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun PlannerPreviewRow(occurrence: PlannerPreviewOccurrence, onToggleDone: () -> Unit, onOpenPlanner: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "plannerPreviewLabelColor",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = occurrence.isCompleted, onCheckedChange = { onToggleDone() })
        Text(
            occurrence.event.title,
            color = labelColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenPlanner),
        )
        val days = daysUntil(occurrence.occurrenceDateMillis)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                daysUntilLabel(occurrence.occurrenceDateMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatDate(occurrence.occurrenceDateMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if ((days == 0L || days == 1L) && occurrence.timeRangeLabel != null) {
                Text(
                    occurrence.timeRangeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpcomingEventsCard(events: List<UpcomingEvent>, showSeeAll: Boolean, onSeeAll: () -> Unit, onEventClick: (UpcomingEvent) -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_countdowns_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (showSeeAll) {
                    Text(
                        stringResource(R.string.focustimer_history_see_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(onClick = onSeeAll),
                    )
                }
            }
            events.forEach { event ->
                UpcomingEventRow(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
private fun UpcomingEventRow(event: UpcomingEvent, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "upcomingEventLabelColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            event.label,
            color = labelColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                daysUntilLabel(event.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatDate(event.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** What was actually studied today (Focus + Practice Sessions, reading excluded), with today's and this week's totals. */
@Composable
private fun StudiedSection(studied: StudiedPreview, onItemClick: (PlannerAgendaItem.Studied) -> Unit) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.home_studied_today, durationText(studied.todayMinutes)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.home_studied_week, durationText(studied.weekMinutes)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    com.ridvan.target.ui.common.StreakLine(studied.studyStreak, R.string.streak_study, R.string.streak_keep_study)
    if (studied.today.isEmpty()) {
        Text(
            stringResource(R.string.home_studied_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    } else {
        studied.today.take(3).forEach { item -> StudiedPreviewRow(item, onClick = { onItemClick(item) }) }
    }
}

@Composable
private fun StudiedPreviewRow(item: PlannerAgendaItem.Studied, onClick: () -> Unit) {
    val (icon, label, startMillis) = when (item) {
        is PlannerAgendaItem.FocusSessionEntry -> Triple(
            if (item.isReading) Icons.Filled.AutoStories else Icons.Filled.Timer,
            focusSessionTitle(item.item),
            item.item.session.startedAt,
        )
        is PlannerAgendaItem.PracticeLogEntry -> Triple(
            Icons.Filled.Quiz,
            listOfNotNull(item.row.courseName?.let { courseDisplayName(it) } ?: item.row.languageName, item.row.topicName)
                .joinToString(" · "),
            item.row.practiceLog.loggedAt,
        )
        is PlannerAgendaItem.MockExamFinishedEntry -> Triple(
            Icons.AutoMirrored.Filled.Assignment,
            "${item.summary.mockExam.name} · " + stringResource(
                R.string.mockexam_total_net,
                formatNet(mockExamNet(item.summary.correctCount, item.summary.wrongCount)),
            ),
            item.summary.mockExam.takenAt,
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "studiedPreviewLabelColor",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = labelColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Text(
            "${formatTime(startMillis)} · ${durationText(item.studiedMinutes)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun durationText(minutes: Int): String = formatDuration(minutes)

/** Pages read today / this week, and the book currently being read with its progress. */
@Composable
private fun ReadingSection(
    studied: StudiedPreview,
    reading: HomeReading,
    onBookClick: (Long) -> Unit,
    onOpenReading: () -> Unit,
    onSessionClick: (PlannerAgendaItem.Studied) -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenReading),
    ) {
        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.home_reading_today, countText(R.plurals.count_pages, studied.todayPages), durationText(studied.todayReadingMinutes)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Text(
            stringResource(R.string.home_reading_week, countText(R.plurals.count_pages, studied.weekPages), durationText(studied.weekReadingMinutes)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    com.ridvan.target.ui.common.StreakLine(studied.readingStreak, R.string.streak_reading, R.string.streak_keep_reading)
    studied.todayReading.take(3).forEach { item -> StudiedPreviewRow(item, onClick = { onSessionClick(item) }) }
    val book = reading.currentBook
    if (book != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { onBookClick(book.book.id) },
        ) {
            BookCover(book.book, percent = book.percent, compact = true, elevation = 2.dp, modifier = Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        book.book.title,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        bookProgressLabel(book),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                book.percent?.let { percent ->
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                reading.goal?.let { goal ->
                    Text(
                        if (goal.done || goal.passed) {
                            goalTodayLine(goal, reading.pagesPerHour)
                        } else {
                            listOfNotNull(
                                stringResource(R.string.home_reading_goal, goal.readToday, goal.target),
                                minutesFor(goal.remainingToday, reading.pagesPerHour)
                                    ?.let { stringResource(R.string.reading_goal_minutes_left, it) },
                            ).joinToString(" · ")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (goal.done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** This month in Money: spent, income and (with a budget) what's left; tap opens Money. */
@Composable
private fun MoneyMonthCard(money: HomeMoney, onOpen: () -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp)) {
            Text(stringResource(R.string.home_card_money), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.home_money_line,
                    com.ridvan.target.ui.trips.formatMoney(money.spentMinor, money.currency),
                    com.ridvan.target.ui.trips.formatMoney(money.incomeMinor, money.currency),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            money.budgetMinor?.let { budget ->
                val left = budget - money.spentMinor
                Text(
                    if (left >= 0) stringResource(R.string.money_budget_left, com.ridvan.target.ui.trips.formatMoney(left, money.currency), com.ridvan.target.ui.trips.formatMoney(budget, money.currency))
                    else stringResource(R.string.money_budget_over, com.ridvan.target.ui.trips.formatMoney(-left, money.currency), com.ridvan.target.ui.trips.formatMoney(budget, money.currency)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
