package com.ridvan.target.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.TripWithTotals
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun TripListScreen(
    shellNavigation: ShellNavigation,
    onTripClick: (Long) -> Unit,
    viewModel: TripListViewModel = viewModel(),
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val today = remember { startOfTodayMillis() }
    // Ongoing and upcoming first (soonest first), then past ones (most recent first).
    val (past, current) = trips.partition { it.trip.endDate < today }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.TRIPS,
        title = stringResource(R.string.label_trips),
        floatingActionButton = { AddFab(onClick = { showAdd = true }) },
    ) { innerPadding ->
        if (trips.isEmpty()) {
            Column(modifier = Modifier.padding(innerPadding).padding(24.dp)) {
                Text(stringResource(R.string.trips_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
                if (current.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.trips_upcoming)) }
                    items(current, key = { it.trip.id }) { TripCard(it, today) { onTripClick(it.trip.id) } }
                }
                if (past.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.trips_past)) }
                    items(past.reversed(), key = { it.trip.id }) { TripCard(it, today) { onTripClick(it.trip.id) } }
                }
                item { Text("", modifier = Modifier.padding(bottom = 72.dp)) }
            }
        }
    }

    if (showAdd) {
        TripDialog(
            initial = null,
            onConfirm = { trip ->
                showAdd = false
                viewModel.addTrip(trip, onTripClick)
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun TripCard(item: TripWithTotals, today: Long, onClick: () -> Unit) {
    val trip = item.trip
    GroupedCard(modifier = Modifier.padding(bottom = 8.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Luggage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(tripPhaseLabel(tripPhase(trip, today)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                listOfNotNull(trip.destination, "${formatDate(trip.startDate)} – ${formatDate(trip.endDate)}").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            val parts = listOfNotNull(
                stringResource(R.string.trips_card_days, tripDayCount(trip)),
                item.bookingCount.takeIf { it > 0 }?.let { stringResource(R.string.trips_card_bookings, it) },
                item.packingCount.takeIf { it > 0 }?.let { stringResource(R.string.trips_card_packed, item.packedCount, it) },
                (item.spentMinor > 0 || trip.budgetMinor != null).takeIf { it }?.let {
                    trip.budgetMinor?.let { budget ->
                        stringResource(R.string.trips_card_spent_of, formatMoney(item.spentMinor, trip.currency), formatMoney(budget, trip.currency))
                    } ?: stringResource(R.string.trips_card_spent, formatMoney(item.spentMinor, trip.currency))
                },
            )
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            if (item.packingCount > 0) {
                LinearProgressIndicator(
                    progress = { item.packedCount.toFloat() / item.packingCount },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
