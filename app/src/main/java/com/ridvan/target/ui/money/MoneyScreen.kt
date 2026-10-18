package com.ridvan.target.ui.money

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.MoneyCategory
import com.ridvan.target.data.local.entity.MoneyEntry
import com.ridvan.target.data.local.entity.MoneyGoal
import com.ridvan.target.data.local.entity.MoneyKind
import com.ridvan.target.data.local.entity.MoneyRecurring
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import com.ridvan.target.ui.trips.CurrencyField
import com.ridvan.target.ui.trips.defaultCurrencyCode
import com.ridvan.target.ui.trips.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MoneySection { OVERVIEW, ENTRIES, PLANS }

/** What the entry form is open for: a new entry of [kind], or editing [entry]. */
private data class EntryDraft(val entry: MoneyEntry?, val kind: MoneyKind)

@Composable
fun MoneyScreen(
    shellNavigation: ShellNavigation,
    onTripClick: (Long) -> Unit,
    viewModel: MoneyViewModel = viewModel(),
) {
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    var section by rememberSaveable { mutableStateOf(MoneySection.OVERVIEW) }
    var menuOpen by remember { mutableStateOf(false) }
    var entryDraft by remember { mutableStateOf<EntryDraft?>(null) }
    var editingBudgets by remember { mutableStateOf(false) }
    var recurringDraft by remember { mutableStateOf<MoneyRecurring?>(null) }
    var addingRecurring by remember { mutableStateOf(false) }
    var goalDraft by remember { mutableStateOf<MoneyGoal?>(null) }
    var addingGoal by remember { mutableStateOf(false) }
    var goalActions by remember { mutableStateOf<MoneyGoal?>(null) }
    var managingCategories by remember { mutableStateOf(false) }
    var changingCurrency by remember { mutableStateOf(false) }

    LaunchedEffect(currency) {
        if (currency != null && viewModel.consumeAddExpense()) entryDraft = EntryDraft(null, MoneyKind.EXPENSE)
    }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.MONEY,
        title = stringResource(R.string.label_money),
        floatingActionButton = { if (currency != null) AddFab(onClick = { entryDraft = EntryDraft(null, MoneyKind.EXPENSE) }) },
        actions = {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more)) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.money_categories)) }, onClick = { menuOpen = false; managingCategories = true })
                DropdownMenuItem(text = { Text(stringResource(R.string.money_currency)) }, onClick = { menuOpen = false; changingCurrency = true })
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            val code = currency
            if (code == null) {
                CurrencySetup(onSave = viewModel::setCurrency)
                return@Column
            }
            MonthSwitcher(month.monthKey, onShift = viewModel::shiftMonth)
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(MoneySection.OVERVIEW, stringResource(R.string.money_section_overview)),
                    SegmentedToggleOption(MoneySection.ENTRIES, stringResource(R.string.money_section_entries)),
                    SegmentedToggleOption(MoneySection.PLANS, stringResource(R.string.money_section_plans)),
                ),
                selected = section,
                onSelect = { section = it },
            )
            when (section) {
                MoneySection.OVERVIEW -> {
                    SummaryCard(month, code)
                    SpendingCard(month, categories, code, onEditBudgets = { editingBudgets = true })
                    if (month.incomeByCategory.isNotEmpty()) IncomeCard(month, categories, code)
                    MonthsChart(month.lastMonths, code)
                    if (month.tripsLeftOut > 0) {
                        Text(stringResource(R.string.money_trips_left_out, month.tripsLeftOut), style = MaterialTheme.typography.labelSmall)
                    }
                }
                MoneySection.ENTRIES -> EntriesList(
                    month.rows, categories, code,
                    onEntryClick = { entryDraft = EntryDraft(it, it.kind) },
                    onTripClick = onTripClick,
                )
                MoneySection.PLANS -> {
                    RecurringCard(recurring, categories, code, onAdd = { addingRecurring = true }, onClick = { recurringDraft = it })
                    GoalsCard(goals, code, onAdd = { addingGoal = true }, onClick = { goalActions = it })
                }
            }
        }
    }

    val code = currency ?: return
    entryDraft?.let { draft ->
        EntryDialog(
            entry = draft.entry,
            initialKind = draft.kind,
            categories = categories,
            currency = code,
            onSave = { kind, amount, categoryId, note, date ->
                viewModel.saveEntry(draft.entry, kind, amount, categoryId, note, date)
                entryDraft = null
            },
            onDelete = draft.entry?.let { e -> { viewModel.deleteEntry(e); entryDraft = null } },
            onDismiss = { entryDraft = null },
        )
    }
    if (editingBudgets) {
        BudgetsDialog(
            categories = categories.filter { it.kind == MoneyKind.EXPENSE && !it.isHidden },
            budgets = budgets,
            onSave = { total, perCategory -> viewModel.saveBudgets(total, perCategory); editingBudgets = false },
            onDismiss = { editingBudgets = false },
        )
    }
    if (addingRecurring || recurringDraft != null) {
        RecurringDialog(
            existing = recurringDraft,
            categories = categories,
            onSave = { kind, amount, categoryId, note, day, active ->
                viewModel.saveRecurring(recurringDraft, kind, amount, categoryId, note, day, active)
                addingRecurring = false; recurringDraft = null
            },
            onDelete = recurringDraft?.let { r -> { viewModel.deleteRecurring(r); recurringDraft = null } },
            onDismiss = { addingRecurring = false; recurringDraft = null },
        )
    }
    if (addingGoal || goalDraft != null) {
        GoalDialog(
            existing = goalDraft,
            onSave = { name, target, deadline ->
                viewModel.saveGoal(goalDraft, name, target, deadline)
                addingGoal = false; goalDraft = null
            },
            onDelete = goalDraft?.let { g -> { viewModel.deleteGoal(g); goalDraft = null } },
            onDismiss = { addingGoal = false; goalDraft = null },
        )
    }
    goalActions?.let { goal ->
        GoalAmountDialog(
            goal = goal,
            currency = code,
            onAdd = { viewModel.addToGoal(goal, it); goalActions = null },
            onEdit = { goalActions = null; goalDraft = goal },
            onDismiss = { goalActions = null },
        )
    }
    if (managingCategories) {
        CategoriesDialog(
            categories = categories,
            onAdd = viewModel::addCategory,
            onRename = viewModel::renameCategory,
            onSetHidden = viewModel::setCategoryHidden,
            onDismiss = { managingCategories = false },
        )
    }
    if (changingCurrency) {
        CurrencyDialog(current = code, onSave = { viewModel.setCurrency(it); changingCurrency = false }, onDismiss = { changingCurrency = false })
    }
}

