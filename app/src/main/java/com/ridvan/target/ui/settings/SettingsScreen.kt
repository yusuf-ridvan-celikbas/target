package com.ridvan.target.ui.settings

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.AppLanguage
import com.ridvan.target.data.local.BannerColor
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
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_dark_mode), modifier = Modifier.weight(1f).padding(end = 8.dp))
                Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
            }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.settings_app_icon_preference))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SettingsToggleSegment(
                        text = stringResource(R.string.settings_icon_white),
                        selected = !useBlueAppIcon,
                        onClick = { viewModel.setUseBlueAppIcon(false) },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsToggleSegment(
                        text = stringResource(R.string.settings_icon_blue),
                        selected = useBlueAppIcon,
                        onClick = { viewModel.setUseBlueAppIcon(true) },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.settings_app_color_preference))
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
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.settings_language_preference))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SettingsToggleSegment(
                        text = stringResource(R.string.settings_language_english),
                        selected = appLanguage == AppLanguage.ENGLISH,
                        onClick = {
                            viewModel.setAppLanguage(AppLanguage.ENGLISH)
                            activity?.recreate()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsToggleSegment(
                        text = stringResource(R.string.settings_language_turkish),
                        selected = appLanguage == AppLanguage.TURKISH,
                        onClick = {
                            viewModel.setAppLanguage(AppLanguage.TURKISH)
                            activity?.recreate()
                        },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "settingsToggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "settingsToggleContentColor",
    )
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
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
