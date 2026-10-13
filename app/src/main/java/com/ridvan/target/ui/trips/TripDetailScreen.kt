package com.ridvan.target.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripPackingCategory
import com.ridvan.target.data.local.entity.TripPackingItem
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TripTab { ITINERARY, BOOKINGS, PACKING, BUDGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(onBack: () -> Unit, viewModel: TripDetailViewModel = viewModel()) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val packing by viewModel.packingItems.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(TripTab.ITINERARY) }
    var menuOpen by remember { mutableStateOf(false) }
    var editTrip by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<TripActivity?>(null) }
    var editingBooking by remember { mutableStateOf<TripBooking?>(null) }
    var editingPacking by remember { mutableStateOf<TripPackingItem?>(null) }
    var editingExpense by remember { mutableStateOf<TripExpense?>(null) }
    val today = remember { startOfTodayMillis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { editTrip = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.trip_edit_title))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                text = { Text(stringResource(R.string.trip_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            val t = trip ?: return@Scaffold
            AddFab(onClick = {
                val day = today.coerceIn(t.startDate, t.endDate)
                when (tab) {
                    TripTab.ITINERARY -> editingActivity = TripActivity(tripId = t.id, title = "", date = day)
                    TripTab.BOOKINGS -> editingBooking = TripBooking(tripId = t.id, type = TripBookingType.FLIGHT, title = "", startDate = t.startDate)
                    TripTab.PACKING -> editingPacking = TripPackingItem(tripId = t.id, name = "")
                    TripTab.BUDGET -> editingExpense = TripExpense(tripId = t.id, title = "", amountMinor = 0, date = day)
                }
            })
        },
    ) { innerPadding ->
        val t = trip ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Overview(t, today)
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(TripTab.ITINERARY, stringResource(R.string.trip_tab_itinerary)),
                    SegmentedToggleOption(TripTab.BOOKINGS, stringResource(R.string.trip_tab_bookings)),
                    SegmentedToggleOption(TripTab.PACKING, stringResource(R.string.trip_tab_packing)),
                    SegmentedToggleOption(TripTab.BUDGET, stringResource(R.string.trip_tab_budget)),
                ),
                selected = tab,
                onSelect = { tab = it },
                textStyle = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            when (tab) {
                TripTab.ITINERARY -> Itinerary(
                    trip = t,
                    today = today,
                    activities = activities,
                    bookings = bookings,
                    onAdd = { day -> editingActivity = TripActivity(tripId = t.id, title = "", date = day) },
                    onActivityClick = { editingActivity = it },
                    onToggleDone = viewModel::setActivityDone,
                    onBookingClick = { editingBooking = it },
                )
                TripTab.BOOKINGS -> BookingList(bookings) { editingBooking = it }
                TripTab.PACKING -> PackingList(
                    tripId = t.id,
                    items = packing,
                    onToggle = viewModel::setPacked,
                    onClick = { editingPacking = it },
                    onUnpackAll = viewModel::unpackAll,
                    onAddStarter = viewModel::addStarterItems,
                )
                TripTab.BUDGET -> Budget(t, expenses, today) { editingExpense = it }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    val t = trip ?: return
    if (editTrip) {
        TripDialog(initial = t, onConfirm = { viewModel.updateTrip(it); editTrip = false }, onDismiss = { editTrip = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.trip_delete)) },
            text = { Text(stringResource(R.string.trip_delete_message, t.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteTrip()
                    onBack()
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    editingActivity?.let { activity ->
        TripActivityDialog(
            trip = t,
            initial = activity,
            onConfirm = { viewModel.saveActivity(it); editingActivity = null },
            onDismiss = { editingActivity = null },
            onDelete = if (activity.id != 0L) ({ viewModel.deleteActivity(activity); editingActivity = null }) else null,
        )
    }
    editingBooking?.let { booking ->
        TripBookingDialog(
            trip = t,
            initial = booking,
            onConfirm = { saved, price -> viewModel.saveBooking(saved, price); editingBooking = null },
            onDismiss = { editingBooking = null },
            onDelete = if (booking.id != 0L) ({ viewModel.deleteBooking(booking); editingBooking = null }) else null,
        )
    }
    editingPacking?.let { item ->
        TripPackingItemDialog(
            initial = item,
            onConfirm = { viewModel.savePackingItem(it); editingPacking = null },
            onDismiss = { editingPacking = null },
            onDelete = if (item.id != 0L) ({ viewModel.deletePackingItem(item); editingPacking = null }) else null,
        )
    }
    editingExpense?.let { expense ->
        TripExpenseDialog(
            trip = t,
            initial = expense,
            onConfirm = { viewModel.saveExpense(it); editingExpense = null },
            onDismiss = { editingExpense = null },
            onDelete = if (expense.id != 0L) ({ viewModel.deleteExpense(expense); editingExpense = null }) else null,
        )
    }
}

private fun dayHeader(day: Long): String = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(day))

@Composable
private fun Overview(trip: Trip, today: Long) {
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(tripPhaseLabel(tripPhase(trip, today)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                listOfNotNull(trip.destination, "${formatDate(trip.startDate)} – ${formatDate(trip.endDate)}").joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                countText(R.plurals.count_days, tripDayCount(trip)) + " · " + trip.currency,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            trip.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun SmallRow(
    icon: ImageVector,
    title: String,
    lines: List<String>,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (leading != null) leading() else {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp).size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            lines.filter { it.isNotBlank() }.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let { Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp)) }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
}

private fun bookingTime(minute: Int?): String = minute?.let { minuteLabel(it) }.orEmpty()

@Composable
private fun Itinerary(
    trip: Trip,
    today: Long,
    activities: List<TripActivity>,
    bookings: List<TripBooking>,
    onAdd: (Long) -> Unit,
    onActivityClick: (TripActivity) -> Unit,
    onToggleDone: (TripActivity, Boolean) -> Unit,
    onBookingClick: (TripBooking) -> Unit,
) {
    tripDays(trip).forEachIndexed { index, day ->
        val isToday = day == today
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                stringResource(R.string.trip_day_header, index + 1, dayHeader(day)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isToday) FontWeight.Bold else null,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAdd(day) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trip_activity_add_title))
            }
        }
        val dayBookings = bookings.filter { it.startDate == day }
        val checkOuts = bookings.filter { it.endDate == day && it.endDate != it.startDate }
        val dayActivities = activities.filter { it.date == day }
        GroupedCard {
            if (dayBookings.isEmpty() && checkOuts.isEmpty() && dayActivities.isEmpty()) {
                EmptyHint(stringResource(R.string.trip_day_empty))
            }
            dayBookings.forEach { b ->
                SmallRow(
                    tripBookingTypeIcon(b.type), b.title,
                    listOf(listOfNotNull(tripBookingTypeLabel(b.type), b.place.name, b.arrivalPlace.name).joinToString(" · ")),
                    onClick = { onBookingClick(b) }, trailing = bookingTime(b.startMinuteOfDay),
                )
            }
            checkOuts.forEach { b ->
                SmallRow(
                    tripBookingTypeIcon(b.type), b.title,
                    listOf(stringResource(if (b.type.isTransport) R.string.trip_field_arrival else R.string.trip_field_check_out)),
                    onClick = { onBookingClick(b) }, trailing = bookingTime(b.endMinuteOfDay),
                )
            }
            dayActivities.forEach { a ->
                SmallRow(
                    tripActivityCategoryIcon(a.category), a.title,
                    listOfNotNull(timeRangeText(a.startMinuteOfDay, a.durationMinutes), a.place.label, a.notes),
                    onClick = { onActivityClick(a) },
                    leading = { Checkbox(checked = a.isDone, onCheckedChange = { onToggleDone(a, it) }) },
                )
            }
        }
    }
}

