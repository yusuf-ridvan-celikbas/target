package com.ridvan.target.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ridvan.target.data.local.entity.PreferredNameSource

@Composable
fun PreferredNameSelector(
    source: PreferredNameSource,
    onSourceChange: (PreferredNameSource) -> Unit,
    customText: String,
    onCustomTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleActive = source == PreferredNameSource.FIRST || source == PreferredNameSource.MIDDLE

    Column(modifier = modifier) {
        Text("Preferred name", style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            ToggleSegment(
                text = "First",
                selected = source == PreferredNameSource.FIRST,
                dimmed = !toggleActive,
                onClick = { onSourceChange(PreferredNameSource.FIRST) },
                modifier = Modifier.weight(1f),
            )
            ToggleSegment(
                text = "Middle",
                selected = source == PreferredNameSource.MIDDLE,
                dimmed = !toggleActive,
                onClick = { onSourceChange(PreferredNameSource.MIDDLE) },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        }
        CheckboxOption(
            label = "Use Last Name",
            checked = source == PreferredNameSource.LAST,
            onCheck = { onSourceChange(PreferredNameSource.LAST) },
        )
        CheckboxOption(
            label = "Use Username",
            checked = source == PreferredNameSource.USERNAME,
            onCheck = { onSourceChange(PreferredNameSource.USERNAME) },
        )
        CheckboxOption(
            label = "Other",
            checked = source == PreferredNameSource.OTHER,
            onCheck = { onSourceChange(PreferredNameSource.OTHER) },
        )
        if (source == PreferredNameSource.OTHER) {
            OutlinedTextField(
                value = customText,
                onValueChange = onCustomTextChange,
                label = { Text("Preferred name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ToggleSegment(
    text: String,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            dimmed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "toggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            dimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "toggleContentColor",
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
private fun CheckboxOption(label: String, checked: Boolean, onCheck: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCheck),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheck() })
        Text(label)
    }
}
