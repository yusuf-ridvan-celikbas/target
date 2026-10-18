package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.Exam
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

/** Quantitative/Verbal nets (by course category), the three weighted nets and, if set, an estimated score. */
@Composable
fun AlesScoreCard(exam: Exam, parts: List<MockExamPartWithCourse>) {
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
            val weighted = alesWeightedNet(type, quantitativeNet, verbalNet)
            val conversion = exam.alesConversion(type)
            ListItem(
                headlineContent = { Text(alesTypeLabel(type)) },
                supportingContent = { Text(stringResource(R.string.mockexam_ales_weighted, formatNet(weighted))) },
                trailingContent = conversion?.let { (base, coef) ->
                    {
                        Text(
                            stringResource(R.string.mockexam_ales_estimate, formatNet(alesEstimate(base, coef, weighted))),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
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
            Text(
                stringResource(
                    if (AlesScoreType.entries.any { exam.alesConversion(it) != null }) R.string.mockexam_ales_estimate_note
                    else R.string.mockexam_ales_no_conversion
                ),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
fun AlesSettingsDialog(
    exam: Exam,
    onSave: (enabled: Boolean, coefficients: Map<AlesScoreType, Pair<Double?, Double?>>) -> Unit,
    onDismiss: () -> Unit,
) {
    fun text(value: Double?) = value?.toString().orEmpty()
    var enabled by remember { mutableStateOf(exam.alesScoring) }
    val fields = remember {
        mapOf(
            AlesScoreType.QUANTITATIVE to (mutableStateOf(text(exam.alesSayBase)) to mutableStateOf(text(exam.alesSayCoef))),
            AlesScoreType.VERBAL to (mutableStateOf(text(exam.alesSozBase)) to mutableStateOf(text(exam.alesSozCoef))),
            AlesScoreType.EQUAL_WEIGHT to (mutableStateOf(text(exam.alesEaBase)) to mutableStateOf(text(exam.alesEaCoef))),
        )
    }
    val valid = fields.values.all { (base, coef) ->
        listOf(base.value, coef.value).all { it.isBlank() || parseDecimal(it) != null }
    }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_ales_settings)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mockexam_ales_enable), modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Text(stringResource(R.string.mockexam_ales_enable_hint), style = MaterialTheme.typography.labelSmall)
                if (enabled) {
                    Text(
                        stringResource(R.string.mockexam_ales_conversion_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    fields.forEach { (type, pair) ->
                        val (base, coef) = pair
                        Text(alesTypeLabel(type), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                        Row {
                            DecimalField(base.value, { base.value = it }, stringResource(R.string.mockexam_ales_base), Modifier.weight(1f).padding(end = 4.dp))
                            DecimalField(coef.value, { coef.value = it }, stringResource(R.string.mockexam_ales_coef), Modifier.weight(1f).padding(start = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(enabled, fields.mapValues { (_, p) -> parseDecimal(p.first.value) to parseDecimal(p.second.value) }) },
                enabled = valid,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun DecimalField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 10 && input.all { it.isDigit() || it in ".,-" }) onValueChange(input) },
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotBlank() && parseDecimal(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
