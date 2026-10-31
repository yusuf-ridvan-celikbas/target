package com.ridvan.target.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.statistics.StatsSource

/** Whether a bottom tab shows for an account's enabled modules. Home is always visible. */
fun ShellTab.isVisible(modules: Set<AppModule>): Boolean = when (this) {
    ShellTab.HOME -> true
    ShellTab.STUDY -> AppModule.EXAMS_STUDY in modules || AppModule.FOCUS in modules
    ShellTab.PLAN -> AppModule.PLANNER_ROUTINES in modules
    ShellTab.HEALTH -> AppModule.SPORTS in modules
    ShellTab.LIFE -> AppModule.READING in modules || AppModule.TRIPS in modules || AppModule.MONEY in modules
}

fun visibleTabs(modules: Set<AppModule>): List<ShellTab> = ShellTab.entries.filter { it.isVisible(modules) }

/** Statistics sources: the study ones (Study tab) or Reading alone (Life tab's Reading stats). */
fun availableStatsSources(modules: Set<AppModule>, readingOnly: Boolean): List<StatsSource> = when {
    readingOnly -> listOfNotNull(StatsSource.READING.takeIf { AppModule.READING in modules })
    AppModule.EXAMS_STUDY in modules -> listOf(StatsSource.PRACTICE_SESSIONS, StatsSource.PRACTICE_EXAMS, StatsSource.MOCK_EXAMS)
    else -> emptyList()
}

/** Planner agenda groups that belong to a module. */
enum class AgendaGroup { EXAMS, PRACTICE, FOCUS, READING, TRIPS, ROUTINES, SPORTS }

fun agendaGroupVisible(group: AgendaGroup, modules: Set<AppModule>): Boolean = when (group) {
    AgendaGroup.EXAMS, AgendaGroup.PRACTICE -> AppModule.EXAMS_STUDY in modules
    AgendaGroup.FOCUS -> AppModule.FOCUS in modules
    AgendaGroup.READING -> AppModule.READING in modules
    AgendaGroup.TRIPS -> AppModule.TRIPS in modules
    AgendaGroup.ROUTINES -> AppModule.PLANNER_ROUTINES in modules
    AgendaGroup.SPORTS -> AppModule.SPORTS in modules
}

@Composable
fun rememberEnabledModules(): Set<AppModule> {
    val app = LocalContext.current.applicationContext as TargetApplication
    return app.modules.enabled.collectAsState().value
}
