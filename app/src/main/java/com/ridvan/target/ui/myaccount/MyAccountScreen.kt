package com.ridvan.target.ui.myaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun MyAccountScreen(shellNavigation: ShellNavigation) {
    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.MY_ACCOUNT,
        title = stringResource(R.string.label_my_account),
    ) { innerPadding ->
        GroupedCard(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            AccountMenuRow(
                label = stringResource(R.string.label_profile),
                icon = Icons.Filled.Person,
                onClick = shellNavigation.onNavigateProfile,
            )
            HorizontalDivider()
            AccountMenuRow(
                label = stringResource(R.string.menu_log_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = shellNavigation.onLogOut,
            )
            HorizontalDivider()
            AccountMenuRow(
                label = stringResource(R.string.label_switch_account),
                icon = Icons.Filled.SwitchAccount,
                onClick = shellNavigation.onSwitchAccount,
            )
        }
    }
}

@Composable
private fun AccountMenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
