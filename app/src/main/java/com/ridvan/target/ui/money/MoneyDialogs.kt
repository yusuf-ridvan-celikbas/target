package com.ridvan.target.ui.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.MoneyBudget
import com.ridvan.target.data.local.entity.MoneyCategory
import com.ridvan.target.data.local.entity.MoneyEntry
import com.ridvan.target.data.local.entity.MoneyGoal
import com.ridvan.target.data.local.entity.MoneyKind
import com.ridvan.target.data.local.entity.MoneyRecurring
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.ui.common.FormDialogProperties
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.focustimer.localDayToUtcMillis
import com.ridvan.target.ui.focustimer.utcMillisToLocalDay
import com.ridvan.target.ui.trips.CurrencyField
import com.ridvan.target.ui.trips.amountInputText
import com.ridvan.target.ui.trips.formatMoney
import com.ridvan.target.ui.trips.parseAmountMinor

@Composable
private fun AmountField(text: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = text.isNotBlank() && parseAmountMinor(text) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun KindToggle(kind: MoneyKind, onChange: (MoneyKind) -> Unit) {
    SegmentedToggle(
        options = listOf(
            SegmentedToggleOption(MoneyKind.EXPENSE, stringResource(R.string.money_kind_expense)),
            SegmentedToggleOption(MoneyKind.INCOME, stringResource(R.string.money_kind_income)),
        ),
        selected = kind,
        onSelect = onChange,
    )
}

@Composable
private fun CategoryField(categories: List<MoneyCategory>, kind: MoneyKind, selectedId: Long?, onSelect: (Long?) -> Unit) {
    val options = categories.filter { it.kind == kind && (!it.isHidden || it.id == selectedId) }
    DropdownField(
        selectedLabel = categoryLabel(options.firstOrNull { it.id == selectedId }),
        label = stringResource(R.string.money_field_category),
        modifier = Modifier.padding(top = 8.dp),
    ) { close ->
        options.forEach { c -> DropdownMenuItem(text = { Text(categoryLabel(c)) }, onClick = { onSelect(c.id); close() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayField(label: String, day: Long?, onChange: (Long?) -> Unit, allowClear: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text(day?.let { "$label: ${formatDate(it)}" } ?: label)
        }
        if (allowClear && day != null) TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.money_clear)) }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localDayToUtcMillis(day ?: startOfTodayMillis()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(utcMillisToLocalDay(it)) }
                    open = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** New or edited income/expense: amount, category, day, note. */
@Composable
fun EntryDialog(
    entry: MoneyEntry?,
    initialKind: MoneyKind,
    categories: List<MoneyCategory>,
    currency: String,
    onSave: (kind: MoneyKind, amountMinor: Long, categoryId: Long?, note: String, date: Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }
    var amount by remember { mutableStateOf(entry?.let { amountInputText(it.amountMinor) }.orEmpty()) }
    var categoryId by remember { mutableStateOf(entry?.categoryId) }
    var note by remember { mutableStateOf(entry?.note.orEmpty()) }
    var date by remember { mutableStateOf(entry?.date ?: startOfTodayMillis()) }
    val parsed = parseAmountMinor(amount)?.takeIf { it > 0 }
    // A category of the other kind doesn't fit after switching.
    if (categoryId != null && categories.none { it.id == categoryId && it.kind == kind }) categoryId = null

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (entry == null) R.string.money_add_entry else R.string.money_edit_entry)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                KindToggle(kind) { kind = it }
                AmountField(amount, { amount = it }, stringResource(R.string.money_field_amount, currency), Modifier.padding(top = 8.dp))
                CategoryField(categories, kind, categoryId) { categoryId = it }
                DayField(stringResource(R.string.money_field_date), date, { it?.let { d -> date = d } })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.money_field_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onSave(kind, it, categoryId, note, date) } }, enabled = parsed != null) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Monthly budgets: an optional total and one per expense category (blank = none). */
@Composable
fun BudgetsDialog(
    categories: List<MoneyCategory>,
    budgets: List<MoneyBudget>,
    onSave: (total: Long?, perCategory: Map<Long, Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var total by remember { mutableStateOf(budgets.firstOrNull { it.categoryId == null }?.let { amountInputText(it.amountMinor) }.orEmpty()) }
    val perCategory = remember {
        mutableStateMapOf<Long, String>().apply {
            categories.forEach { c -> put(c.id, budgets.firstOrNull { it.categoryId == c.id }?.let { amountInputText(it.amountMinor) }.orEmpty()) }
        }
    }
    val valid = (listOf(total) + perCategory.values).all { it.isBlank() || parseAmountMinor(it) != null }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_set_budgets)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.money_budgets_hint), style = MaterialTheme.typography.bodySmall)
                AmountField(total, { total = it }, stringResource(R.string.money_budget_total), Modifier.padding(top = 8.dp))
                categories.forEach { c ->
                    AmountField(perCategory[c.id].orEmpty(), { perCategory[c.id] = it }, categoryLabel(c), Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        parseAmountMinor(total)?.takeIf { it > 0 },
                        perCategory.mapNotNull { (id, text) -> parseAmountMinor(text)?.takeIf { it > 0 }?.let { id to it } }.toMap(),
                    )
                },
                enabled = valid,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Monthly income/expense: amount, category, day of month, note, active. */
