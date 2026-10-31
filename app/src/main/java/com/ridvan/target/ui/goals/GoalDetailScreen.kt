package com.ridvan.target.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.GoalLog
import com.ridvan.target.data.local.entity.GoalMilestone
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatRate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.money.DayField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(onBack: () -> Unit, viewModel: GoalDetailViewModel = viewModel()) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val item by viewModel.item.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // null = closed; a new log is GoalLog with id 0.
    var editingLog by remember { mutableStateOf<GoalLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.goal_edit)) }, onClick = { menuOpen = false; editing = true })
                        val done = goal?.completedAt != null
                        DropdownMenuItem(
                            text = { Text(stringResource(if (done) R.string.goal_reopen else R.string.goal_mark_done)) },
                            onClick = { menuOpen = false; viewModel.setDone(!done) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; confirmDelete = true },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val g = goal
        val i = item
        if (g == null || i == null) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            GroupedCard {
                GoalSummary(i, Modifier.padding(16.dp))
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    g.completedAt?.let { Text(stringResource(R.string.goal_completed_on, formatDate(it)), color = MaterialTheme.colorScheme.primary) }
                    g.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (i.progress.reached && g.completedAt == null && g.kind != GoalKind.HABIT) {
                        Text(stringResource(R.string.goal_reached), modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = { viewModel.setDone(true) }, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.goal_mark_done)) }
                    }
                }
            }
            when (g.kind) {
                GoalKind.TARGET -> if (g.source != null) {
                    Text(
                        stringResource(R.string.goal_auto_hint, stringResource(g.source.labelRes()), formatDate(g.startDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FilledTonalButton(onClick = { editingLog = GoalLog(goalId = g.id, date = startOfTodayMillis(), amount = 0.0) }) {
                        Text(stringResource(R.string.goal_log_add))
                    }
                    LogList(logs, g) { editingLog = it }
                }
                GoalKind.HABIT -> {
                    FilledTonalButton(onClick = viewModel::doneToday) { Text(stringResource(R.string.goal_done_today)) }
                    LogList(logs, g) { editingLog = it }
                }
                GoalKind.MEASURE -> {
                    FilledTonalButton(onClick = { editingLog = GoalLog(goalId = g.id, date = startOfTodayMillis(), amount = 0.0) }) {
                        Text(stringResource(R.string.goal_reading_add))
                    }
                    if (logs.isNotEmpty()) MeasureChart(g, logs)
                    LogList(logs, g) { editingLog = it }
                }
                GoalKind.MILESTONES -> MilestoneList(milestones, viewModel)
            }
        }
    }

    val g = goal
    if (editing && g != null) {
        GoalFormDialog(goal = g, onSave = { viewModel.update(it); editing = false }, onDismiss = { editing = false })
    }
    editingLog?.let { log ->
        if (g != null) {
            LogDialog(
                goal = g,
                log = log,
                onSave = { date, amount, note -> viewModel.saveLog(log.takeIf { it.id != 0L }, date, amount, note); editingLog = null },
                onDelete = if (log.id != 0L) ({ viewModel.deleteLog(log); editingLog = null }) else null,
                onDismiss = { editingLog = null },
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun LogList(logs: List<GoalLog>, goal: Goal, onClick: (GoalLog) -> Unit) {
    if (logs.isEmpty()) return
    Text(stringResource(R.string.goal_history), style = MaterialTheme.typography.titleSmall)
    GroupedCard {
        logs.forEachIndexed { index, log ->
            if (index > 0) HorizontalDivider()
            val amount = when (goal.kind) {
                GoalKind.HABIT -> null
                GoalKind.TARGET -> "+${formatRate(log.amount)} ${goalUnit(goal)}".trim()
                else -> "${formatRate(log.amount)} ${goalUnit(goal)}".trim()
            }
            ListItem(
                headlineContent = { Text(listOfNotNull(formatDate(log.date), amount).joinToString(" · ")) },
                supportingContent = log.note?.let { { Text(it) } },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onClick(log) },
            )
        }
    }
}

/** Readings over time, with the start and target values as the vertical range. */
@Composable
private fun MeasureChart(goal: Goal, logs: List<GoalLog>) {
    val points = (listOfNotNull(goal.startValue?.let { goal.startDate to it }) + logs.map { it.date to it.amount }).sortedBy { it.first }
    if (points.size < 2) return
    val line = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outlineVariant
    val values = points.map { it.second } + listOfNotNull(goal.targetValue)
    val minV = values.min()
    val maxV = values.max().let { if (it == minV) minV + 1 else it }
    val minT = points.first().first
    val maxT = points.last().first.let { if (it == minT) minT + 1 else it }
    GroupedCard {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp)) {
            fun at(t: Long, v: Double) = Offset(
                ((t - minT).toFloat() / (maxT - minT)) * size.width,
                size.height - ((v - minV) / (maxV - minV)).toFloat() * size.height,
            )
            goal.targetValue?.let { target ->
                val y = at(minT, target).y
                drawLine(guide, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
            }
            points.zipWithNext().forEach { (a, b) -> drawLine(line, at(a.first, a.second), at(b.first, b.second), strokeWidth = 5f) }
            points.forEach { drawCircle(line, radius = 7f, center = at(it.first, it.second)) }
        }
    }
}

@Composable
private fun MilestoneList(milestones: List<GoalMilestone>, viewModel: GoalDetailViewModel) {
    var editMode by remember { mutableStateOf(false) }
    var newStep by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<GoalMilestone?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.goal_steps), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (milestones.isNotEmpty()) {
            TextButton(onClick = { editMode = !editMode }) {
                Text(stringResource(if (editMode) R.string.routine_done_editing else R.string.routine_edit_steps))
            }
        }
    }
    GroupedCard {
        milestones.forEachIndexed { index, m ->
            if (index > 0) HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                if (!editMode) Checkbox(checked = m.doneAt != null, onCheckedChange = { viewModel.setMilestoneDone(m, it) })
                Text(
                    m.name,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 8.dp)
                        .then(if (editMode) Modifier.clickable { renaming = m } else Modifier),
                )
                if (editMode) {
                    IconButton(onClick = { viewModel.moveMilestone(m, -1) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_up))
                    }
                    IconButton(onClick = { viewModel.moveMilestone(m, 1) }, enabled = index < milestones.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_down))
                    }
                    IconButton(onClick = { viewModel.deleteMilestone(m) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                value = newStep, onValueChange = { newStep = it }, singleLine = true,
                label = { Text(stringResource(R.string.goal_add_step)) }, modifier = Modifier.weight(1f).imePadding(),
            )
            TextButton(enabled = newStep.isNotBlank(), onClick = { viewModel.addMilestone(newStep.trim()); newStep = "" }) {
                Text(stringResource(R.string.common_add))
            }
        }
    }
    renaming?.let { m ->
        var text by remember(m.id) { mutableStateOf(m.name) }
        AlertDialog(
            properties = FormDialogProperties,
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.goal_steps)) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = { viewModel.renameMilestone(m, text.trim()); renaming = null }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

