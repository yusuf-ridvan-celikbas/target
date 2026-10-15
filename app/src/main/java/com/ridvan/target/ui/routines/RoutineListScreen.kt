package com.ridvan.target.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun RoutineListScreen(
    shellNavigation: ShellNavigation,
    onRoutineClick: (Long) -> Unit,
    viewModel: RoutineListViewModel = viewModel(),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.ROUTINES,
        title = stringResource(R.string.label_routines),
        floatingActionButton = { AddFab(onClick = { showAdd = true }) },
    ) { innerPadding ->
        if (routines.isEmpty()) {
            Column(modifier = Modifier.padding(innerPadding).padding(24.dp)) {
                Text(stringResource(R.string.routines_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(routines, key = { it.routine.id }) { item ->
                    val routine = item.routine
                    GroupedCard(modifier = Modifier.padding(bottom = 8.dp).clickable { onRoutineClick(routine.id) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoutineIcon(routine.category, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(
                                    routine.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                                if (!routine.isActive) {
                                    Text(stringResource(R.string.routine_paused), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else item.today?.let {
                                    Text(routineProgressText(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                listOf(routineScheduleLine(routine), countText(R.plurals.count_steps, item.stepCount)).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                item { Text("", modifier = Modifier.padding(bottom = 72.dp)) }
            }
        }
    }

    if (showAdd) {
        RoutineFormDialog(
            initial = null,
            onConfirm = { routine ->
                showAdd = false
                viewModel.addRoutine(routine, onRoutineClick)
            },
            onDismiss = { showAdd = false },
        )
    }
}
