package com.ridvan.target.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripActivityCategory
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripExpenseCategory
import com.ridvan.target.data.local.entity.TripPackingCategory
import com.ridvan.target.data.local.entity.TripPackingItem
import com.ridvan.target.data.local.entity.TripPlace
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.focustimer.DropdownField
import com.ridvan.target.ui.focustimer.localDayToUtcMillis
import com.ridvan.target.ui.focustimer.utcMillisToLocalDay

/*
 * Add/edit dialogs for a trip and everything in it. Each returns an unsaved entity (id kept when editing);
 * the ViewModel decides insert vs update by id. A lookup service could later produce the same entities
 * to pre-fill these via `initial` (data/trips/TripLookups.kt).
 */

private fun String.orNullIfBlank(): String? = trim().takeIf { it.isNotEmpty() }

/** Shared chrome: scrollable body, Save (enabled by [canSave]) / Cancel, optional red Delete action. */
@Composable
private fun TripFormDialog(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                content()
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, label: String, decimal: Boolean = false, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = if (decimal) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun <T> EnumPicker(
    label: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: @Composable (T) -> String,
    iconOf: (T) -> ImageVector,
) {
    FieldLabel(label)
    DropdownField(selectedLabel = labelOf(selected)) { close ->
        values.forEach { value ->
            DropdownMenuItem(
                leadingIcon = { Icon(iconOf(value), contentDescription = null) },
                text = { Text(labelOf(value)) },
                onClick = {
                    onSelect(value)
                    close()
                },
            )
        }
    }
}

/** A date button opening Material3's DatePicker, optionally limited to [range] (inclusive day starts). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayField(label: String, day: Long, onDayChange: (Long) -> Unit, range: LongRange? = null) {
    var open by remember { mutableStateOf(false) }
    FieldLabel(label)
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(formatDate(day)) }
    if (open) {
        val utcRange = range?.let { localDayToUtcMillis(it.first)..localDayToUtcMillis(it.last) }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localDayToUtcMillis(day),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcRange == null || utcTimeMillis in utcRange
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDayChange(utcMillisToLocalDay(it)) }
                    open = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** A "Set a time" switch revealing a 24-hour TimeInput. Read the result with [minuteOrNull]. */
@OptIn(ExperimentalMaterial3Api::class)
private class OptionalTime(val enabled: androidx.compose.runtime.MutableState<Boolean>, val state: TimePickerState) {
    fun minuteOrNull(): Int? = if (enabled.value) state.hour * 60 + state.minute else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberOptionalTime(minuteOfDay: Int?, defaultMinute: Int = 9 * 60): OptionalTime {
    val enabled = remember { mutableStateOf(minuteOfDay != null) }
    val start = minuteOfDay ?: defaultMinute
    val state = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
    return remember(state) { OptionalTime(enabled, state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalTimeField(label: String, time: OptionalTime) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = time.enabled.value, onCheckedChange = { time.enabled.value = it })
    }
    if (time.enabled.value) TimeInput(state = time.state)
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(top = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TripDialog(initial: Trip?, onConfirm: (Trip) -> Unit, onDismiss: () -> Unit) {
    val today = remember { startOfTodayMillis() }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var destination by remember { mutableStateOf(initial?.destination.orEmpty()) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: today) }
    var endDate by remember { mutableStateOf(initial?.endDate ?: addDays(today, 2)) }
    var currency by remember { mutableStateOf(initial?.currency ?: defaultCurrencyCode()) }
    var budget by remember { mutableStateOf(initial?.budgetMinor?.let { amountInputText(it) }.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    val currencyCode = normalizedCurrencyCode(currency)
    val budgetMinor = budget.orNullIfBlank()?.let { parseAmountMinor(it) }
    val budgetValid = budget.isBlank() || budgetMinor != null
    TripFormDialog(
        title = stringResource(if (initial == null) R.string.trip_add_title else R.string.trip_edit_title),
        canSave = name.isNotBlank() && currencyCode != null && budgetValid,
        onSave = {
            val base = initial ?: Trip(name = "", startDate = startDate, endDate = endDate, currency = currencyCode!!)
            onConfirm(
                base.copy(
                    name = name.trim(),
                    destination = destination.orNullIfBlank(),
                    startDate = startDate,
                    endDate = endDate,
                    currency = currencyCode!!,
                    budgetMinor = budgetMinor,
                    notes = notes.orNullIfBlank(),
                )
            )
        },
        onDismiss = onDismiss,
    ) {
        TextInput(name, { name = it }, stringResource(R.string.trip_field_name))
        TextInput(destination, { destination = it }, stringResource(R.string.trip_field_destination))
        DayField(stringResource(R.string.trip_field_start), startDate, {
            startDate = it
            if (endDate < it) endDate = it
        })
        DayField(stringResource(R.string.trip_field_end), endDate, { endDate = it }, range = startDate..addDays(startDate, 365))
        TextInput(currency, { currency = it.take(3).uppercase() }, stringResource(R.string.trip_field_currency))
        if (currencyCode == null) {
            Text(stringResource(R.string.trip_currency_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        TextInput(budget, { budget = it }, stringResource(R.string.trip_field_budget, currencyCode ?: currency), decimal = true)
        TextInput(notes, { notes = it }, stringResource(R.string.trip_field_notes), singleLine = false)
    }
}

@Composable
fun TripActivityDialog(
    trip: Trip,
    initial: TripActivity,
    onConfirm: (TripActivity) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initial.title) }
    var category by remember { mutableStateOf(initial.category) }
    var date by remember { mutableStateOf(initial.date) }
    val start = rememberOptionalTime(initial.startMinuteOfDay)
    val end = rememberOptionalTime(
        initial.startMinuteOfDay?.let { s -> initial.durationMinutes?.let { (s + it) % (24 * 60) } },
        defaultMinute = 10 * 60,
    )
    var placeName by remember { mutableStateOf(initial.place.name.orEmpty()) }
    var address by remember { mutableStateOf(initial.place.address.orEmpty()) }
    var notes by remember { mutableStateOf(initial.notes.orEmpty()) }

    TripFormDialog(
        title = stringResource(if (initial.id == 0L) R.string.trip_activity_add_title else R.string.trip_activity_edit_title),
        canSave = title.isNotBlank(),
        onSave = {
            val startMinute = start.minuteOrNull()
            val endMinute = end.minuteOrNull()
            onConfirm(
                initial.copy(
                    title = title.trim(),
                    category = category,
                    date = date,
                    startMinuteOfDay = startMinute,
                    durationMinutes = if (startMinute != null && endMinute != null) minutesBetween(startMinute, endMinute) else null,
                    place = initial.place.copy(name = placeName.orNullIfBlank(), address = address.orNullIfBlank()),
                    notes = notes.orNullIfBlank(),
                )
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete,
    ) {
        TextInput(title, { title = it }, stringResource(R.string.trip_field_title))
        EnumPicker(
            stringResource(R.string.trip_field_category), TripActivityCategory.entries, category, { category = it },
            { tripActivityCategoryLabel(it) }, ::tripActivityCategoryIcon,
        )
        DayField(stringResource(R.string.trip_field_day), date, { date = it }, range = trip.startDate..trip.endDate)
        OptionalTimeField(stringResource(R.string.trip_field_start_time), start)
        if (start.enabled.value) OptionalTimeField(stringResource(R.string.trip_field_end_time), end)
        TextInput(placeName, { placeName = it }, stringResource(R.string.trip_field_place))
        TextInput(address, { address = it }, stringResource(R.string.trip_field_address))
        TextInput(notes, { notes = it }, stringResource(R.string.trip_field_notes), singleLine = false)
    }
}

/** [onConfirm] gets the booking plus, when "Log the price as an expense" was filled, that expense's amount. */
@Composable
fun TripBookingDialog(
    trip: Trip,
    initial: TripBooking,
    onConfirm: (TripBooking, priceMinor: Long?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var type by remember { mutableStateOf(initial.type) }
    var title by remember { mutableStateOf(initial.title) }
    var provider by remember { mutableStateOf(initial.provider.orEmpty()) }
    var code by remember { mutableStateOf(initial.confirmationCode.orEmpty()) }
    var startDate by remember { mutableStateOf(initial.startDate) }
    val startTime = rememberOptionalTime(initial.startMinuteOfDay)
    var hasEnd by remember { mutableStateOf(initial.endDate != null) }
    var endDate by remember { mutableStateOf(initial.endDate ?: initial.startDate) }
    val endTime = rememberOptionalTime(initial.endMinuteOfDay, defaultMinute = 12 * 60)
    var placeName by remember { mutableStateOf(initial.place.name.orEmpty()) }
    var address by remember { mutableStateOf(initial.place.address.orEmpty()) }
    var arrival by remember { mutableStateOf(initial.arrivalPlace.name.orEmpty()) }
    var notes by remember { mutableStateOf(initial.notes.orEmpty()) }
    var price by remember { mutableStateOf("") }

    val priceMinor = price.orNullIfBlank()?.let { parseAmountMinor(it) }
    val isTransport = type.isTransport
    val isStay = type == TripBookingType.HOTEL || type == TripBookingType.CAR_RENTAL
    TripFormDialog(
        title = stringResource(if (initial.id == 0L) R.string.trip_booking_add_title else R.string.trip_booking_edit_title),
        canSave = title.isNotBlank() && (price.isBlank() || priceMinor != null),
        onSave = {
            onConfirm(
                initial.copy(
                    type = type,
                    title = title.trim(),
                    provider = provider.orNullIfBlank(),
                    confirmationCode = code.orNullIfBlank(),
                    startDate = startDate,
                    startMinuteOfDay = startTime.minuteOrNull(),
                    endDate = if (hasEnd) maxOf(endDate, startDate) else null,
                    endMinuteOfDay = if (hasEnd) endTime.minuteOrNull() else null,
                    place = initial.place.copy(name = placeName.orNullIfBlank(), address = address.orNullIfBlank()),
                    arrivalPlace = if (isTransport) initial.arrivalPlace.copy(name = arrival.orNullIfBlank()) else TripPlace(),
                    notes = notes.orNullIfBlank(),
                ),
                priceMinor,
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete,
    ) {
        EnumPicker(
            stringResource(R.string.trip_field_type), TripBookingType.entries, type, { type = it },
            { tripBookingTypeLabel(it) }, ::tripBookingTypeIcon,
        )
        TextInput(title, { title = it }, stringResource(if (isTransport) R.string.trip_field_booking_number else R.string.trip_field_title))
        TextInput(provider, { provider = it }, stringResource(R.string.trip_field_provider))
        TextInput(code, { code = it }, stringResource(R.string.trip_field_confirmation))
        val range = addDays(trip.startDate, -30)..addDays(trip.endDate, 30)
        DayField(
            stringResource(if (isStay) R.string.trip_field_check_in else R.string.trip_field_departure),
            startDate, { startDate = it }, range = range,
        )
        OptionalTimeField(stringResource(R.string.trip_field_set_time), startTime)
        TextInput(placeName, { placeName = it }, stringResource(if (isTransport) R.string.trip_field_from else R.string.trip_field_place))
        if (isTransport) TextInput(arrival, { arrival = it }, stringResource(R.string.trip_field_to))
        TextInput(address, { address = it }, stringResource(R.string.trip_field_address))
        CheckRow(stringResource(if (isStay) R.string.trip_field_has_check_out else R.string.trip_field_has_arrival), hasEnd) { hasEnd = it }
        if (hasEnd) {
            DayField(
                stringResource(if (isStay) R.string.trip_field_check_out else R.string.trip_field_arrival),
                endDate, { endDate = it }, range = startDate..addDays(trip.endDate, 30),
            )
            OptionalTimeField(stringResource(R.string.trip_field_set_time), endTime)
        }
        TextInput(notes, { notes = it }, stringResource(R.string.trip_field_notes), singleLine = false)
        if (initial.id == 0L) {
            TextInput(price, { price = it }, stringResource(R.string.trip_field_booking_price, trip.currency), decimal = true)
            Text(stringResource(R.string.trip_booking_price_hint), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TripPackingItemDialog(
    initial: TripPackingItem,
    onConfirm: (TripPackingItem) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var quantity by remember { mutableStateOf(initial.quantity.toString()) }
    TripFormDialog(
        title = stringResource(if (initial.id == 0L) R.string.trip_packing_add_title else R.string.trip_packing_edit_title),
        canSave = name.isNotBlank(),
        onSave = {
            onConfirm(initial.copy(name = name.trim(), category = category, quantity = quantity.toIntOrNull()?.coerceIn(1, 999) ?: 1))
        },
        onDismiss = onDismiss,
        onDelete = onDelete,
    ) {
        TextInput(name, { name = it }, stringResource(R.string.trip_field_item))
        EnumPicker(
            stringResource(R.string.trip_field_category), TripPackingCategory.entries, category, { category = it },
            { tripPackingCategoryLabel(it) }, ::tripPackingCategoryIcon,
        )
        TextInput(quantity, { quantity = it.filter(Char::isDigit).take(3) }, stringResource(R.string.trip_field_quantity), decimal = true)
    }
}

@Composable
fun TripExpenseDialog(
    trip: Trip,
    initial: TripExpense,
    onConfirm: (TripExpense) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initial.title) }
    var amount by remember { mutableStateOf(if (initial.id == 0L) "" else amountInputText(initial.amountMinor)) }
    var category by remember { mutableStateOf(initial.category) }
    var date by remember { mutableStateOf(initial.date) }
    var notes by remember { mutableStateOf(initial.notes.orEmpty()) }
    val amountMinor = parseAmountMinor(amount)
    TripFormDialog(
        title = stringResource(if (initial.id == 0L) R.string.trip_expense_add_title else R.string.trip_expense_edit_title),
        canSave = title.isNotBlank() && amountMinor != null,
        onSave = {
            onConfirm(initial.copy(title = title.trim(), amountMinor = amountMinor!!, category = category, date = date, notes = notes.orNullIfBlank()))
        },
        onDismiss = onDismiss,
        onDelete = onDelete,
    ) {
        TextInput(title, { title = it }, stringResource(R.string.trip_field_title))
        TextInput(amount, { amount = it }, stringResource(R.string.trip_field_amount, trip.currency), decimal = true)
        EnumPicker(
            stringResource(R.string.trip_field_category), TripExpenseCategory.entries, category, { category = it },
            { tripExpenseCategoryLabel(it) }, ::tripExpenseCategoryIcon,
        )
        DayField(stringResource(R.string.trip_field_day), date, { date = it }, range = addDays(trip.startDate, -60)..addDays(trip.endDate, 30))
        TextInput(notes, { notes = it }, stringResource(R.string.trip_field_notes), singleLine = false)
    }
}