/** A number goal's amount, a habit's day or a new reading; with date and note. */
@Composable
private fun LogDialog(goal: Goal, log: GoalLog, onSave: (Long, Double, String?) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val habit = goal.kind == GoalKind.HABIT
    var amount by remember { mutableStateOf(if (log.id != 0L && !habit) formatRate(log.amount) else "") }
    var date by remember { mutableStateOf(log.date) }
    var note by remember { mutableStateOf(log.note.orEmpty()) }
    val parsed = if (habit) 1.0 else amount.trim().replace(',', '.').toDoubleOrNull()
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (goal.kind == GoalKind.MEASURE) R.string.goal_reading_title else R.string.goal_log_title)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                if (!habit) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it }, singleLine = true,
                        isError = amount.isNotBlank() && parsed == null,
                        label = {
                            Text(listOf(stringResource(if (goal.kind == GoalKind.MEASURE) R.string.goal_field_value else R.string.goal_field_amount), goalUnit(goal)).filter { it.isNotBlank() }.joinToString(" · "))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DayField(stringResource(R.string.goal_field_date), date, { it?.let { d -> date = d } })
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, singleLine = true,
                    label = { Text(stringResource(R.string.goal_field_note)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let { onSave(date, it, note.trim().takeIf { n -> n.isNotEmpty() }) } }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
