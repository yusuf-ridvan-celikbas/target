package com.ridvan.target.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
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
        Text(stringResource(R.string.preferred_name_label), style = MaterialTheme.typography.labelSmall)
        SegmentedToggle(
            options = listOf(
                SegmentedToggleOption(PreferredNameSource.FIRST, stringResource(R.string.preferred_name_first), dimmed = !toggleActive),
                SegmentedToggleOption(PreferredNameSource.MIDDLE, stringResource(R.string.preferred_name_middle), dimmed = !toggleActive),
            ),
            selected = source,
            onSelect = onSourceChange,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        CheckboxOption(
            label = stringResource(R.string.preferred_name_use_last_name),
            checked = source == PreferredNameSource.LAST,
            onCheck = { onSourceChange(PreferredNameSource.LAST) },
        )
        CheckboxOption(
            label = stringResource(R.string.preferred_name_use_username),
            checked = source == PreferredNameSource.USERNAME,
            onCheck = { onSourceChange(PreferredNameSource.USERNAME) },
        )
        CheckboxOption(
            label = stringResource(R.string.preferred_name_other),
            checked = source == PreferredNameSource.OTHER,
            onCheck = { onSourceChange(PreferredNameSource.OTHER) },
        )
        if (source == PreferredNameSource.OTHER) {
            OutlinedTextField(
                value = customText,
                onValueChange = onCustomTextChange,
                label = { Text(stringResource(R.string.preferred_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
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
