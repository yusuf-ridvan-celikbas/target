package com.ridvan.target.ui.money

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.TripExpenseForMoney
import com.ridvan.target.data.local.entity.MoneyBudget
import com.ridvan.target.data.local.entity.MoneyCategory
import com.ridvan.target.data.local.entity.MoneyEntry
import com.ridvan.target.data.local.entity.MoneyGoal
import com.ridvan.target.data.local.entity.MoneyKind
import com.ridvan.target.data.local.entity.MoneyRecurring
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.navigation.MoneyRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A row of the month's list: one of your entries, or a trip expense shown under Travel. */
sealed interface MoneyRow {
    val date: Long
    val amountMinor: Long

    data class Entry(val entry: MoneyEntry) : MoneyRow {
        override val date get() = entry.date
        override val amountMinor get() = entry.amountMinor
    }

    data class Trip(val expense: TripExpenseForMoney, override val amountMinor: Long) : MoneyRow {
        override val date get() = expense.expense.date
    }
}

data class MonthTotals(val monthKey: Int, val incomeMinor: Long, val expenseMinor: Long)

/** Everything the Money page shows for the chosen month. */
data class MoneyMonth(
    val monthKey: Int,
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    /** Overall budget line (explicit total or the sum of category budgets), if any. */
    val total: BudgetLine? = null,
    /** Expense categories with a budget, in category order. */
    val budgets: List<BudgetLine> = emptyList(),
    /** Spending per category (null = uncategorized), largest first. */
    val spending: List<Pair<Long?, Long>> = emptyList(),
    val incomeByCategory: List<Pair<Long?, Long>> = emptyList(),
    /** This and the 5 months before, oldest first. */
    val lastMonths: List<MonthTotals> = emptyList(),
    val rows: List<MoneyRow> = emptyList(),
    /** Trip expenses this month that couldn't be converted (other currency, no rate). */
    val tripsLeftOut: Int = 0,
) {
    val netMinor: Long get() = incomeMinor - expenseMinor
}

class MoneyViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val userId = app.preferences.currentUserId
    private val moneyDao = app.database.moneyDao()
    private val userDao = app.database.userDao()

    private var pendingAddExpense = savedStateHandle.toRoute<MoneyRoute>().addExpense &&
        savedStateHandle.get<Boolean>("addExpenseShown") != true
    private val handle = savedStateHandle

    /** True once when opened from a Home "Add expense" shortcut. */
    fun consumeAddExpense(): Boolean {
        if (!pendingAddExpense) return false
        pendingAddExpense = false
        handle["addExpenseShown"] = true
        return true
    }

    val currency: StateFlow<String?> = (userId?.let { userDao.getById(it) } ?: flowOf(null))
        .map { it?.moneyCurrency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _monthKey = MutableStateFlow(monthKeyOf(System.currentTimeMillis()))
    val monthKey: StateFlow<Int> = _monthKey.asStateFlow()

    val categories: StateFlow<List<MoneyCategory>> = (userId?.let { moneyDao.getCategories(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val entries = (userId?.let { moneyDao.getEntries(it) } ?: flowOf(emptyList()))
    val budgets: StateFlow<List<MoneyBudget>> = (userId?.let { moneyDao.getBudgets(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recurring: StateFlow<List<MoneyRecurring>> = (userId?.let { moneyDao.getRecurring(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals: StateFlow<List<MoneyGoal>> = (userId?.let { moneyDao.getGoals(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val tripExpenses = (userId?.let { moneyDao.getTripExpenses(it) } ?: flowOf(emptyList()))

    /** All rows (entries + convertible trip expenses), plus trip expenses that couldn't be converted. */
    private val allRows = combine(entries, tripExpenses, currency) { list, trips, code ->
        val tripRows = if (code == null) emptyList() else trips.map { t ->
            t to tripAmountInMoneyCurrency(t.expense.amountMinor, t.tripCurrency, t.homeCurrency, t.exchangeRate, code)
        }
        Triple(
            list.map { MoneyRow.Entry(it) } + tripRows.mapNotNull { (t, amount) -> amount?.let { MoneyRow.Trip(t, it) } },
            tripRows.filter { it.second == null }.map { it.first.expense.date },
            Unit,
        )
    }

    val month: StateFlow<MoneyMonth> = combine(allRows, categories, budgets, _monthKey) { (rows, leftOutDates, _), cats, budgetList, key ->
        val travelId = cats.firstOrNull { it.kind == MoneyKind.EXPENSE && it.systemKey == TRAVEL_KEY }?.id
        fun categoryOf(row: MoneyRow) = when (row) {
            is MoneyRow.Entry -> row.entry.categoryId
            is MoneyRow.Trip -> travelId
        }
        fun isIncome(row: MoneyRow) = row is MoneyRow.Entry && row.entry.kind == MoneyKind.INCOME
        fun inMonth(row: MoneyRow, k: Int) = row.date >= monthStart(k) && row.date < monthStart(k + 1)

        val monthRows = rows.filter { inMonth(it, key) }.sortedByDescending { it.date }
        val expenses = monthRows.filterNot(::isIncome)
        val incomes = monthRows.filter(::isIncome)
        val spent = spentByCategory(expenses.map { Spend(categoryOf(it), it.amountMinor) })
        val categoryBudgets = budgetList.filter { it.categoryId != null }.associate { it.categoryId!! to it.amountMinor }
        val totalBudgetMinor = totalBudget(categoryBudgets, budgetList.firstOrNull { it.categoryId == null }?.amountMinor)
        val expenseTotal = expenses.sumOf { it.amountMinor }
        val order = cats.map { it.id }
        MoneyMonth(
            monthKey = key,
            incomeMinor = incomes.sumOf { it.amountMinor },
            expenseMinor = expenseTotal,
            total = totalBudgetMinor?.let { BudgetLine(null, it, expenseTotal) },
            budgets = categoryBudgets.entries.sortedBy { order.indexOf(it.key) }
                .map { (id, amount) -> BudgetLine(id, amount, spent[id] ?: 0) },
            spending = spent.entries.map { it.key to it.value }.sortedByDescending { it.second },
            incomeByCategory = spentByCategory(incomes.map { Spend(categoryOf(it), it.amountMinor) })
                .entries.map { it.key to it.value }.sortedByDescending { it.second },
            lastMonths = (key - 5..key).map { k ->
                val inK = rows.filter { inMonth(it, k) }
                MonthTotals(k, inK.filter(::isIncome).sumOf { it.amountMinor }, inK.filterNot(::isIncome).sumOf { it.amountMinor })
            },
            rows = monthRows,
            tripsLeftOut = leftOutDates.count { it >= monthStart(key) && it < monthStart(key + 1) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyMonth(monthKeyOf(System.currentTimeMillis())))

    init {
        val id = userId
        if (id != null) viewModelScope.launch {
            moneyDao.seedCategoriesIfEmpty(id, defaultMoneyCategories(id))
            generateRecurring()
        }
    }

    /** Creates the entries recurring items owe up to today (one per item per month, idempotent). */
    private suspend fun generateRecurring() {
        val id = userId ?: return
        val today = startOfTodayMillis()
        moneyDao.getActiveRecurringOnce(id).forEach { r ->
            recurringDueDays(r, today).forEach { day ->
                val key = monthKeyOf(day)
                if (moneyDao.countRecurringEntries(r.id, monthStart(key), monthStart(key + 1)) == 0) {
                    moneyDao.insertEntry(
                        MoneyEntry(userId = id, kind = r.kind, amountMinor = r.amountMinor, categoryId = r.categoryId, note = r.note, date = day, recurringId = r.id)
                    )
                }
            }
        }
    }

    fun shiftMonth(by: Int) { _monthKey.value += by }

    fun setCurrency(code: String) {
        val id = userId ?: return
        viewModelScope.launch { userDao.setMoneyCurrency(id, code) }
    }

    fun saveEntry(existing: MoneyEntry?, kind: MoneyKind, amountMinor: Long, categoryId: Long?, note: String, date: Long) {
        val id = userId ?: return
        val trimmed = note.trim().ifEmpty { null }
        viewModelScope.launch {
            if (existing == null) moneyDao.insertEntry(MoneyEntry(userId = id, kind = kind, amountMinor = amountMinor, categoryId = categoryId, note = trimmed, date = date))
            else moneyDao.updateEntry(existing.copy(kind = kind, amountMinor = amountMinor, categoryId = categoryId, note = trimmed, date = date))
        }
    }

    fun deleteEntry(entry: MoneyEntry) {
        viewModelScope.launch { moneyDao.deleteEntry(entry) }
    }

    fun restoreEntry(entry: MoneyEntry) {
        viewModelScope.launch { moneyDao.insertEntry(entry) }
    }

    /** Replaces all budgets: [total] for all spending (null = none) and per expense category. */
    fun saveBudgets(total: Long?, perCategory: Map<Long, Long>) {
        val id = userId ?: return
        viewModelScope.launch {
            moneyDao.replaceBudgets(
                id,
                listOfNotNull(total?.let { MoneyBudget(userId = id, amountMinor = it) }) +
                    perCategory.map { (categoryId, amount) -> MoneyBudget(userId = id, categoryId = categoryId, amountMinor = amount) },
            )
        }
    }

    fun saveRecurring(existing: MoneyRecurring?, kind: MoneyKind, amountMinor: Long, categoryId: Long?, note: String, dayOfMonth: Int, isActive: Boolean) {
        val id = userId ?: return
        val trimmed = note.trim().ifEmpty { null }
        viewModelScope.launch {
            if (existing == null) {
                moneyDao.insertRecurring(
                    MoneyRecurring(
                        userId = id, kind = kind, amountMinor = amountMinor, categoryId = categoryId, note = trimmed,
                        dayOfMonth = dayOfMonth, startMonth = monthKeyOf(System.currentTimeMillis()), isActive = isActive,
                    )
                )
            } else {
                moneyDao.updateRecurring(existing.copy(kind = kind, amountMinor = amountMinor, categoryId = categoryId, note = trimmed, dayOfMonth = dayOfMonth, isActive = isActive))
            }
            generateRecurring()
        }
    }

    /** Deletes the recurring item; entries it already made stay (they're real money that moved). */
    fun deleteRecurring(recurring: MoneyRecurring) {
        viewModelScope.launch { moneyDao.deleteRecurring(recurring) }
    }

    fun saveGoal(existing: MoneyGoal?, name: String, targetMinor: Long, deadline: Long?) {
        val id = userId ?: return
        viewModelScope.launch {
            if (existing == null) moneyDao.insertGoal(MoneyGoal(userId = id, name = name.trim(), targetMinor = targetMinor, deadline = deadline))
            else moneyDao.updateGoal(existing.copy(name = name.trim(), targetMinor = targetMinor, deadline = deadline))
        }
    }

    /** Adds (or with a negative amount, takes out) money saved toward a goal. */
    fun addToGoal(goal: MoneyGoal, amountMinor: Long) {
        viewModelScope.launch { moneyDao.updateGoal(goal.copy(savedMinor = (goal.savedMinor + amountMinor).coerceAtLeast(0))) }
    }

    fun deleteGoal(goal: MoneyGoal) {
        viewModelScope.launch { moneyDao.deleteGoal(goal) }
    }

    fun addCategory(kind: MoneyKind, name: String) {
        val id = userId ?: return
        val trimmed = name.trim().ifEmpty { return }
        viewModelScope.launch {
            val order = (categories.value.filter { it.kind == kind }.maxOfOrNull { it.orderIndex } ?: -1) + 1
            moneyDao.insertCategory(MoneyCategory(userId = id, kind = kind, name = trimmed, orderIndex = order))
        }
    }

    fun renameCategory(category: MoneyCategory, name: String) {
        val trimmed = name.trim().ifEmpty { return }
        viewModelScope.launch { moneyDao.updateCategory(category.copy(name = trimmed)) }
    }

    fun setCategoryHidden(category: MoneyCategory, hidden: Boolean) {
        viewModelScope.launch { moneyDao.updateCategory(category.copy(isHidden = hidden)) }
    }
}
