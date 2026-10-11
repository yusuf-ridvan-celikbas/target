package com.ridvan.target.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ridvan.target.R

data class ShellNavigation(
    val onNavigateHome: () -> Unit,
    val onNavigateStudyHub: () -> Unit,
    val onNavigateFocusHub: () -> Unit,
    val onNavigateExams: () -> Unit,
    val onNavigateCourses: () -> Unit,
    val onNavigateStudyResources: () -> Unit,
    val onNavigateTopics: () -> Unit,
    val onNavigateStatistics: () -> Unit,
    val onNavigateLanguages: () -> Unit,
    val onNavigatePlanner: () -> Unit,
    val onNavigateFocusTimer: () -> Unit,
    val onNavigateFlowTimer: () -> Unit,
    val onNavigateStudyHistory: () -> Unit,
    val onNavigateReading: () -> Unit,
    val onNavigateHelp: () -> Unit,
    val onNavigateMyAccount: () -> Unit,
    val onNavigateSettings: () -> Unit,
    val onNavigateProfile: () -> Unit,
    val onLogOut: () -> Unit,
    val onSwitchAccount: () -> Unit,
    /** Tapping the tab you're already in: back to that tab's own first page. */
    val onReselectTab: (ShellTab) -> Unit,
    val onBack: () -> Unit,
)

enum class ShellDestination {
    HOME, STUDY_HUB, FOCUS_HUB, EXAMS, COURSES, STUDY_RESOURCES, TOPICS, STATISTICS, LANGUAGES, PLANNER,
    FOCUS_TIMER, FLOW_TIMER, STUDY_HISTORY, READING, HELP, MY_ACCOUNT, SETTINGS, OTHER
}

/**
 * The five bottom-bar tabs. Each owns a set of pages: a tab is highlighted while any of them is
 * open, and only its [root] page goes without a back arrow.
 */
enum class ShellTab(val root: ShellDestination, val pages: Set<ShellDestination>) {
    HOME(ShellDestination.HOME, emptySet()),
    STUDY(
        ShellDestination.STUDY_HUB,
        setOf(
            ShellDestination.EXAMS, ShellDestination.COURSES, ShellDestination.STUDY_RESOURCES,
            ShellDestination.TOPICS, ShellDestination.LANGUAGES,
        ),
    ),
    PLAN(ShellDestination.PLANNER, emptySet()),
    FOCUS(
        ShellDestination.FOCUS_HUB,
        setOf(
            ShellDestination.FOCUS_TIMER, ShellDestination.FLOW_TIMER, ShellDestination.STUDY_HISTORY,
            ShellDestination.READING,
        ),
    ),
    STATS(ShellDestination.STATISTICS, emptySet()),
    ;

    fun owns(destination: ShellDestination) = destination == root || destination in pages
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navigation: ShellNavigation,
    currentDestination: ShellDestination = ShellDestination.OTHER,
    title: String = "",
    floatingActionButton: @Composable () -> Unit = {},
    /** Screen-specific top-bar icons, placed before the profile menu. */
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    val currentTab = ShellTab.entries.firstOrNull { it.owns(currentDestination) }
    val isTabRoot = ShellTab.entries.any { it.root == currentDestination }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (!isTabRoot) {
                        IconButton(onClick = navigation.onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                actions = {
                    actions()
                    IconButton(onClick = { profileMenuExpanded = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = stringResource(R.string.cd_profile_menu))
                    }
                    DropdownMenu(expanded = profileMenuExpanded, onDismissRequest = { profileMenuExpanded = false }) {
                        ProfileMenuItem(stringResource(R.string.label_my_account), Icons.Filled.AccountCircle) {
                            profileMenuExpanded = false
                            navigation.onNavigateMyAccount()
                        }
                        ProfileMenuItem(stringResource(R.string.settings_title), Icons.Filled.Settings) {
                            profileMenuExpanded = false
                            navigation.onNavigateSettings()
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        ProfileMenuItem(stringResource(R.string.label_help), Icons.AutoMirrored.Filled.Help) {
                            profileMenuExpanded = false
                            navigation.onNavigateHelp()
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                ShellTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val look = tab.look()
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (selected) navigation.onReselectTab(tab) else navigation.open(tab) },
                        icon = { Icon(if (selected) look.selectedIcon else look.icon, contentDescription = null) },
                        label = { Text(stringResource(look.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
        floatingActionButton = floatingActionButton,
    ) { innerPadding -> content(innerPadding) }
}

private fun ShellNavigation.open(tab: ShellTab) = when (tab) {
    ShellTab.HOME -> onNavigateHome()
    ShellTab.STUDY -> onNavigateStudyHub()
    ShellTab.PLAN -> onNavigatePlanner()
    ShellTab.FOCUS -> onNavigateFocusHub()
    ShellTab.STATS -> onNavigateStatistics()
}

private class TabLook(val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private fun ShellTab.look() = when (this) {
    ShellTab.HOME -> TabLook(R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home)
    ShellTab.STUDY -> TabLook(R.string.label_study_group, Icons.Outlined.School, Icons.Filled.School)
    ShellTab.PLAN -> TabLook(R.string.tab_plan, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    ShellTab.FOCUS -> TabLook(R.string.label_focus_group, Icons.Outlined.SelfImprovement, Icons.Filled.SelfImprovement)
    ShellTab.STATS -> TabLook(R.string.tab_stats, Icons.Outlined.BarChart, Icons.Filled.BarChart)
}

@Composable
private fun ProfileMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
