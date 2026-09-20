package com.ridvan.target.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.AppLanguage
import com.ridvan.target.data.local.BannerColor
import com.ridvan.target.data.local.NotificationLeadTime
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.HelpTooltip
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.findActivity
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun SettingsScreen(
    shellNavigation: ShellNavigation,
    viewModel: SettingsViewModel = viewModel(),
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val useBlueAppIcon by viewModel.useBlueAppIcon.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val bannerColor by viewModel.bannerColor.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationLeadTime by viewModel.notificationLeadTime.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    var exactAlarmGranted by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    LifecycleResumeEffect(Unit) {
        exactAlarmGranted = canScheduleExactAlarms(context)
        onPauseOrDispose { }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setNotificationsEnabled(granted) }

    AppShell(navigation = shellNavigation, title = stringResource(R.string.menu_app_settings)) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupedCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_dark_mode), modifier = Modifier.weight(1f).padding(end = 8.dp))
                    Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                }
            }
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.settings_app_icon_preference))
                    SegmentedToggle(
                        options = listOf(
                            SegmentedToggleOption(false, stringResource(R.string.settings_icon_white)),
                            SegmentedToggleOption(true, stringResource(R.string.settings_icon_blue)),
                        ),
                        selected = useBlueAppIcon,
                        onSelect = { viewModel.setUseBlueAppIcon(it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_app_color_preference))
                        HelpTooltip(R.string.help_tooltip_app_color_preference, R.string.cd_help_app_color_preference)
                    }
                    BannerColor.entries.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { colorOption ->
                                ColorSwatch(
                                    color = colorOption.color,
                                    selected = bannerColor == colorOption,
                                    contentDescription = stringResource(colorOption.labelRes),
                                    onClick = { viewModel.setBannerColor(colorOption) },
                                )
                            }
                        }
                    }
                }
            }
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.settings_language_preference))
                    SegmentedToggle(
                        options = listOf(
                            SegmentedToggleOption(AppLanguage.ENGLISH, stringResource(R.string.settings_language_english)),
                            SegmentedToggleOption(AppLanguage.TURKISH, stringResource(R.string.settings_language_turkish)),
                        ),
                        selected = appLanguage,
                        onSelect = {
                            viewModel.setAppLanguage(it)
                            activity?.recreate()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_notifications), modifier = Modifier.weight(1f))
                        HelpTooltip(R.string.help_tooltip_notifications, R.string.cd_help_notifications)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                        PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            },
                        )
                    }
                    if (notificationsEnabled) {
                        Text(
                            stringResource(R.string.settings_notification_lead_time),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        SegmentedToggle(
                            options = listOf(
                                SegmentedToggleOption(NotificationLeadTime.AT_TIME, stringResource(R.string.notification_lead_at_time)),
                                SegmentedToggleOption(NotificationLeadTime.MIN_15, stringResource(R.string.notification_lead_15)),
                                SegmentedToggleOption(NotificationLeadTime.MIN_30, stringResource(R.string.notification_lead_30)),
                                SegmentedToggleOption(NotificationLeadTime.HOUR_1, stringResource(R.string.notification_lead_60)),
                            ),
                            selected = notificationLeadTime,
                            onSelect = { viewModel.setNotificationLeadTime(it) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        Text(
                            stringResource(R.string.settings_notifications_all_day_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (!exactAlarmGranted) {
                            Text(
                                stringResource(R.string.settings_notifications_grant_exact_alarm),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                                modifier = Modifier.padding(top = 8.dp).clickable {
                                    context.startActivity(
                                        Intent(
                                            AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun canScheduleExactAlarms(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return true
    return alarmManager.canScheduleExactAlarms()
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Surface(
        color = color,
        shape = CircleShape,
        modifier = Modifier
            .size(48.dp)
            .border(width = if (selected) 3.dp else 1.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (selected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (color.luminance() >= 0.5f) Color.Black else Color.White,
                )
            }
        }
    }
}