@Composable
private fun BookingList(bookings: List<TripBooking>, onClick: (TripBooking) -> Unit) {
    if (bookings.isEmpty()) {
        EmptyHint(stringResource(R.string.trip_bookings_empty))
        return
    }
    GroupedCard {
        bookings.forEach { b ->
            val start = listOfNotNull(formatDate(b.startDate), b.startMinuteOfDay?.let { minuteLabel(it) }).joinToString(" ")
            val end = b.endDate?.let { listOfNotNull(formatDate(it), b.endMinuteOfDay?.let { m -> minuteLabel(m) }).joinToString(" ") }
            SmallRow(
                tripBookingTypeIcon(b.type),
                b.title,
                listOf(
                    listOfNotNull(tripBookingTypeLabel(b.type), b.provider).joinToString(" · "),
                    listOfNotNull(start, end).joinToString(" → "),
                    listOfNotNull(b.place.label, b.arrivalPlace.name).joinToString(" → "),
                    b.confirmationCode?.let { stringResource(R.string.trip_confirmation_line, it) }.orEmpty(),
                ),
                onClick = { onClick(b) },
            )
        }
    }
}

@Composable
private fun PackingList(
    tripId: Long,
    items: List<TripPackingItem>,
    onToggle: (TripPackingItem, Boolean) -> Unit,
    onClick: (TripPackingItem) -> Unit,
    onUnpackAll: () -> Unit,
    onAddStarter: (List<TripPackingItem>) -> Unit,
) {
    // "CATEGORY:Name" entries, translated per locale.
    val starter = stringArrayResource(R.array.trip_starter_items).mapNotNull { entry ->
        val category = TripPackingCategory.entries.firstOrNull { entry.startsWith(it.name + ":") } ?: return@mapNotNull null
        TripPackingItem(tripId = tripId, name = entry.substringAfter(":").trim(), category = category)
    }
    val packed = items.count { it.isPacked }
    if (items.isNotEmpty()) {
        Text(stringResource(R.string.trip_packed_progress, packed, items.size), style = MaterialTheme.typography.titleSmall)
        LinearProgressIndicator(progress = { packed.toFloat() / items.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    } else {
        EmptyHint(stringResource(R.string.trip_packing_empty))
    }
    Row {
        OutlinedButton(onClick = { onAddStarter(starter) }) { Text(stringResource(R.string.trip_packing_starter)) }
        if (packed > 0) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onUnpackAll) { Text(stringResource(R.string.trip_packing_unpack_all)) }
        }
    }
    items.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }).forEach { (category, list) ->
        Text(
            tripPackingCategoryLabel(category),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        GroupedCard {
            list.sortedBy { it.isPacked }.forEach { item ->
                SmallRow(
                    tripPackingCategoryIcon(category),
                    if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                    emptyList(),
                    onClick = { onClick(item) },
                    leading = { Checkbox(checked = item.isPacked, onCheckedChange = { onToggle(item, it) }) },
                )
            }
        }
    }
}

