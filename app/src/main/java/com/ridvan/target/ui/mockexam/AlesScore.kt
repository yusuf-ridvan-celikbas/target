package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.courseDisplayName

@Composable
private fun alesTypeLabel(type: AlesScoreType): String = stringResource(
    when (type) {
        AlesScoreType.QUANTITATIVE -> R.string.mockexam_ales_type_quantitative
        AlesScoreType.VERBAL -> R.string.mockexam_ales_type_verbal
        AlesScoreType.EQUAL_WEIGHT -> R.string.mockexam_ales_type_equal
    }
)

/** Quantitative/Verbal nets (by course category) and the SAY/SÖZ/EA scores. */
@Composable
fun AlesScoreCard(parts: List<MockExamPartWithCourse>) {
    fun netOf(category: CourseCategory) = parts.filter { it.courseCategory == category }
        .sumOf { mockExamNet(it.part.correctCount, it.part.wrongCount) }
    val quantitativeNet = netOf(CourseCategory.QUANTITATIVE)
    val verbalNet = netOf(CourseCategory.VERBAL)
    val uncategorized = parts.filter { it.courseCategory == null }

    GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Text(stringResource(R.string.mockexam_ales_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mockexam_ales_nets, formatNet(quantitativeNet), formatNet(verbalNet)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AlesScoreType.entries.forEach { type ->
            ListItem(
                headlineContent = { Text(alesTypeLabel(type)) },
                trailingContent = {
                    Text(
                        formatNet(alesScore(type, quantitativeNet, verbalNet)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            if (uncategorized.isNotEmpty()) {
                val names = uncategorized.map { courseDisplayName(it.courseName) }
                Text(
                    stringResource(R.string.mockexam_ales_uncategorized, names.joinToString()),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(stringResource(R.string.mockexam_ales_term_note), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AlesSettingsDialog(
    enabled: Boolean,
    onSave: (enabled: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by remember { mutableStateOf(enabled) }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_ales_settings)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mockexam_ales_enable), modifier = Modifier.weight(1f))
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
                Text(stringResource(R.string.mockexam_ales_enable_hint), style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(checked) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
