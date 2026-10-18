package com.ridvan.target.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.home.HomeCard
import com.ridvan.target.data.home.HomeItem
import com.ridvan.target.data.home.HomeShortcut
import com.ridvan.target.data.home.ShortcutTarget
import com.ridvan.target.data.home.isEnabled
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.common.GroupedCard

private fun HomeShortcut.icon(): ImageVector = when (this) {
    HomeShortcut.EXAMS -> Icons.Filled.School
    HomeShortcut.COURSES -> Icons.Filled.Category
    HomeShortcut.STUDY_RESOURCES -> Icons.Filled.Inventory2
    HomeShortcut.STATISTICS -> Icons.Filled.BarChart
    HomeShortcut.EXAM -> Icons.Filled.Description
    HomeShortcut.NEW_MOCK_EXAM -> Icons.Filled.AddTask
    HomeShortcut.FOCUS_TIMER -> Icons.Filled.Timer
    HomeShortcut.FLOW_TIMER -> Icons.Filled.HourglassBottom
    HomeShortcut.STUDY_HISTORY -> Icons.Filled.History
    HomeShortcut.LIBRARY -> Icons.Filled.LocalLibrary
    HomeShortcut.BOOK -> Icons.Filled.Book
    HomeShortcut.PLANNER -> Icons.Filled.CalendarMonth
    HomeShortcut.ROUTINES -> Icons.Filled.Checklist
    HomeShortcut.ROUTINE -> Icons.Filled.TaskAlt
    HomeShortcut.TRIPS -> Icons.Filled.Luggage
    HomeShortcut.TRIP -> Icons.Filled.Flight
    HomeShortcut.SPORTS -> Icons.Filled.FitnessCenter
    HomeShortcut.LIVE_WORKOUT -> Icons.AutoMirrored.Filled.DirectionsRun
    HomeShortcut.MONEY -> Icons.Filled.AccountBalanceWallet
    HomeShortcut.ADD_EXPENSE -> Icons.Filled.AddCard
}

@Composable
private fun shortcutName(shortcut: HomeShortcut): String = stringResource(
    when (shortcut) {
        HomeShortcut.EXAMS -> R.string.home_shortcut_exams
        HomeShortcut.COURSES -> R.string.home_shortcut_courses
        HomeShortcut.STUDY_RESOURCES -> R.string.home_shortcut_study_resources
        HomeShortcut.STATISTICS -> R.string.home_shortcut_statistics
        HomeShortcut.EXAM -> R.string.home_shortcut_exam
        HomeShortcut.NEW_MOCK_EXAM -> R.string.home_shortcut_new_mock_exam
        HomeShortcut.FOCUS_TIMER -> R.string.home_shortcut_focus_timer
        HomeShortcut.FLOW_TIMER -> R.string.home_shortcut_flow_timer
        HomeShortcut.STUDY_HISTORY -> R.string.home_shortcut_study_history
        HomeShortcut.LIBRARY -> R.string.home_shortcut_library
        HomeShortcut.BOOK -> R.string.home_shortcut_book
        HomeShortcut.PLANNER -> R.string.home_shortcut_planner
        HomeShortcut.ROUTINES -> R.string.home_shortcut_routines
        HomeShortcut.ROUTINE -> R.string.home_shortcut_routine
        HomeShortcut.TRIPS -> R.string.home_shortcut_trips
        HomeShortcut.TRIP -> R.string.home_shortcut_trip
        HomeShortcut.SPORTS -> R.string.home_shortcut_sports
        HomeShortcut.LIVE_WORKOUT -> R.string.home_shortcut_live_workout
        HomeShortcut.MONEY -> R.string.label_money
        HomeShortcut.ADD_EXPENSE -> R.string.home_shortcut_add_expense
    }
)

@Composable
private fun cardName(card: HomeCard): String = stringResource(
    when (card) {
        HomeCard.COUNTDOWNS -> R.string.home_card_countdowns
        HomeCard.PLANS -> R.string.home_card_plans
        HomeCard.ROUTINES -> R.string.home_card_routines
        HomeCard.TODAY_SO_FAR -> R.string.home_card_today_so_far
        HomeCard.WORK_ON_NEXT -> R.string.home_card_work_on_next
        HomeCard.QUICK_START -> R.string.home_card_quick_start
        HomeCard.MONEY_MONTH -> R.string.home_card_money
    }
)

/** A shortcut's tile text: the item's own name for item shortcuts ("New mock exam: ALES" for that one). */
@Composable
fun shortcutLabel(item: HomeItem.Shortcut, targets: Map<ShortcutTarget, List<Pair<Long, String>>>): String? {
    val target = item.shortcut.target ?: return shortcutName(item.shortcut)
    val name = targets[target]?.firstOrNull { it.first == item.targetId }?.second ?: return null
    return if (item.shortcut == HomeShortcut.NEW_MOCK_EXAM) stringResource(R.string.home_shortcut_new_mock_exam_for, name) else name
}

