package com.ridvan.target.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.daysUntilLabel
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
    viewModel: HomeViewModel = viewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()

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
                UpcomingEventsCard(upcomingEvents)
            }
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
private fun UpcomingEventsCard(events: List<UpcomingEvent>) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.home_upcoming_title), style = MaterialTheme.typography.titleMedium)
            events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(event.label, modifier = Modifier.weight(1f))
                    Text(
                        daysUntilLabel(event.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
