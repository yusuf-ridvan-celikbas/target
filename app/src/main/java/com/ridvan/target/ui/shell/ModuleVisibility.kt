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
    ShellTab.STUDY -> AppModule.EXAMS_STUDY in modules
    ShellTab.PLAN -> AppModule.PLANNER_ROUTINES in modules || AppModule.TRIPS in modules
    ShellTab.FOCUS -> AppModule.FOCUS in modules || AppModule.READING in modules
    ShellTab.STATS -> AppModule.EXAMS_STUDY in modules || AppModule.READING in modules
}

fun visibleTabs(modules: Set<AppModule>): List<ShellTab> = ShellTab.entries.filter { it.isVisible(modules) }

/** Statistics sources an account can see; never empty for a Stats tab that is visible. */
fun availableStatsSources(modules: Set<AppModule>): List<StatsSource> = buildList {
    if (AppModule.EXAMS_STUDY in modules) {
        add(StatsSource.PRACTICE_SESSIONS)
        add(StatsSource.PRACTICE_EXAMS)
    }
    if (AppModule.READING in modules) add(StatsSource.READING)
}

/** Planner agenda groups that belong to a module. */
enum class AgendaGroup { EXAMS, PRACTICE, FOCUS, READING, TRIPS, ROUTINES }

fun agendaGroupVisible(group: AgendaGroup, modules: Set<AppModule>): Boolean = when (group) {
    AgendaGroup.EXAMS, AgendaGroup.PRACTICE -> AppModule.EXAMS_STUDY in modules
    AgendaGroup.FOCUS -> AppModule.FOCUS in modules
    AgendaGroup.READING -> AppModule.READING in modules
    AgendaGroup.TRIPS -> AppModule.TRIPS in modules
    AgendaGroup.ROUTINES -> AppModule.PLANNER_ROUTINES in modules
}

@Composable
fun rememberEnabledModules(): Set<AppModule> {
    val app = LocalContext.current.applicationContext as TargetApplication
    return app.modules.enabled.collectAsState().value
}
