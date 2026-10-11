package com.ridvan.target.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A picker that looks tappable: a rounded tinted box with the current choice and a ▾ that flips while the
 * menu is open, with an optional small [label] above it. The menu is as wide as the box. First built for
 * Focus Timer's preset/link pickers, then shared so every filter and picker in the app reads the same.
 */
@Composable
fun DropdownField(
    selectedLabel: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    items: @Composable (closeMenu: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "dropdownArrow")
    var boxWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp).alpha(if (enabled) 1f else 0.5f)) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { boxWidthPx = it.width }) {
            Surface(
                onClick = { expanded = true },
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Text(
                        selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(arrowRotation),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(density) { boxWidthPx.toDp() }),
            ) {
                items { expanded = false }
            }
        }
    }
}