@Composable
private fun Budget(trip: Trip, expenses: List<TripExpense>, today: Long, onClick: (TripExpense) -> Unit) {
    val summary = budgetSummary(trip, expenses, today)
    val money = { minor: Long -> formatMoney(minor, trip.currency) }
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                summary.budgetMinor?.let { stringResource(R.string.trips_card_spent_of, money(summary.spentMinor), money(it)) }
                    ?: stringResource(R.string.trips_card_spent, money(summary.spentMinor)),
                style = MaterialTheme.typography.titleMedium,
            )
            summary.usedFraction?.let { fraction ->
                val over = (summary.remainingMinor ?: 0) < 0
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Text(
                    if (over) stringResource(R.string.trip_budget_over, money(-summary.remainingMinor!!))
                    else stringResource(R.string.trip_budget_left, money(summary.remainingMinor!!)),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (summary.spentMinor > 0) {
                Text(stringResource(R.string.trip_budget_per_day, money(summary.perDayMinor)), style = MaterialTheme.typography.bodySmall)
            }
            summary.byCategory.forEach { (category, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(tripExpenseCategoryIcon(category), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tripExpenseCategoryLabel(category), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text(money(amount), style = MaterialTheme.typography.bodySmall)
                }
                LinearProgressIndicator(
                    progress = { amount.toFloat() / summary.spentMinor },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            }
        }
    }
    if (expenses.isEmpty()) {
        EmptyHint(stringResource(R.string.trip_expenses_empty))
        return
    }
    expenses.groupBy { it.date }.forEach { (day, list) ->
        Text(
            "${dayHeader(day)} · ${money(list.sumOf { it.amountMinor })}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        GroupedCard {
            list.forEach { e ->
                SmallRow(
                    tripExpenseCategoryIcon(e.category), e.title,
                    listOfNotNull(tripExpenseCategoryLabel(e.category), e.notes),
                    onClick = { onClick(e) }, trailing = money(e.amountMinor),
                )
            }
        }
    }
}