/** A category's display name: its own name, else the translated default, else "Uncategorized". */
@Composable
fun categoryLabel(category: MoneyCategory?): String {
    category?.name?.let { return it }
    val res = when (category?.systemKey) {
        "FOOD" -> R.string.money_cat_food
        "TRANSPORT" -> R.string.money_cat_transport
        "BILLS" -> R.string.money_cat_bills
        "RENT" -> R.string.money_cat_rent
        "SHOPPING" -> R.string.money_cat_shopping
        "HEALTH" -> R.string.money_cat_health
        "EDUCATION" -> R.string.money_cat_education
        "FUN" -> R.string.money_cat_fun
        "TRAVEL" -> R.string.money_cat_travel
        "OTHER" -> R.string.money_cat_other
        "SALARY" -> R.string.money_cat_salary
        "ALLOWANCE" -> R.string.money_cat_allowance
        "OTHER_INCOME" -> R.string.money_cat_other_income
        else -> R.string.money_uncategorized
    }
    return stringResource(res)
}

fun monthLabel(monthKey: Int): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(monthStart(monthKey)))

@Composable
private fun CurrencySetup(onSave: (String) -> Unit) {
    var code by remember { mutableStateOf(defaultCurrencyCode()) }
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.money_setup_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.money_setup_hint), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            CurrencyField(code) { code = it }
            Button(onClick = { onSave(code) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@Composable
private fun MonthSwitcher(monthKey: Int, onShift: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.money_prev_month))
        }
        Text(monthLabel(monthKey), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.money_next_month))
        }
    }
}

