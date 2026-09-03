package com.ridvan.target.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import kotlinx.coroutines.launch

data class ShellNavigation(
    val onNavigateHome: () -> Unit,
    val onNavigateExams: () -> Unit,
    val onNavigateCourses: () -> Unit,
    val onNavigateLanguages: () -> Unit,
    val onNavigateUser: () -> Unit,
    val onNavigateSettings: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navigation: ShellNavigation,
    title: String = "",
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var overflowExpanded by remember { mutableStateOf(false) }

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
                        painter = painterResource(R.drawable.drawer_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                NavigationDrawerItem(
                    label = { Text("Exams") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateExams()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                NavigationDrawerItem(
                    label = { Text("Courses") },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateCourses()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                NavigationDrawerItem(
                    label = { Text("Languages") },
                    icon = { Icon(Icons.Filled.Translate, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateLanguages()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Home") },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigation.onNavigateHome()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("User Settings") },
                                onClick = {
                                    overflowExpanded = false
                                    navigation.onNavigateUser()
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("App Settings") },
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
