package com.ridvan.target.ui.mockexam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.ScoringSystem
import com.ridvan.target.data.local.entity.ScoringTest
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseDisplayName

/** A part's display name: its course (translated if it's a known one) or its own free-text name. */
@Composable
fun partLabel(part: MockExamPartWithCourse): String =
    part.courseName?.let { courseDisplayName(it) } ?: part.part.name.orEmpty()

@Composable
private fun systemLabel(system: ScoringSystem): String = stringResource(
    when (system) {
        ScoringSystem.ALES -> R.string.scoring_system_ales
        ScoringSystem.TYT -> R.string.scoring_system_tyt
        ScoringSystem.AYT -> R.string.scoring_system_ayt
    }
)

@Composable
private fun testLabel(test: ScoringTest): String = stringResource(
    when (test) {
        ScoringTest.ALES_QUANTITATIVE -> R.string.scoring_test_ales_quantitative
        ScoringTest.ALES_VERBAL -> R.string.scoring_test_ales_verbal
        ScoringTest.TYT_TURKISH -> R.string.scoring_test_tyt_turkish
        ScoringTest.TYT_SOCIAL -> R.string.scoring_test_tyt_social
        ScoringTest.TYT_MATH -> R.string.scoring_test_tyt_math
        ScoringTest.TYT_SCIENCE -> R.string.scoring_test_tyt_science
        ScoringTest.AYT_MATH -> R.string.scoring_test_ayt_math
        ScoringTest.AYT_PHYSICS -> R.string.scoring_test_ayt_physics
        ScoringTest.AYT_CHEMISTRY -> R.string.scoring_test_ayt_chemistry
        ScoringTest.AYT_BIOLOGY -> R.string.scoring_test_ayt_biology
        ScoringTest.AYT_LITERATURE -> R.string.scoring_test_ayt_literature
        ScoringTest.AYT_HISTORY_1 -> R.string.scoring_test_ayt_history_1
        ScoringTest.AYT_GEOGRAPHY_1 -> R.string.scoring_test_ayt_geography_1
        ScoringTest.AYT_HISTORY_2 -> R.string.scoring_test_ayt_history_2
        ScoringTest.AYT_GEOGRAPHY_2 -> R.string.scoring_test_ayt_geography_2
        ScoringTest.AYT_PHILOSOPHY -> R.string.scoring_test_ayt_philosophy
        ScoringTest.AYT_RELIGION -> R.string.scoring_test_ayt_religion
    }
)

@Composable
fun scoreTypeLabel(type: ScoreType): String = stringResource(
    when (type) {
        ScoreType.ALES_SAY, ScoreType.YKS_SAY -> R.string.score_type_say
        ScoreType.ALES_SOZ, ScoreType.YKS_SOZ -> R.string.score_type_soz
        ScoreType.ALES_EA, ScoreType.YKS_EA -> R.string.score_type_ea
        ScoreType.TYT -> R.string.scoring_system_tyt
    }
)

/**
 * Nets per scored test and the system's scores. AYT scores add the TYT part from [tytSource]
 * (the exam's latest finished TYT deneme); without one, only the nets show.
 */
@Composable
fun ScoreCard(
    system: ScoringSystem,
    parts: List<MockExamPartWithCourse>,
    tytSource: Pair<MockExam, Map<ScoringTest, Double>>?,
) {
    val nets = netsByTest(parts.map { it.part })
    val notCounted = parts.filter { it.part.scoringTest == null }.map { partLabel(it) }
    val needsTyt = system == ScoringSystem.AYT && tytSource == null
    val allNets = if (system == ScoringSystem.AYT && tytSource != null) nets + tytSource.second else nets

    GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Text(stringResource(R.string.mockexam_score_title, systemLabel(system)), style = MaterialTheme.typography.titleMedium)
            val netLine = system.tests().filter { it in nets }.map { testLabel(it) to nets.getValue(it) }
            if (netLine.isNotEmpty()) {
                Text(
                    netLine.joinToString(" · ") { (label, net) -> "$label ${formatNet(net)}" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (!needsTyt) {
            system.scoreTypes().forEach { type ->
                ListItem(
                    headlineContent = { Text(scoreTypeLabel(type)) },
                    trailingContent = {
                        Text(
                            formatNet(score(type, allNets)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            if (needsTyt) {
                Text(stringResource(R.string.mockexam_score_needs_tyt), style = MaterialTheme.typography.bodyMedium)
            } else if (tytSource != null && system == ScoringSystem.AYT) {
                Text(stringResource(R.string.mockexam_score_tyt_from, tytSource.first.name), style = MaterialTheme.typography.labelSmall)
            }
            if (notCounted.isNotEmpty()) {
                Text(stringResource(R.string.mockexam_score_not_counted, notCounted.joinToString()), style = MaterialTheme.typography.labelSmall)
            }
            Text(stringResource(R.string.mockexam_score_approximate), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Pick the score system, then what each part counts as (prefilled with a guess from course name/category). */
@Composable
fun ScoringDialog(
    current: ScoringSystem?,
    parts: List<MockExamPartWithCourse>,
    onSave: (system: ScoringSystem?, tests: Map<Long, ScoringTest?>) -> Unit,
    onDismiss: () -> Unit,
) {
    var system by remember { mutableStateOf(current) }
    // Per system, so switching back and forth keeps choices.
    val choices = remember { mutableStateMapOf<Pair<ScoringSystem, Long>, ScoringTest?>() }
    fun choiceFor(s: ScoringSystem, part: MockExamPartWithCourse): ScoringTest? {
        val key = s to part.part.id
        return if (key in choices) choices[key]
        else part.part.scoringTest?.takeIf { it.system == s }
            ?: guessScoringTest(s, part.courseName ?: part.part.name, part.courseCategory)
    }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mockexam_scoring)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SegmentedToggle(
                    options = listOf(SegmentedToggleOption<ScoringSystem?>(null, stringResource(R.string.mockexam_scoring_none))) +
                        ScoringSystem.entries.map { SegmentedToggleOption(it, systemLabel(it)) },
                    selected = system,
                    onSelect = { system = it },
                )
                system?.let { s ->
                    Text(
                        stringResource(R.string.mockexam_scoring_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    parts.forEach { part ->
                        val chosen = choiceFor(s, part)
                        DropdownField(
                            selectedLabel = chosen?.let { testLabel(it) } ?: stringResource(R.string.mockexam_scoring_not_counted),
                            label = partLabel(part),
                            modifier = Modifier.padding(top = 8.dp),
                        ) { close ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mockexam_scoring_not_counted)) },
                                onClick = { choices[s to part.part.id] = null; close() },
                            )
                            s.tests().forEach { test ->
                                DropdownMenuItem(
                                    text = { Text(testLabel(test)) },
                                    onClick = { choices[s to part.part.id] = test; close() },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = system
                onSave(s, if (s == null) emptyMap() else parts.associate { it.part.id to choiceFor(s, it) })
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
