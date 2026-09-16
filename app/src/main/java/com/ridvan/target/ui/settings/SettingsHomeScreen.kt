package com.ridvan.target.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun SettingsHomeScreen(shellNavigation: ShellNavigation) {
    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.SETTINGS,
        title = stringResource(R.string.settings_title),
    ) { innerPadding ->
        GroupedCard(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.menu_app_settings)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().clickable(onClick = shellNavigation.onNavigateAppSettings),
            )
        }
    }
}