@Composable
fun RecurringDialog(
    existing: MoneyRecurring?,
    categories: List<MoneyCategory>,
    onSave: (kind: MoneyKind, amountMinor: Long, categoryId: Long?, note: String, dayOfMonth: Int, isActive: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(existing?.kind ?: MoneyKind.EXPENSE) }
    var amount by remember { mutableStateOf(existing?.let { amountInputText(it.amountMinor) }.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var dayText by remember { mutableStateOf((existing?.dayOfMonth ?: 1).toString()) }
    var active by remember { mutableStateOf(existing?.isActive ?: true) }
    val parsed = parseAmountMinor(amount)?.takeIf { it > 0 }
    val day = dayText.toIntOrNull()?.takeIf { it in 1..31 }
    if (categoryId != null && categories.none { it.id == categoryId && it.kind == kind }) categoryId = null

    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.money_add_recurring else R.string.money_edit_recurring)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                KindToggle(kind) { kind = it }
                AmountField(amount, { amount = it }, stringResource(R.string.money_field_amount_plain), Modifier.padding(top = 8.dp))
                CategoryField(categories, kind, categoryId) { categoryId = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.money_field_recurring_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { input -> if (input.length <= 2 && input.all(Char::isDigit)) dayText = input },
                    label = { Text(stringResource(R.string.money_field_day_of_month)) },
                    isError = day == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(stringResource(R.string.money_recurring_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.money_recurring_active), modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (parsed != null && day != null) onSave(kind, parsed, categoryId, note, day, active) },
                enabled = parsed != null && day != null,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
fun GoalDialog(
    existing: MoneyGoal?,
    onSave: (name: String, targetMinor: Long, deadline: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var target by remember { mutableStateOf(existing?.let { amountInputText(it.targetMinor) }.orEmpty()) }
    var deadline by remember { mutableStateOf(existing?.deadline) }
    val parsed = parseAmountMinor(target)?.takeIf { it > 0 }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.money_add_goal else R.string.money_edit_goal)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(target, { target = it }, stringResource(R.string.money_field_goal_target), Modifier.padding(top = 8.dp))
                DayField(stringResource(R.string.money_field_goal_deadline), deadline, { deadline = it }, allowClear = true)
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onSave(name, it, deadline) } }, enabled = parsed != null && name.isNotBlank()) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Put money into a goal (or take some out), or open its edit form. */
@Composable
fun GoalAmountDialog(
    goal: MoneyGoal,
    currency: String,
    onAdd: (Long) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    val parsed = parseAmountMinor(amount)?.takeIf { it > 0 }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(goal.name) },
        text = {
            Column(modifier = Modifier.imePadding()) {
                Text(stringResource(R.string.money_spent_of, formatMoney(goal.savedMinor, currency), formatMoney(goal.targetMinor, currency)))
                AmountField(amount, { amount = it }, stringResource(R.string.money_field_amount, currency), Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { parsed?.let(onAdd) }, enabled = parsed != null) { Text(stringResource(R.string.money_goal_add)) }
                    TextButton(onClick = { parsed?.let { onAdd(-it) } }, enabled = parsed != null) { Text(stringResource(R.string.money_goal_take_out)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text(stringResource(R.string.money_edit_goal)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.money_close)) } },
    )
}

/** Rename, hide/show and add categories, expenses and income separately. */
@Composable
fun CategoriesDialog(
    categories: List<MoneyCategory>,
    onAdd: (MoneyKind, String) -> Unit,
    onRename: (MoneyCategory, String) -> Unit,
    onSetHidden: (MoneyCategory, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(MoneyKind.EXPENSE) }
    var newName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<MoneyCategory?>(null) }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_categories)) },
        text = {
            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                KindToggle(kind) { kind = it }
                categories.filter { it.kind == kind }.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            categoryLabel(c),
                            color = if (c.isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { renaming = c }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.money_rename)) }
                        IconButton(onClick = { onSetHidden(c, !c.isHidden) }) {
                            Icon(
                                if (c.isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(if (c.isHidden) R.string.money_show else R.string.money_hide),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.money_new_category)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onAdd(kind, newName); newName = "" }, enabled = newName.isNotBlank()) {
                        Text(stringResource(R.string.common_add))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.money_close)) } },
    )
    renaming?.let { c ->
        val current = categoryLabel(c)
        var text by remember(c.id) { mutableStateOf(current) }
        AlertDialog(
            properties = FormDialogProperties,
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.money_rename)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.width(280.dp))
            },
            confirmButton = {
                TextButton(onClick = { onRename(c, text); renaming = null }, enabled = text.isNotBlank()) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
fun CurrencyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf(current) }
    AlertDialog(
        properties = FormDialogProperties,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.money_currency)) },
        text = {
            Column {
                Text(stringResource(R.string.money_currency_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                CurrencyField(code) { code = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(code) }) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