/** Shortcut tiles, three per row. Item shortcuts whose item is gone are left out. */
@Composable
fun ShortcutGrid(
    items: List<HomeItem.Shortcut>,
    targets: Map<ShortcutTarget, List<Pair<Long, String>>>,
    onClick: (HomeItem.Shortcut) -> Unit,
) {
    val shown = items.mapNotNull { item -> shortcutLabel(item, targets)?.let { item to it } }
    if (shown.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (item, label) ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).height(88.dp).clickable { onClick(item) },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Icon(item.shortcut.icon(), contentDescription = null, modifier = Modifier.size(28.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Edit Home: every card and shortcut as a row with move up/down and remove, plus Add and Reset.
 * Each change is handed to [onChange] right away (saved immediately).
 */
@Composable
fun HomeEditor(
    items: List<HomeItem>,
    modules: Set<AppModule>,
    targets: Map<ShortcutTarget, List<Pair<Long, String>>>,
    onChange: (List<HomeItem>) -> Unit,
    onReset: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    // Items of modules that are off stay saved but aren't listed.
    val visible = items.filter { it.isEnabled(modules) }
    Text(stringResource(R.string.home_edit_hint), style = MaterialTheme.typography.bodyMedium)
    GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
        if (visible.isEmpty()) {
            Text(stringResource(R.string.home_edit_empty), modifier = Modifier.padding(16.dp))
        }
        visible.forEachIndexed { index, item ->
            val (icon, title) = when (item) {
                is HomeItem.Card -> Icons.Filled.Dashboard to cardName(item.card)
                is HomeItem.Shortcut -> item.shortcut.icon() to (shortcutLabel(item, targets) ?: shortcutName(item.shortcut))
            }
            ListItem(
                leadingContent = { Icon(icon, contentDescription = null) },
                headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(stringResource(if (item is HomeItem.Card) R.string.home_edit_kind_card else R.string.home_edit_kind_shortcut))
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { onChange(items.move(item, -1, visible)) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.home_edit_move_up))
                        }
                        IconButton(onClick = { onChange(items.move(item, 1, visible)) }, enabled = index < visible.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.home_edit_move_down))
                        }
                        IconButton(onClick = { onChange(items - item) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_edit_remove))
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (index < visible.lastIndex) HorizontalDivider()
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        OutlinedButton(onClick = { adding = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.home_edit_add), modifier = Modifier.padding(start = 4.dp))
        }
        TextButton(onClick = onReset) { Text(stringResource(R.string.home_edit_reset)) }
    }

    if (adding) {
        AddHomeItemDialog(
            current = items,
            modules = modules,
            targets = targets,
            onAdd = { onChange(items + it); adding = false },
            onDismiss = { adding = false },
        )
    }
}

/** Moves [item] one visible place up/down, keeping hidden (module-off) items where they are. */
private fun List<HomeItem>.move(item: HomeItem, step: Int, visible: List<HomeItem>): List<HomeItem> {
    val neighbour = visible.getOrNull(visible.indexOf(item) + step) ?: return this
    val a = indexOf(item)
    val b = indexOf(neighbour)
    return toMutableList().also { it[a] = neighbour; it[b] = item }
}

private val SHORTCUT_GROUPS = listOf(
    R.string.home_add_group_study to listOf(
        HomeShortcut.EXAMS, HomeShortcut.EXAM, HomeShortcut.NEW_MOCK_EXAM, HomeShortcut.COURSES,
        HomeShortcut.STUDY_RESOURCES, HomeShortcut.STATISTICS,
    ),
    R.string.home_add_group_timers to listOf(
        HomeShortcut.FOCUS_TIMER, HomeShortcut.FLOW_TIMER, HomeShortcut.STUDY_HISTORY, HomeShortcut.LIBRARY, HomeShortcut.BOOK,
    ),
    R.string.home_add_group_plan to listOf(
        HomeShortcut.PLANNER, HomeShortcut.ROUTINES, HomeShortcut.ROUTINE, HomeShortcut.TRIPS, HomeShortcut.TRIP,
    ),
    R.string.home_add_group_sports to listOf(HomeShortcut.SPORTS, HomeShortcut.LIVE_WORKOUT),
    R.string.label_money to listOf(HomeShortcut.MONEY, HomeShortcut.ADD_EXPENSE),
)

/** Cards not on Home yet, then shortcuts by area; item shortcuts ask which item next. */
@Composable
private fun AddHomeItemDialog(
    current: List<HomeItem>,
    modules: Set<AppModule>,
    targets: Map<ShortcutTarget, List<Pair<Long, String>>>,
    onAdd: (HomeItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf<HomeShortcut?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (picking == null) R.string.home_edit_add else R.string.home_add_pick_item)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val shortcut = picking
                if (shortcut != null) {
                    val options = targets[shortcut.target].orEmpty()
                        .filterNot { (id, _) -> HomeItem.Shortcut(shortcut, id) in current }
                    if (options.isEmpty()) Text(stringResource(R.string.home_add_nothing_to_pick))
                    options.forEach { (id, name) ->
                        AddRow(name, shortcut.icon()) { onAdd(HomeItem.Shortcut(shortcut, id)) }
                    }
                } else {
                    val cards = HomeCard.entries.map { HomeItem.Card(it) }.filter { it !in current && it.isEnabled(modules) }
                    if (cards.isNotEmpty()) {
                        GroupTitle(stringResource(R.string.home_add_group_cards))
                        cards.forEach { item -> AddRow(cardName(item.card), Icons.Filled.Dashboard) { onAdd(item) } }
                    }
                    SHORTCUT_GROUPS.forEach { (title, shortcuts) ->
                        val available = shortcuts.filter { s ->
                            s.module in modules && (s.target != null || HomeItem.Shortcut(s) !in current)
                        }
                        if (available.isNotEmpty()) {
                            GroupTitle(stringResource(title))
                            available.forEach { s ->
                                AddRow(shortcutName(s), s.icon()) {
                                    if (s.target != null) picking = s else onAdd(HomeItem.Shortcut(s))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (picking != null) picking = null else onDismiss() }) {
                Text(stringResource(if (picking != null) R.string.home_add_back else R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun GroupTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun AddRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(text, modifier = Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