@Composable
private fun SummaryCard(month: MoneyMonth, code: String) {
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Figure(stringResource(R.string.money_income), formatMoney(month.incomeMinor, code), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                Figure(stringResource(R.string.money_expenses), formatMoney(month.expenseMinor, code), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                Figure(stringResource(R.string.money_net), formatMoney(month.netMinor, code), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }
            month.total?.let { total ->
                LinearProgressIndicator(
                    progress = { total.fraction.coerceAtMost(1f) },
                    color = if (total.leftMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Text(
                    if (total.leftMinor >= 0) stringResource(R.string.money_budget_left, formatMoney(total.leftMinor, code), formatMoney(total.budgetMinor, code))
                    else stringResource(R.string.money_budget_over, formatMoney(-total.leftMinor, code), formatMoney(total.budgetMinor, code)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Spending per category with a bar: against its budget when it has one, else against the biggest category. */
@Composable
private fun SpendingCard(month: MoneyMonth, categories: List<MoneyCategory>, code: String, onEditBudgets: () -> Unit) {
    val byId = categories.associateBy { it.id }
    val budgetById = month.budgets.associateBy { it.categoryId }
    val budgetOnly = month.budgets.filter { line -> month.spending.none { it.first == line.categoryId } }.map { it.categoryId to 0L }
    val rows = month.spending + budgetOnly
    val max = rows.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.money_spending_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditBudgets) { Text(stringResource(R.string.money_set_budgets)) }
            }
            if (rows.isEmpty()) Text(stringResource(R.string.money_no_spending), style = MaterialTheme.typography.bodyMedium)
            rows.forEach { (categoryId, spent) ->
                val budget = budgetById[categoryId]
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(categoryLabel(categoryId?.let { byId[it] }), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (budget != null) stringResource(R.string.money_spent_of, formatMoney(spent, code), formatMoney(budget.budgetMinor, code))
                        else formatMoney(spent, code),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (budget != null && budget.leftMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
                LinearProgressIndicator(
                    progress = { (budget?.fraction ?: (spent.toFloat() / max)).coerceAtMost(1f) },
                    color = if (budget != null && budget.leftMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun IncomeCard(month: MoneyMonth, categories: List<MoneyCategory>, code: String) {
    val byId = categories.associateBy { it.id }
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.money_income_title), style = MaterialTheme.typography.titleMedium)
            month.incomeByCategory.forEach { (categoryId, amount) ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(categoryLabel(categoryId?.let { byId[it] }), modifier = Modifier.weight(1f))
                    Text(formatMoney(amount, code), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Income vs expenses for this and the previous five months. */
@Composable
private fun MonthsChart(months: List<MonthTotals>, code: String) {
    if (months.all { it.incomeMinor == 0L && it.expenseMinor == 0L }) return
    val max = months.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1)
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.money_months_title), style = MaterialTheme.typography.titleMedium)
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
                val slot = size.width / months.size
                val bar = slot * 0.3f
                months.forEachIndexed { i, m ->
                    val x = i * slot + slot * 0.15f
                    val inH = size.height * (m.incomeMinor.toFloat() / max)
                    val exH = size.height * (m.expenseMinor.toFloat() / max)
                    drawRect(incomeColor, topLeft = Offset(x, size.height - inH), size = Size(bar, inH))
                    drawRect(expenseColor, topLeft = Offset(x + bar + slot * 0.1f, size.height - exH), size = Size(bar, exH))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                months.forEach { m ->
                    Text(
                        SimpleDateFormat("MMM", Locale.getDefault()).format(Date(monthStart(m.monthKey))),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val latest = months.last()
            Text(
                stringResource(R.string.money_months_legend, formatMoney(latest.incomeMinor, code), formatMoney(latest.expenseMinor, code)),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun EntriesList(
    rows: List<MoneyRow>,
    categories: List<MoneyCategory>,
    code: String,
    onEntryClick: (MoneyEntry) -> Unit,
    onTripClick: (Long) -> Unit,
) {
    if (rows.isEmpty()) {
        Text(stringResource(R.string.money_no_entries), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val byId = categories.associateBy { it.id }
    val travel = categories.firstOrNull { it.systemKey == TRAVEL_KEY && it.kind == MoneyKind.EXPENSE }
    rows.groupBy { it.date }.forEach { (day, list) ->
        Text(formatDate(day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        GroupedCard {
            list.forEachIndexed { i, row ->
                when (row) {
                    is MoneyRow.Entry -> {
                        val income = row.entry.kind == MoneyKind.INCOME
                        ListItem(
                            headlineContent = { Text(categoryLabel(row.entry.categoryId?.let { byId[it] })) },
                            supportingContent = row.entry.note?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                            trailingContent = {
                                Text(
                                    (if (income) "+" else "−") + formatMoney(row.amountMinor, code),
                                    color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onEntryClick(row.entry) },
                        )
                    }
                    is MoneyRow.Trip -> ListItem(
                        headlineContent = { Text(categoryLabel(travel)) },
                        supportingContent = { Text(stringResource(R.string.money_trip_row, row.expense.tripName, row.expense.expense.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = { Text("−" + formatMoney(row.amountMinor, code), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onTripClick(row.expense.expense.tripId) },
                    )
                }
                if (i < list.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecurringCard(
    items: List<MoneyRecurring>,
    categories: List<MoneyCategory>,
    code: String,
    onAdd: () -> Unit,
    onClick: (MoneyRecurring) -> Unit,
) {
    val byId = categories.associateBy { it.id }
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.money_recurring_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text(stringResource(R.string.common_add)) }
            }
            if (items.isEmpty()) {
                Text(stringResource(R.string.money_recurring_empty), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
            }
            items.forEach { r ->
                val income = r.kind == MoneyKind.INCOME
                ListItem(
                    headlineContent = { Text(r.note ?: categoryLabel(r.categoryId?.let { byId[it] })) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                stringResource(R.string.money_recurring_day, r.dayOfMonth),
                                categoryLabel(r.categoryId?.let { byId[it] }).takeIf { r.note != null },
                                stringResource(R.string.money_recurring_paused).takeIf { !r.isActive },
                            ).joinToString(" · ")
                        )
                    },
                    trailingContent = {
                        Text(
                            (if (income) "+" else "−") + formatMoney(r.amountMinor, code),
                            color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onClick(r) },
                )
            }
        }
    }
}

@Composable
private fun GoalsCard(goals: List<MoneyGoal>, code: String, onAdd: () -> Unit, onClick: (MoneyGoal) -> Unit) {
    val today = startOfTodayMillis()
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.money_goals_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text(stringResource(R.string.common_add)) }
            }
            if (goals.isEmpty()) {
                Text(stringResource(R.string.money_goals_empty), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
            }
            goals.forEach { g ->
                Column(modifier = Modifier.fillMaxWidth().clickable { onClick(g) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row {
                        Text(g.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.money_spent_of, formatMoney(g.savedMinor, code), formatMoney(g.targetMinor, code)))
                    }
                    LinearProgressIndicator(progress = { goalFraction(g.savedMinor, g.targetMinor) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    val perMonth = monthlyNeededForGoal(g.savedMinor, g.targetMinor, g.deadline, today)
                    val line = when {
                        g.savedMinor >= g.targetMinor -> stringResource(R.string.money_goal_reached)
                        g.deadline != null && perMonth != null ->
                            stringResource(R.string.money_goal_per_month, formatMoney(perMonth, code), formatDate(g.deadline))
                        g.deadline != null -> stringResource(R.string.money_goal_deadline, formatDate(g.deadline))
                        else -> null
                    }
                    line?.let { Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp)) }
                }
            }
        }
    }
}
