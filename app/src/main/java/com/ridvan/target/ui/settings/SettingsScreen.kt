package com.ridvan.target.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun SettingsScreen(
    shellNavigation: ShellNavigation,
    viewModel: SettingsViewModel = viewModel(),
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    AppShell(navigation = shellNavigation, title = "Settings") { innerPadding ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark mode", modifier = Modifier.weight(1f).padding(end = 8.dp))
            Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
        }
    }
}
