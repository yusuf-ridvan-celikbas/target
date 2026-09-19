package com.ridvan.target.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.daysUntilLabel
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.planner.PlannerPreviewOccurrence
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    shellNavigation: ShellNavigation,
    onExamClick: (Long) -> Unit,
    onSectionClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val upcomingPlannerEvents by viewModel.upcomingPlannerEvents.collectAsStateWithLifecycle()

    AppShell(navigation = shellNavigation, currentDestination = ShellDestination.HOME) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val prefix = stringResource(R.string.home_welcome_prefix)
            val suffix = stringResource(R.string.home_welcome_suffix)
            Text(
                buildAnnotatedString {
                    append(prefix)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentUser?.preferredName ?: "")
                    }
                    append(suffix)
                },
            )
            Spacer(Modifier.height(16.dp))
            ClockCard()
            if (upcomingEvents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                UpcomingEventsCard(
                    events = upcomingEvents,
                    onEventClick = { event ->
                        if (event.sectionId != null) onSectionClick(event.sectionId) else onExamClick(event.examId)
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            PlannerPreviewCard(
                occurrences = upcomingPlannerEvents,
                onToggleDone = viewModel::togglePlannerOccurrenceDone,
                onOpenPlanner = shellNavigation.onNavigatePlanner,
            )
        }
    }
}

/** A compact "what's in front of us" preview of the Planner — events only (Upcoming above
 *  already covers exam/section dates on its own) — always shown, unlike Upcoming, so Home
 *  also doubles as a quick entry point into the full Planner even with nothing logged yet. */
@Composable
private fun PlannerPreviewCard(
    occurrences: List<PlannerPreviewOccurrence>,
    onToggleDone: (PlannerPreviewOccurrence) -> Unit,
    onOpenPlanner: () -> Unit,
) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.label_planner),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.home_open_planner),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onOpenPlanner),
                )
            }
            if (occurrences.isEmpty()) {
                Text(
                    stringResource(R.string.home_planner_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                occurrences.forEach { occurrence ->
                    PlannerPreviewRow(
                        occurrence = occurrence,
                        onToggleDone = { onToggleDone(occurrence) },
                        onOpenPlanner = onOpenPlanner,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerPreviewRow(occurrence: PlannerPreviewOccurrence, onToggleDone: () -> Unit, onOpenPlanner: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "plannerPreviewLabelColor",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = occurrence.isCompleted, onCheckedChange = { onToggleDone() })
        Text(
            occurrence.event.title,
            color = labelColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenPlanner),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                daysUntilLabel(occurrence.occurrenceDateMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatDate(occurrence.occurrenceDateMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ticks once a minute — a study tracker doesn't need second-accurate updates. */
@Composable
private fun ClockCard() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - (now % 60_000))
        }
    }
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    val dateText = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(now))
    GroupedCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(timeText, style = MaterialTheme.typography.displayMedium)
            Text(dateText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun UpcomingEventsCard(events: List<UpcomingEvent>, onEventClick: (UpcomingEvent) -> Unit) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_upcoming_title), style = MaterialTheme.typography.titleMedium)
            events.forEach { event ->
                UpcomingEventRow(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
private fun UpcomingEventRow(event: UpcomingEvent, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val labelColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "upcomingEventLabelColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            event.label,
            color = labelColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                daysUntilLabel(event.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatDate(event.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
