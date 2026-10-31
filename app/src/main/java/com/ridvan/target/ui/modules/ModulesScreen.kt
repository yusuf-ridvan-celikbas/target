package com.ridvan.target.ui.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.data.modules.toggleModule
import com.ridvan.target.ui.common.GroupedCard
import kotlinx.coroutines.launch

private class ModuleInfo(val module: AppModule, val titleRes: Int, val hintRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val moduleInfos = listOf(
    ModuleInfo(AppModule.EXAMS_STUDY, R.string.module_exams_study, R.string.module_exams_study_hint, Icons.AutoMirrored.Filled.MenuBook),
    ModuleInfo(AppModule.PLANNER_ROUTINES, R.string.module_planner_routines, R.string.module_planner_routines_hint, Icons.Filled.CalendarMonth),
    ModuleInfo(AppModule.FOCUS, R.string.module_focus, R.string.module_focus_hint, Icons.Filled.Timer),
    ModuleInfo(AppModule.READING, R.string.module_reading, R.string.module_reading_hint, Icons.Filled.AutoStories),
    ModuleInfo(AppModule.TRIPS, R.string.module_trips, R.string.module_trips_hint, Icons.Filled.Luggage),
    ModuleInfo(AppModule.SPORTS, R.string.module_sports, R.string.module_sports_hint, Icons.Filled.FitnessCenter),
    ModuleInfo(AppModule.MONEY, R.string.module_money, R.string.module_money_hint, Icons.Filled.AccountBalanceWallet),
    ModuleInfo(AppModule.GOALS, R.string.module_goals, R.string.module_goals_hint, Icons.Filled.Flag),
)

/**
 * Picks which parts of Target the signed-in account uses. [onboarding] = the screen right after
 * Register (no back arrow, "Continue"); otherwise it is Settings → Modules (back arrow, "Save").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(onboarding: Boolean, onDone: () -> Unit, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as TargetApplication
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(application.modules.enabled.value) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (onboarding) R.string.modules_onboarding_title else R.string.modules_title)) },
                navigationIcon = {
                    if (!onboarding) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                stringResource(if (onboarding) R.string.modules_onboarding_hint else R.string.modules_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            moduleInfos.forEach { info ->
                val checked = info.module in selected
                // The last ticked module can't be unticked: at least one has to stay on.
                val locked = checked && selected.size <= 1
                GroupedCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !locked) { selected = toggleModule(selected, info.module) }
                            .padding(16.dp),
                    ) {
                        Icon(info.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(stringResource(info.titleRes), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(info.hintRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = checked,
                            enabled = !locked,
                            onCheckedChange = { selected = toggleModule(selected, info.module) },
                        )
                    }
                }
            }
            Button(
                enabled = !saving && selected.isNotEmpty(),
                onClick = {
                    val userId = application.preferences.currentUserId ?: return@Button
                    saving = true
                    scope.launch {
                        application.modules.save(userId, selected)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (onboarding) R.string.action_continue else R.string.common_save))
            }
        }
    }
}
