package com.ridvan.target.ui.sports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.GoalPeriod
import com.ridvan.target.data.local.entity.GoalTargetType
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.ui.common.formatTime
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val RECENT_PAGE = 10

/** Sports: period summary with chart, goals, personal bests, streaks and recent workouts. */
@Composable
fun SportsScreen(
    shellNavigation: ShellNavigation,
    onStartLive: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    viewModel: SportsViewModel = viewModel(),
) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val liftRows by viewModel.liftRows.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<SportGoal?>(null) }
    var shown by remember { mutableStateOf(RECENT_PAGE) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.sports_workout_deleted)
    val undoLabel = stringResource(R.string.common_undo)
    val onDelete: (Workout) -> Unit = { workout ->
        viewModel.delete(workout) { deleted ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) viewModel.restore(deleted)
            }
        }
    }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.SPORTS,
        title = stringResource(R.string.label_sports),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { AddFab(onClick = { showAdd = true }) },
    ) { innerPadding ->
        val now = System.currentTimeMillis()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        ) {
            Button(onClick = onStartLive, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.sports_start_live), modifier = Modifier.padding(start = 8.dp))
            }

            SummaryCard(workouts, period, now, onPeriod = viewModel::setPeriod)
            GoalsCard(goals, workouts, now, onAdd = { showAddGoal = true }, onDelete = { goalToDelete = it })
            BestsCard(workouts, liftRows,onWorkoutClick)
            StreaksCard(workouts, now)

            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.sports_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (workouts.isEmpty()) {
                        Text(
                            stringResource(R.string.sports_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    workouts.take(shown).forEach { workout ->
                        WorkoutRow(workout, onClick = { onWorkoutClick(workout.id) }, onDelete = onDelete)
                    }
                    if (workouts.size > shown) {
                        TextButton(onClick = { shown += RECENT_PAGE }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(stringResource(R.string.sports_show_more))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        WorkoutFormDialog(
            initial = null,
            initialExercises = emptyList(),
            onConfirm = { form -> viewModel.addWorkout(form); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    if (showAddGoal) {
        GoalDialog(
            onConfirm = { goal -> viewModel.addGoal(goal); showAddGoal = false },
            onDismiss = { showAddGoal = false },
        )
    }
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(stringResource(R.string.sports_goal_delete_title)) },
            text = { Text(stringResource(R.string.sports_goal_delete_message, goalTitle(goal))) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGoal(goal); goalToDelete = null }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun SummaryCard(workouts: List<Workout>, period: SportsPeriod, now: Long, onPeriod: (SportsPeriod) -> Unit) {
    val start = periodStart(period, now)
    val end = periodEnd(period, start)
    val inPeriod = workoutsIn(workouts, start, end)
    val totals = totalsOf(inPeriod)
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(SportsPeriod.WEEK, stringResource(R.string.sports_this_week)),
                    SegmentedToggleOption(SportsPeriod.MONTH, stringResource(R.string.sports_this_month)),
                ),
                selected = period,
                onSelect = onPeriod,
            )
            if (inPeriod.isEmpty()) {
                Text(
                    stringResource(R.string.sports_empty_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                return@Column
            }
            Text(
                listOfNotNull(
                    countText(R.plurals.count_workouts, totals.count),
                    formatDuration(totals.minutes),
                    totals.meters.takeIf { it > 0 }?.let { stringResource(R.string.sports_distance_km, formatKm(it.toInt())) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            val minutes = dailyMinutes(workouts, start, end)
            val format = remember(period) { SimpleDateFormat(if (period == SportsPeriod.WEEK) "EEE" else "d", Locale.getDefault()) }
            val labels = remember(start, minutes.size) {
                val c = Calendar.getInstance().apply { timeInMillis = start }
                minutes.map { format.format(Date(c.timeInMillis)).also { c.add(Calendar.DAY_OF_YEAR, 1) } }
            }
            MinutesBarChart(minutes, labels, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            Text(
                stringResource(R.string.sports_chart_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            totalsBySport(inPeriod).forEach { (sport, sportTotals) ->
                val sportWorkouts = inPeriod.filter { it.sport == sport }
                // Average pace over the workouts that have a distance.
                val withDistance = sportWorkouts.filter { (it.distanceMeters ?: 0) > 0 }
                val pace = if (sport.isDistance && withDistance.isNotEmpty()) {
                    paceText(sport, withDistance.sumOf { it.durationSeconds }, withDistance.sumOf { it.distanceMeters!! })
                } else null
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Icon(sport.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(sport.label())
                        Text(
                            listOfNotNull(
                                countText(R.plurals.count_workouts, sportTotals.count),
                                formatDuration(sportTotals.minutes),
                                sportTotals.meters.takeIf { it > 0 }?.let { distanceText(it.toInt(), sport) },
                                pace,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Minutes per day as bars; only every Nth bar is labelled so a month's labels don't collide. */
@Composable
private fun MinutesBarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelStep = (values.size / 7).coerceAtLeast(1)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1).coerceAtLeast(0)) / values.size
            values.forEachIndexed { index, value ->
                val x = index * (barWidth + gap)
                drawRect(trackColor, topLeft = Offset(x, size.height - 2.dp.toPx()), size = Size(barWidth, 2.dp.toPx()))
                if (value > 0) {
                    val h = size.height * (value.toFloat() / maxValue)
                    drawRect(barColor, topLeft = Offset(x, size.height - h), size = Size(barWidth, h))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEachIndexed { index, label ->
                Text(
                    if (index % labelStep == 0) label else "",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GoalsCard(goals: List<SportGoal>, workouts: List<Workout>, now: Long, onAdd: () -> Unit, onDelete: (SportGoal) -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sports_goals_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text(stringResource(R.string.sports_goal_add)) }
            }
            if (goals.isEmpty()) {
                Text(
                    stringResource(R.string.sports_goals_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            goals.forEach { goal ->
                val progress = goalProgress(goal, workouts, now)
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goalTitle(goal), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            goalProgressText(goal, progress),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (progress.reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onDelete(goal) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.sports_goal_delete_title))
                        }
                    }
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BestsCard(workouts: List<Workout>, liftRows: List<com.ridvan.target.data.local.dao.LiftRow>, onWorkoutClick: (Long) -> Unit) {
    val fiveK = fastestFiveK(workouts)
    val longest = listOf(Sport.RUNNING, Sport.CYCLING, Sport.SWIMMING).mapNotNull { sport -> longestDistance(workouts, sport)?.let { sport to it } }
    val lifts = heaviestLifts(liftRows).take(5)
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.sports_bests_title), style = MaterialTheme.typography.titleMedium)
            if (fiveK == null && longest.isEmpty() && lifts.isEmpty()) {
                Text(
                    stringResource(R.string.sports_bests_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            fiveK?.let { (workout, seconds) ->
                BestRow(stringResource(R.string.sports_best_5k), formatHms(Math.round(seconds)), formatDate(workout.startedAt)) { onWorkoutClick(workout.id) }
            }
            longest.forEach { (sport, workout) ->
                val title = stringResource(
                    when (sport) {
                        Sport.RUNNING -> R.string.sports_best_longest_run
                        Sport.CYCLING -> R.string.sports_best_longest_ride
                        else -> R.string.sports_best_longest_swim
                    },
                )
                BestRow(title, distanceText(workout.distanceMeters!!, sport), formatDate(workout.startedAt)) { onWorkoutClick(workout.id) }
            }
            lifts.forEach { lift ->
                BestRow(
                    lift.name,
                    stringResource(R.string.sports_best_lift, formatWeight(lift.weightKg), lift.reps),
                    formatDate(lift.startedAt),
                ) { onWorkoutClick(lift.workoutId) }
            }
        }
    }
}

@Composable
private fun BestRow(title: String, value: String, date: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StreaksCard(workouts: List<Workout>, now: Long) {
    val streaks = streaks(workouts, now)
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.sports_streaks_title), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(streaks.days.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.sports_streak_days), style = MaterialTheme.typography.labelMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(streaks.weeks.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.sports_streak_weeks), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** A recent-workouts row; swipe end-to-start to delete (with Undo). */
@Composable
private fun WorkoutRow(workout: Workout, onClick: () -> Unit, onDelete: (Workout) -> Unit) {
    key(workout.id) {
        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete(workout)
                    true
                } else {
                    false
                }
            },
        )
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            },
        ) {
            WorkoutRowContent(workout, onClick, Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
        }
    }
}

@Composable
internal fun WorkoutRowContent(workout: Workout, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { Icon(workout.sport.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(workout.sport.label()) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.focus_session_title, formatDate(workout.startedAt), formatTime(workout.startedAt)), style = MaterialTheme.typography.bodySmall)
                Text(workoutSummary(workout), style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}

@Composable
private fun GoalDialog(onConfirm: (SportGoal) -> Unit, onDismiss: () -> Unit) {
    var sport by remember { mutableStateOf<Sport?>(null) }
    var period by remember { mutableStateOf(GoalPeriod.WEEK) }
    var type by remember { mutableStateOf(GoalTargetType.DISTANCE_KM) }
    var valueText by remember { mutableStateOf("") }
    val value = parseDecimal(valueText)
    // Distance goals only make sense for a distance sport (or any sport).
    val typeOk = type != GoalTargetType.DISTANCE_KM || sport?.isDistance != false
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sports_goal_add_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DropdownField(
                    selectedLabel = sport?.label() ?: stringResource(R.string.sports_goal_any),
                    label = stringResource(R.string.sports_field_sport),
                ) { close ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.sports_goal_any)) }, onClick = { sport = null; close() })
                    Sport.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            leadingIcon = { Icon(option.icon(), contentDescription = null) },
                            onClick = { sport = option; close() },
                        )
                    }
                }
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(GoalPeriod.WEEK, stringResource(R.string.sports_goal_week)),
                        SegmentedToggleOption(GoalPeriod.MONTH, stringResource(R.string.sports_goal_month)),
                    ),
                    selected = period,
                    onSelect = { period = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(GoalTargetType.DISTANCE_KM, stringResource(R.string.sports_goal_type_distance)),
                        SegmentedToggleOption(GoalTargetType.MINUTES, stringResource(R.string.sports_goal_type_minutes)),
                        SegmentedToggleOption(GoalTargetType.WORKOUTS, stringResource(R.string.sports_goal_type_workouts)),
                    ),
                    selected = type,
                    onSelect = { type = it },
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                    label = { Text(stringResource(R.string.sports_goal_value)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (!typeOk) {
                    Text(
                        stringResource(R.string.sports_goal_distance_not_for_sport),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value != null && typeOk,
                onClick = { onConfirm(SportGoal(userId = 0, sport = sport, period = period, targetType = type, targetValue = value!!)) },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
