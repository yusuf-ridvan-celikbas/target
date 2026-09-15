package com.ridvan.target.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class SegmentedToggleOption<T>(
    val value: T,
    val label: String,
    val dimmed: Boolean = false,
)

/**
 * The pill-row segmented-toggle look used throughout Settings/Preferred Name/Course
 * Category/Statistics — one shared implementation instead of a private copy per screen.
 */
@Composable
fun <T> SegmentedToggle(
    options: List<SegmentedToggleOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    segmentContentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    Row(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedToggleItem(
                text = option.label,
                selected = option.value == selected,
                dimmed = option.dimmed,
                onClick = { onSelect(option.value) },
                textStyle = textStyle,
                maxLines = maxLines,
                overflow = overflow,
                contentPadding = segmentContentPadding,
                modifier = if (index == 0) Modifier.weight(1f) else Modifier.weight(1f).padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun SegmentedToggleItem(
    text: String,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    textStyle: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            dimmed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "segmentedToggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            dimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "segmentedToggleContentColor",
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
            style = textStyle,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
        )
    }
}
