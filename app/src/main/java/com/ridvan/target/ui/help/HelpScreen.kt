package com.ridvan.target.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun HelpScreen(shellNavigation: ShellNavigation) {
    AppShell(navigation = shellNavigation, title = stringResource(R.string.label_help)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            HelpSection(R.string.help_getting_started_title, R.string.help_getting_started_body)
            HelpSection(R.string.help_exams_title, R.string.help_exams_body)
            HelpSection(R.string.help_courses_title, R.string.help_courses_body)
            HelpSection(R.string.help_study_resources_title, R.string.help_study_resources_body)
            HelpSection(R.string.help_topics_title, R.string.help_topics_body)
            HelpSection(R.string.help_languages_title, R.string.help_languages_body)
            HelpSection(R.string.help_shortcuts_title, R.string.help_shortcuts_body)
            HelpSection(R.string.help_backup_title, R.string.help_backup_body)
            HelpSection(R.string.help_settings_title, R.string.help_settings_body, isLast = true)
        }
    }
}

@Composable
private fun HelpSection(titleRes: Int, bodyRes: Int, isLast: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 20.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
