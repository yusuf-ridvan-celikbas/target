package com.ridvan.target.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.shell.rememberEnabledModules
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

/** One row on a hub page: an area of the app, with a line saying what's in it. */
private class HubEntry(val titleRes: Int, val hintRes: Int, val icon: ImageVector, val onClick: () -> Unit)

/** The Study tab's first page — every exam-prep area, one tap away. */
@Composable
fun StudyHubScreen(shellNavigation: ShellNavigation) {
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.STUDY_HUB,
        title = stringResource(R.string.label_study_group),
        groups = listOf(
            listOfNotNull(
                HubEntry(R.string.label_exams, R.string.hub_exams_hint, Icons.AutoMirrored.Filled.Assignment, shellNavigation.onNavigateExams),
                HubEntry(R.string.label_courses, R.string.hub_courses_hint, Icons.AutoMirrored.Filled.MenuBook, shellNavigation.onNavigateCourses),
                HubEntry(R.string.label_languages, R.string.hub_languages_hint, Icons.Filled.Translate, shellNavigation.onNavigateLanguages),
            ),
            listOfNotNull(
                HubEntry(R.string.label_study_resources, R.string.hub_study_resources_hint, Icons.Filled.Bookmark, shellNavigation.onNavigateStudyResources),
                HubEntry(R.string.label_topics, R.string.hub_topics_hint, Icons.Filled.Topic, shellNavigation.onNavigateTopics),
            ),
        ),
    )
}

/** The Plan tab's first page — the Planner and trips. */
@Composable
fun PlanHubScreen(shellNavigation: ShellNavigation) {
    val modules = rememberEnabledModules()
    val planner = AppModule.PLANNER_ROUTINES in modules
    val trips = AppModule.TRIPS in modules
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.PLAN_HUB,
        title = stringResource(R.string.tab_plan),
        groups = listOf(
            listOfNotNull(
                HubEntry(R.string.label_planner, R.string.hub_planner_hint, Icons.Filled.CalendarMonth, shellNavigation.onNavigatePlanner).takeIf { planner },
                HubEntry(R.string.label_trips, R.string.hub_trips_hint, Icons.Filled.Luggage, shellNavigation.onNavigateTrips).takeIf { trips },
                HubEntry(R.string.label_routines, R.string.hub_routines_hint, Icons.Filled.Checklist, shellNavigation.onNavigateRoutines).takeIf { planner },
            ),
        ),
    )
}

/** The Focus tab's first page — timers, their history, and reading. */
@Composable
fun FocusHubScreen(shellNavigation: ShellNavigation) {
    val modules = rememberEnabledModules()
    val focus = AppModule.FOCUS in modules
    val reading = AppModule.READING in modules
    val sports = AppModule.SPORTS in modules
    HubPage(
        navigation = shellNavigation,
        destination = ShellDestination.FOCUS_HUB,
        title = stringResource(R.string.label_focus_group),
        groups = listOf(
            listOfNotNull(
                HubEntry(R.string.label_focus_timer, R.string.hub_focus_timer_hint, Icons.Filled.Timer, shellNavigation.onNavigateFocusTimer).takeIf { focus },
                HubEntry(R.string.label_flow_timer, R.string.hub_flow_timer_hint, Icons.Filled.AvTimer, shellNavigation.onNavigateFlowTimer).takeIf { focus },
                HubEntry(R.string.focustimer_history_page_title, R.string.hub_study_history_hint, Icons.Filled.History, shellNavigation.onNavigateStudyHistory).takeIf { focus },
            ),
            listOfNotNull(
                HubEntry(R.string.label_reading, R.string.hub_reading_hint, Icons.Filled.AutoStories, shellNavigation.onNavigateReading).takeIf { reading },
                HubEntry(R.string.label_sports, R.string.hub_sports_hint, Icons.Filled.FitnessCenter, shellNavigation.onNavigateSports).takeIf { sports },
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
        supportingContent = { Text(stringResource(entry.hintRes)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        // Transparent so the GroupedCard's tint shows through (ListItem is opaque by default).
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = entry.onClick),
    )
}
