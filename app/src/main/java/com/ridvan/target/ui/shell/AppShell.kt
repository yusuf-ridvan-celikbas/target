package com.ridvan.target.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import kotlinx.coroutines.launch

data class ShellNavigation(
    val onNavigateHome: () -> Unit,
    val onNavigateExams: () -> Unit,
    val onNavigateCourses: () -> Unit,
    val onNavigateStudyResources: () -> Unit,
    val onNavigateTopics: () -> Unit,
    val onNavigateStatistics: () -> Unit,
    val onNavigateLanguages: () -> Unit,
    val onNavigatePlanner: () -> Unit,
    val onNavigateFocusTimer: () -> Unit,
    val onNavigateHelp: () -> Unit,
    val onNavigateMyAccount: () -> Unit,
    val onNavigateSettings: () -> Unit,
    val onNavigateProfile: () -> Unit,
    val onNavigateAppSettings: () -> Unit,
    val onLogOut: () -> Unit,
    val onSwitchAccount: () -> Unit,
)

enum class ShellDestination {
    HOME, EXAMS, COURSES, STUDY_RESOURCES, TOPICS, STATISTICS, LANGUAGES, PLANNER, FOCUS_TIMER, HELP, MY_ACCOUNT, SETTINGS, OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navigation: ShellNavigation,
    currentDestination: ShellDestination = ShellDestination.OTHER,
    title: String = "",
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }
    var studyExpanded by remember { mutableStateOf(true) }
    val bannerColor by (LocalContext.current.applicationContext as TargetApplication).preferences.bannerColor
        .collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateHome()
                        },
                ) {
                    Image(
                        painter = painterResource(bannerColor.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val studyLabel = stringResource(R.string.label_study_group)
                DrawerGroupHeader(
                    label = studyLabel,
                    icon = Icons.Filled.School,
                    expanded = studyExpanded,
                    onToggleExpand = { studyExpanded = !studyExpanded },
                )
                if (studyExpanded) {
                    DrawerItem(
                        label = stringResource(R.string.label_exams),
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        selected = currentDestination == ShellDestination.EXAMS,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateExams()
                        },
                    )
                    DrawerItem(
                        label = stringResource(R.string.label_courses),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        selected = currentDestination == ShellDestination.COURSES,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateCourses()
                        },
                    )
                    DrawerItem(
                        label = stringResource(R.string.label_study_resources),
                        icon = Icons.Filled.Bookmark,
                        selected = currentDestination == ShellDestination.STUDY_RESOURCES,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateStudyResources()
                        },
                    )
                    DrawerItem(
                        label = stringResource(R.string.label_topics),
                        icon = Icons.Filled.Topic,
                        selected = currentDestination == ShellDestination.TOPICS,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateTopics()
                        },
                    )
                    DrawerItem(
                        label = stringResource(R.string.label_statistics),
                        icon = Icons.Filled.BarChart,
                        selected = currentDestination == ShellDestination.STATISTICS,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateStatistics()
                        },
                    )
                    DrawerItem(
                        label = stringResource(R.string.label_languages),
                        icon = Icons.Filled.Translate,
                        selected = currentDestination == ShellDestination.LANGUAGES,
                        indented = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigation.onNavigateLanguages()
                        },
                    )
                }
                DrawerItem(
                    label = stringResource(R.string.label_planner),
                    icon = Icons.Filled.CalendarMonth,
                    selected = currentDestination == ShellDestination.PLANNER,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigatePlanner()
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.label_focus_timer),
                    icon = Icons.Filled.Timer,
                    selected = currentDestination == ShellDestination.FOCUS_TIMER,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateFocusTimer()
                    },
                )
                Spacer(Modifier.weight(1f))
                DrawerItem(
                    label = stringResource(R.string.label_my_account),
                    icon = Icons.Filled.AccountCircle,
                    selected = currentDestination == ShellDestination.MY_ACCOUNT,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateMyAccount()
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.settings_title),
                    icon = Icons.Filled.Settings,
                    selected = currentDestination == ShellDestination.SETTINGS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateSettings()
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.label_help),
                    icon = Icons.AutoMirrored.Filled.Help,
                    selected = currentDestination == ShellDestination.HELP,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateHelp()
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.label_home),
                    icon = Icons.Filled.Home,
                    selected = currentDestination == ShellDestination.HOME,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateHome()
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                        }
                    },
                    actions = {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_my_account)) },
                                leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    navigation.onNavigateMyAccount()
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    navigation.onNavigateSettings()
                                },
                            )
                        }
                    },
                )
            },
            floatingActionButton = floatingActionButton,
        ) { innerPadding -> content(innerPadding) }
    }
}

/**
 * A compact drawer row — replaces the stock NavigationDrawerItem, whose fixed
 * ~56dp height and full-bleed selected-state pill read as oversized once the
 * drawer had 8 destinations (reported directly: "the rounded highlight is too
 * big, maybe it can contain the text size"). This hugs the icon/label with
 * tighter padding and a smaller shape/text size instead.
 */
@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    indented: Boolean = false,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "drawerItemContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "drawerItemContentColor",
    )
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 24.dp else 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * A collapsible drawer group header — introduced to group the exam-prep
 * destinations (Exams/Courses/Study Resources/Topics/Statistics/Languages)
 * under one "Study" label, by explicit request. Not persisted across drawer
 * closes (defaults back to expanded), the same "collapses again on next
 * visit" precedent already established for other collapsible sections in
 * the app — except this one defaults open since it holds the drawer's
 * primary navigation destinations.
 */
@Composable
private fun DrawerGroupHeader(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(onClick = onToggleExpand),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    stringResource(R.string.cd_collapse_x, label)
                } else {
                    stringResource(R.string.cd_expand_x, label)
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
