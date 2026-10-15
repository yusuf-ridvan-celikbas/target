package com.ridvan.target.ui.sports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.formatDuration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

/** First instant of the month containing [millis]. */
fun monthStartOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long = Calendar.getInstance(zone).apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** [monthStart] moved by [delta] months (still a month start). */
fun shiftMonth(monthStart: Long, delta: Int, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply { timeInMillis = monthStart; add(Calendar.MONTH, delta) }.timeInMillis

/** Workouts in the month starting at [monthStart], optionally one [sport], newest first. */
fun historyFor(workouts: List<Workout>, monthStart: Long, sport: Sport?, zone: TimeZone = TimeZone.getDefault()): List<Workout> {
    val end = shiftMonth(monthStart, 1, zone)
    return workouts.filter { it.startedAt >= monthStart && it.startedAt < end && (sport == null || it.sport == sport) }
        .sortedByDescending { it.startedAt }
}

/** Groups workouts by local day, newest day first; each group is keyed by its day start. */
fun groupByDay(workouts: List<Workout>, zone: TimeZone = TimeZone.getDefault()): List<Pair<Long, List<Workout>>> {
    val groups = LinkedHashMap<Long, MutableList<Workout>>()
    workouts.sortedByDescending { it.startedAt }.forEach { w ->
        val day = Calendar.getInstance(zone).apply {
            timeInMillis = w.startedAt
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        groups.getOrPut(day) { mutableListOf() }.add(w)
    }
    return groups.map { it.key to it.value.toList() }
}

private sealed interface HistoryItem {
    data class Header(val day: Long) : HistoryItem
    data class Row(val workout: Workout) : HistoryItem
}

/** All logged workouts by day, with a month switcher and a sport filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsHistoryScreen(
    onBack: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    viewModel: SportsViewModel = viewModel(),
) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    var month by rememberSaveable { mutableLongStateOf(monthStartOf(System.currentTimeMillis())) }
    var sportName by rememberSaveable { mutableStateOf<String?>(null) }
    val sport = sportName?.let { name -> Sport.entries.firstOrNull { it.name == name } }

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

    val shown = historyFor(workouts, month, sport)
    val items = groupByDay(shown).flatMap { (day, list) ->
        listOf<HistoryItem>(HistoryItem.Header(day)) + list.map { HistoryItem.Row(it) }
    }
    val totals = totalsOf(shown)
    val monthLabel = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(Date(month))
    val sportsInHistory = workouts.map { it.sport }.distinct().sortedBy { it.ordinal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sports_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { month = shiftMonth(month, -1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.sports_history_prev_month))
                        }
                        Text(monthLabel, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        IconButton(onClick = { month = shiftMonth(month, 1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.sports_history_next_month))
                        }
                    }
                    DropdownField(
                        selectedLabel = sport?.label() ?: stringResource(R.string.sports_history_all_sports),
                        label = stringResource(R.string.sports_field_sport),
                        modifier = Modifier.fillMaxWidth(),
                    ) { close ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sports_history_all_sports)) },
                            onClick = { sportName = null; close() },
                        )
                        sportsInHistory.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label()) },
                                leadingIcon = { Icon(option.icon(), contentDescription = null) },
                                onClick = { sportName = option.name; close() },
                            )
                        }
                    }
                    if (shown.isNotEmpty()) {
                        val parts = mutableListOf(
                            countText(R.plurals.count_workouts, totals.count),
                            formatDuration(totals.minutes.coerceAtLeast(1)),
                        )
                        if (totals.meters > 0) {
                            val meters = totals.meters.toInt()
                            parts += if (sport == Sport.SWIMMING && meters < 1000) {
                                stringResource(R.string.sports_distance_m, meters)
                            } else {
                                stringResource(R.string.sports_distance_km, formatKm(meters))
                            }
                        }
                        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.sports_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(items, key = { if (it is HistoryItem.Header) "h${it.day}" else "w${(it as HistoryItem.Row).workout.id}" }) { item ->
                when (item) {
                    is HistoryItem.Header -> Text(
                        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(item.day)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    is HistoryItem.Row -> WorkoutRow(item.workout, onClick = { onWorkoutClick(item.workout.id) }, onDelete = onDelete)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
