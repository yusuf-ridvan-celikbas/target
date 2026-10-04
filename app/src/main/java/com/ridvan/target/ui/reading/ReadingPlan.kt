package com.ridvan.target.ui.reading

import com.ridvan.target.ui.common.formatRate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.focustimer.localDayToUtcMillis
import com.ridvan.target.ui.focustimer.utcMillisToLocalDay

private val PACE_CHOICES = listOf(10, 20, 30, 50)

/**
 * A book's finish plan: when it'll be finished at the recent pace, and — once a goal date is set —
 * today's page target with roughly how long it'll take. Only for unfinished books with a page count.
 */
@Composable
fun ReadingPlanCard(
    progress: BookProgress,
    bookSessions: List<FocusSession>,
    allReadingSessions: List<FocusSession>,
    onSetGoal: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val book = progress.book
    val total = book.totalPages ?: return
    if (book.isFinished) return
    val today = dayStart(System.currentTimeMillis())
    val remaining = (total - progress.currentPage).coerceAtLeast(0)
    val pace = readingPace(bookSessions, allReadingSessions, today)
    val goal = dailyGoal(book, bookSessions, today)
    var showDialog by remember { mutableStateOf(false) }

    GroupedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reading_plan_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.reading_pages_left, remaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val perDay = pace.pagesPerDay
            Text(
                if (perDay != null) {
                    stringResource(R.string.reading_forecast, formatRate(perDay), formatDate(finishDate(remaining, perDay, today)))
                } else {
                    stringResource(R.string.reading_forecast_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (goal == null) {
                Button(onClick = { showDialog = true }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.reading_goal_set))
                }
            } else {
                Text(
                    stringResource(R.string.reading_goal_line, formatDate(goal.goalDate)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    goalTodayLine(goal, pace.pagesPerHour),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (goal.done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row {
                    TextButton(onClick = { showDialog = true }) { Text(stringResource(R.string.reading_goal_change)) }
                    TextButton(onClick = { onSetGoal(null) }) { Text(stringResource(R.string.reading_goal_remove)) }
                }
            }
        }
    }

    if (showDialog) {
        ReadingGoalDialog(
            remaining = remaining,
            pace = pace,
            initialGoalDate = book.goalDate?.takeIf { it >= today },
            today = today,
            onConfirm = { date ->
                onSetGoal(date)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

/** "Today: 8 / 15 pages · about 14 min left", "Today's goal is done" or the passed-date note — shared with Home. */
@Composable
fun goalTodayLine(goal: DailyGoal, pagesPerHour: Double?): String = when {
    goal.passed -> stringResource(R.string.reading_goal_passed)
    goal.done -> stringResource(R.string.reading_goal_today_done)
    else -> listOfNotNull(
        stringResource(R.string.reading_goal_today, goal.readToday, goal.target),
        minutesFor(goal.remainingToday, pagesPerHour)?.let { stringResource(R.string.reading_goal_minutes_left, it) },
    ).joinToString(" · ")
}

private enum class GoalMode { DATE, PACE }

/**
 * Pick a finish date (→ pages a day) or a pages-a-day pace (→ finish date); either way the goal
 * saved is a finish date, so the daily target keeps adjusting to what's actually been read.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ReadingGoalDialog(
    remaining: Int,
    pace: ReadingPace,
    initialGoalDate: Long?,
    today: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(GoalMode.DATE) }
    // Default date: the forecast, or two weeks out without one.
    var goalDate by remember {
        mutableStateOf(initialGoalDate ?: pace.pagesPerDay?.let { finishDate(remaining, it, today) } ?: addDays(today, 13))
    }
    var pagesText by remember { mutableStateOf((pace.pagesPerDay?.let { kotlin.math.ceil(it).toInt() } ?: 20).toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val pagesPerDay = pagesText.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reading_goal_dialog_title)) },
        text = {
            Column {
                SegmentedToggle(
                    options = listOf(
                        SegmentedToggleOption(GoalMode.DATE, stringResource(R.string.reading_goal_mode_date)),
                        SegmentedToggleOption(GoalMode.PACE, stringResource(R.string.reading_goal_mode_pace)),
                    ),
                    selected = mode,
                    onSelect = { mode = it },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.reading_pages_left, remaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                when (mode) {
                    GoalMode.DATE -> {
                        Text(
                            stringResource(R.string.reading_goal_finish_by),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            formatDate(goalDate),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                            modifier = Modifier.padding(top = 2.dp).clickable { showDatePicker = true },
                        )
                        val needed = pagesPerDayNeeded(remaining, goalDate, today) ?: 0
                        ResultLines(
                            first = stringResource(R.string.reading_goal_needed, needed),
                            minutes = minutesFor(needed, pace.pagesPerHour),
                        )
                    }
                    GoalMode.PACE -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            PACE_CHOICES.forEach { choice ->
                                FilterChip(
                                    selected = pagesPerDay == choice,
                                    onClick = { pagesText = choice.toString() },
                                    label = { Text(choice.toString()) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = pagesText,
                            onValueChange = { pagesText = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text(stringResource(R.string.reading_goal_custom)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        if (pagesPerDay != null) {
                            ResultLines(
                                first = stringResource(R.string.reading_goal_finish_on, formatDate(finishDate(remaining, pagesPerDay.toDouble(), today))),
                                minutes = minutesFor(pagesPerDay, pace.pagesPerHour),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = mode == GoalMode.DATE || pagesPerDay != null,
                onClick = {
                    onConfirm(
                        when (mode) {
                            GoalMode.DATE -> goalDate
                            GoalMode.PACE -> finishDate(remaining, (pagesPerDay ?: 1).toDouble(), today)
                        },
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )

    if (showDatePicker) {
        // Material3's DatePicker speaks UTC-midnight millis; only today or later can be picked.
        val todayUtc = localDayToUtcMillis(today)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localDayToUtcMillis(goalDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { goalDate = utcMillisToLocalDay(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ResultLines(first: String, minutes: Int?) {
    Text(first, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    minutes?.let {
        Text(
            stringResource(R.string.reading_goal_minutes_day, it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

