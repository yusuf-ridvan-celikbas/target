package com.ridvan.target.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.StreakLine
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.daysUntilLabel
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.mockexam.formatNet
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import com.ridvan.target.ui.shell.rememberEnabledModules
import com.ridvan.target.ui.trips.formatMoney

/** One row on a hub page: an area of the app, with a live line about it ([live]) or, without one, what's in it. */
private class HubEntry(val titleRes: Int, val hintRes: Int, val icon: ImageVector, val onClick: () -> Unit, val live: String? = null)

/** The Study tab's first page — today's study, exam prep, the study timers and their statistics. */
@Composable
fun StudyHubScreen(shellNavigation: ShellNavigation, viewModel: HubViewModel = viewModel()) {
    val modules = rememberEnabledModules()
    val exams = AppModule.EXAMS_STUDY in modules
    val focus = AppModule.FOCUS in modules
    val upcoming by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val studied by viewModel.studied.collectAsStateWithLifecycle()
    val courseCount by viewModel.courseCount.collectAsStateWithLifecycle()
    val languageCount by viewModel.languageCount.collectAsStateWithLifecycle()
    val lastMockNet by viewModel.lastMockNet.collectAsStateWithLifecycle()
    val nextExam = upcoming.firstOrNull()?.takeIf { exams }?.let { stringResource(R.string.hub_next, it.label, daysUntilLabel(it.date)) }
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.STUDY_HUB,
        title = stringResource(R.string.label_study_group),
        header = {
            Text(stringResource(R.string.home_studied_today, formatDuration(studied.todayMinutes)), style = MaterialTheme.typography.titleMedium)
            StreakLine(studied.studyStreak, R.string.streak_study, R.string.streak_keep_study)
            nextExam?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            if (focus) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    FilledTonalButton(onClick = shellNavigation.onNavigateFocusTimer, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_focus_timer))
                    }
                    FilledTonalButton(onClick = shellNavigation.onNavigateFlowTimer, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_flow_timer))
                    }
                }
            }
        },
        groups = listOf(
            listOf(
                HubEntry(R.string.label_exams, R.string.hub_exams_hint, Icons.AutoMirrored.Filled.Assignment, shellNavigation.onNavigateExams, nextExam),
                HubEntry(
                    R.string.label_courses, R.string.hub_courses_hint, Icons.AutoMirrored.Filled.MenuBook, shellNavigation.onNavigateCourses,
                    courseCount.takeIf { it > 0 }?.let { countText(R.plurals.count_courses, it) },
                ),
                HubEntry(
                    R.string.label_languages, R.string.hub_languages_hint, Icons.Filled.Translate, shellNavigation.onNavigateLanguages,
                    languageCount.takeIf { it > 0 }?.let { countText(R.plurals.count_languages, it) },
                ),
            ).takeIf { exams }.orEmpty(),
            listOf(
                HubEntry(R.string.label_study_resources, R.string.hub_study_resources_hint, Icons.Filled.Bookmark, shellNavigation.onNavigateStudyResources),
                HubEntry(R.string.label_topics, R.string.hub_topics_hint, Icons.Filled.Topic, shellNavigation.onNavigateTopics),
            ).takeIf { exams }.orEmpty(),
            listOf(
                HubEntry(R.string.label_focus_timer, R.string.hub_focus_timer_hint, Icons.Filled.Timer, shellNavigation.onNavigateFocusTimer),
                HubEntry(R.string.label_flow_timer, R.string.hub_flow_timer_hint, Icons.Filled.AvTimer, shellNavigation.onNavigateFlowTimer),
                HubEntry(
                    R.string.focustimer_history_page_title, R.string.hub_study_history_hint, Icons.Filled.History, shellNavigation.onNavigateStudyHistory,
                    studied.weekMinutes.takeIf { it > 0 }?.let {
                        stringResource(R.string.hub_today_week, formatDuration(studied.todayMinutes), formatDuration(it))
                    },
                ),
            ).takeIf { focus }.orEmpty(),
            listOfNotNull(
                HubEntry(
                    R.string.label_statistics, R.string.hub_statistics_hint, Icons.Filled.BarChart, shellNavigation.onNavigateStatistics,
                    lastMockNet?.let { stringResource(R.string.hub_last_mock_net, formatNet(it)) },
                ).takeIf { exams },
            ),
        ),
    )
}

