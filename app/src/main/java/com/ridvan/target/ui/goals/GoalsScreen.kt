package com.ridvan.target.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.HabitPeriod
import com.ridvan.target.data.local.entity.GoalSource
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.formatRate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.money.DayField
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun GoalsScreen(
    shellNavigation: ShellNavigation,
    onGoalClick: (Long) -> Unit,
    viewModel: GoalsViewModel = viewModel(),
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val (done, open) = goals.partition { it.goal.completedAt != null }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.GOALS,
        title = stringResource(R.string.label_goals),
        floatingActionButton = { AddFab(onClick = { adding = true }) },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            if (goals.isEmpty()) {
                Text(stringResource(R.string.goals_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            open.forEach { item -> GoalCard(item) { onGoalClick(item.goal.id) } }
            if (done.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showDone = !showDone }.padding(vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.goal_done_section, done.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Icon(if (showDone) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                if (showDone) done.forEach { item -> GoalCard(item) { onGoalClick(item.goal.id) } }
            }
        }
    }

    if (adding) {
        GoalFormDialog(goal = null, onSave = { viewModel.add(it); adding = false }, onDismiss = { adding = false })
    }
}

/** A goal's name, progress bar and lines; also used on Home. */
@Composable
fun GoalCard(item: GoalItem, onClick: () -> Unit) {
    GroupedCard(modifier = Modifier.clickable(onClick = onClick)) {
        GoalSummary(item, Modifier.padding(16.dp))
    }
}

@Composable
fun GoalSummary(item: GoalItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.goal.kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(item.goal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (item.habitStreak > 0) {
                Text(stringResource(R.string.streak_short, item.habitStreak), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        LinearProgressIndicator(progress = { item.progress.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text(goalProgressLine(item), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        goalPaceLine(item)?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.pace?.onTrack == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

@Composable
private fun NumberField(text: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = text.isNotBlank() && parseNumber(text) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** New or edited goal; the kind is fixed once created. */
@Composable
fun GoalFormDialog(goal: Goal?, onSave: (GoalForm) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(goal?.name.orEmpty()) }
    var kind by remember { mutableStateOf(goal?.kind ?: GoalKind.TARGET) }
    var source by remember { mutableStateOf(goal?.source) }
    var unit by remember { mutableStateOf(goal?.unit.orEmpty()) }
    var target by remember { mutableStateOf(goal?.targetValue?.let { formatRate(it) }.orEmpty()) }
    var start by remember { mutableStateOf(goal?.startValue?.let { formatRate(it) }.orEmpty()) }
    var period by remember { mutableStateOf(goal?.period ?: HabitPeriod.WEEK) }
    var startDate by remember { mutableStateOf(goal?.startDate ?: startOfTodayMillis()) }
    var deadline by remember { mutableStateOf(goal?.deadline) }
    var notes by remember { mutableStateOf(goal?.notes.orEmpty()) }
    var steps by remember { mutableStateOf("") }

    val targetValue = parseNumber(target)
    val startValue = parseNumber(start)
    val valid = name.isNotBlank() && when (kind) {
        GoalKind.TARGET -> targetValue != null && targetValue > 0
        GoalKind.HABIT -> targetValue != null && targetValue >= 1
        GoalKind.MEASURE -> targetValue != null && startValue != null && targetValue != startValue
        GoalKind.MILESTONES -> goal != null || steps.lines().any { it.isNotBlank() }
    }

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (goal == null) R.string.goal_new else R.string.goal_edit)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.common_name)) }, modifier = Modifier.fillMaxWidth(),
                )
                if (goal == null) {
                    SegmentedToggle(
                        options = GoalKind.entries.map { SegmentedToggleOption(it, stringResource(it.labelRes())) },
                        selected = kind,
                        onSelect = { kind = it },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                when (kind) {
                    GoalKind.TARGET -> {
                        DropdownField(
                            selectedLabel = stringResource(source?.labelRes() ?: R.string.goal_source_manual),
                            label = stringResource(R.string.goal_field_source),
                            modifier = Modifier.padding(top = 8.dp),
                        ) { close ->
                            DropdownMenuItem(text = { Text(stringResource(R.string.goal_source_manual)) }, onClick = { source = null; close() })
                            GoalSource.entries.forEach { s ->
                                DropdownMenuItem(text = { Text(stringResource(s.labelRes())) }, onClick = { source = s; close() })
                            }
                        }
                        NumberField(target, { target = it }, stringResource(R.string.goal_field_target))
                        if (source == null) {
                            OutlinedTextField(
                                value = unit, onValueChange = { unit = it }, singleLine = true,
                                label = { Text(stringResource(R.string.goal_field_unit)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                    }
                    GoalKind.HABIT -> {
                        NumberField(target, { target = it }, stringResource(R.string.goal_field_times))
                        SegmentedToggle(
                            options = listOf(
                                SegmentedToggleOption(HabitPeriod.DAY, stringResource(R.string.goal_period_day)),
                                SegmentedToggleOption(HabitPeriod.WEEK, stringResource(R.string.goal_period_week)),
                            ),
                            selected = period,
                            onSelect = { period = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    GoalKind.MEASURE -> {
                        NumberField(start, { start = it }, stringResource(R.string.goal_field_start_value))
                        NumberField(target, { target = it }, stringResource(R.string.goal_field_target_value))
                        OutlinedTextField(
                            value = unit, onValueChange = { unit = it }, singleLine = true,
                            label = { Text(stringResource(R.string.goal_field_unit)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    GoalKind.MILESTONES -> if (goal == null) {
                        OutlinedTextField(
                            value = steps, onValueChange = { steps = it }, minLines = 3,
                            label = { Text(stringResource(R.string.goal_field_steps)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
                DayField(stringResource(R.string.goal_field_start), startDate, { it?.let { d -> startDate = d } })
                if (kind != GoalKind.HABIT) {
                    DayField(stringResource(R.string.goal_field_deadline), deadline, { deadline = it }, allowClear = true)
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.goal_field_notes)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    GoalForm(
                        name = name.trim(),
                        kind = kind,
                        unit = unit.trim().takeIf { it.isNotEmpty() && (kind == GoalKind.MEASURE || (kind == GoalKind.TARGET && source == null)) },
                        targetValue = targetValue.takeIf { kind != GoalKind.MILESTONES },
                        startValue = startValue.takeIf { kind == GoalKind.MEASURE },
                        period = period.takeIf { kind == GoalKind.HABIT },
                        source = source.takeIf { kind == GoalKind.TARGET },
                        startDate = startDate,
                        deadline = deadline.takeIf { kind != GoalKind.HABIT },
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                        milestones = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
                    )
                )
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
