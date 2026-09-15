package com.ridvan.target.ui.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.AppLanguage
import com.ridvan.target.data.local.BannerColor
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
    val activity = LocalContext.current.findActivity()

    AppShell(navigation = shellNavigation, title = stringResource(R.string.settings_title)) { innerPadding ->
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
        }
    }
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