/** The Plan tab's first page — today's plans and routines, then the Planner and Routines. */
@Composable
fun PlanHubScreen(shellNavigation: ShellNavigation, viewModel: HubViewModel = viewModel()) {
    val plans by viewModel.plannerEvents.collectAsStateWithLifecycle()
    val routines by viewModel.todaysRoutines.collectAsStateWithLifecycle()
    val today = startOfTodayMillis()
    val plansToday = plans.count { it.occurrenceDateMillis == today }
    val routinesDone = routines.count { it.progress.total > 0 && it.progress.done == it.progress.total }
    val routinesLine = routines.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.hub_routines_today, routinesDone, it.size) }
    val nextPlan = plans.firstOrNull { !it.isCompleted }?.let { occurrence ->
        stringResource(
            R.string.hub_next,
            occurrence.event.title,
            listOfNotNull(daysUntilLabel(occurrence.occurrenceDateMillis), occurrence.timeRangeLabel).joinToString(" "),
        )
    }
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.PLAN_HUB,
        title = stringResource(R.string.tab_plan),
        header = if (plansToday > 0 || routinesLine != null) {
            {
                Text(countText(R.plurals.count_plans_today, plansToday), style = MaterialTheme.typography.titleMedium)
                routinesLine?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
            }
        } else null,
        groups = listOf(
            listOf(
                HubEntry(R.string.label_planner, R.string.hub_planner_hint, Icons.Filled.CalendarMonth, shellNavigation.onNavigatePlanner, nextPlan),
                HubEntry(R.string.label_routines, R.string.hub_routines_hint, Icons.Filled.Checklist, shellNavigation.onNavigateRoutines, routinesLine),
            ),
        ),
    )
}

/** The Life tab's first page — reading, trips and money, each with where it stands. */
@Composable
fun LifeHubScreen(shellNavigation: ShellNavigation, viewModel: HubViewModel = viewModel()) {
    val modules = rememberEnabledModules()
    val reading = AppModule.READING in modules
    val trips = AppModule.TRIPS in modules
    val money = AppModule.MONEY in modules
    val readingState by viewModel.reading.collectAsStateWithLifecycle()
    val studied by viewModel.studied.collectAsStateWithLifecycle()
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val moneyState by viewModel.money.collectAsStateWithLifecycle()
    val bookLine = readingState?.currentBook?.let { progress ->
        progress.book.totalPages?.let { stringResource(R.string.hub_book_progress, progress.book.title, progress.currentPage, it) }
            ?: stringResource(R.string.hub_book_page, progress.book.title, progress.currentPage)
    }
    val tripLine = trip?.let {
        val where = it.dayNumber?.let { day -> stringResource(R.string.trip_day_of, day, it.dayCount) } ?: daysUntilLabel(it.trip.startDate)
        "${it.trip.name} · $where"
    }
    val moneyLine = moneyState?.let { m ->
        val spent = formatMoney(m.spentMinor, m.currency)
        m.budgetMinor?.let { stringResource(R.string.hub_money_spent_of, spent, formatMoney(it, m.currency)) }
            ?: stringResource(R.string.hub_money_spent, spent)
    }
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.LIFE_HUB,
        title = stringResource(R.string.tab_life),
        groups = listOf(
            listOf(
                HubEntry(R.string.label_reading, R.string.hub_reading_hint, Icons.Filled.AutoStories, shellNavigation.onNavigateReading, bookLine),
                HubEntry(
                    R.string.label_reading_stats, R.string.hub_reading_stats_hint, Icons.Filled.BarChart, shellNavigation.onNavigateReadingStats,
                    studied.weekPages.takeIf { it > 0 }?.let { stringResource(R.string.hub_this_week, countText(R.plurals.count_pages, it)) },
                ),
            ).takeIf { reading }.orEmpty(),
            listOfNotNull(
                HubEntry(R.string.label_trips, R.string.hub_trips_hint, Icons.Filled.Luggage, shellNavigation.onNavigateTrips, tripLine).takeIf { trips },
                HubEntry(R.string.label_money, R.string.hub_money_hint, Icons.Filled.AccountBalanceWallet, shellNavigation.onNavigateMoney, moneyLine).takeIf { money },
            ),
        ),
    )
}

@Composable
private fun HubPage(
    navigation: ShellNavigation,
    destination: ShellDestination,
    title: String,
    groups: List<List<HubEntry>>,
    header: (@Composable () -> Unit)? = null,
) {
    AppShell(navigation = navigation, currentDestination = destination, title = title) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            header?.let { content ->
                GroupedCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) { content() }
                }
            }
            groups.filter { it.isNotEmpty() }.forEach { entries ->
                GroupedCard {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        HubRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HubRow(entry: HubEntry) {
    ListItem(
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        headlineContent = { Text(stringResource(entry.titleRes)) },
        supportingContent = {
            Text(
                entry.live ?: stringResource(entry.hintRes),
                color = if (entry.live != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        // Transparent so the GroupedCard's tint shows through (ListItem is opaque by default).
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = entry.onClick),
    )
}
