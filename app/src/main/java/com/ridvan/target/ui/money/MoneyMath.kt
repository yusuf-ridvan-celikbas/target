package com.ridvan.target.ui.money

import com.ridvan.target.data.local.entity.MoneyCategory
import com.ridvan.target.data.local.entity.MoneyKind
import com.ridvan.target.data.local.entity.MoneyRecurring
import java.util.Calendar
import kotlin.math.roundToLong

/** Months as one number: year × 12 + month index (January = 0). */
fun monthKeyOf(millis: Long): Int = Calendar.getInstance().run {
    timeInMillis = millis
    get(Calendar.YEAR) * 12 + get(Calendar.MONTH)
}

/** Start of the month's first day (local time). */
fun monthStart(monthKey: Int): Long = Calendar.getInstance().run {
    clear()
    set(monthKey / 12, monthKey % 12, 1)
    timeInMillis
}

/** Start of the month's [day] (clamped to its last day). */
fun dayInMonth(monthKey: Int, day: Int): Long = Calendar.getInstance().run {
    clear()
    set(monthKey / 12, monthKey % 12, 1)
    set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
    timeInMillis
}

/** Days a recurring item falls on, from its start month up to [todayStart] (inclusive). */
fun recurringDueDays(recurring: MoneyRecurring, todayStart: Long): List<Long> {
    if (!recurring.isActive) return emptyList()
    return (recurring.startMonth..monthKeyOf(todayStart))
        .map { dayInMonth(it, recurring.dayOfMonth) }
        .filter { it <= todayStart }
}

/**
 * A trip expense in Money's currency: as-is when the trip uses it, converted with the trip's
 * rate when Money's currency is the trip's home currency, otherwise null (left out).
 */
fun tripAmountInMoneyCurrency(
    amountMinor: Long,
    tripCurrency: String,
    homeCurrency: String?,
    exchangeRate: Double?,
    moneyCurrency: String,
): Long? = when {
    tripCurrency.equals(moneyCurrency, ignoreCase = true) -> amountMinor
    homeCurrency.equals(moneyCurrency, ignoreCase = true) && exchangeRate != null -> (amountMinor * exchangeRate).roundToLong()
    else -> null
}

/** Default categories seeded per account; keys are translated for display until renamed. */
val DEFAULT_EXPENSE_KEYS = listOf("FOOD", "TRANSPORT", "BILLS", "RENT", "SHOPPING", "HEALTH", "EDUCATION", "FUN", "TRAVEL", "OTHER")
val DEFAULT_INCOME_KEYS = listOf("SALARY", "ALLOWANCE", "OTHER_INCOME")
const val TRAVEL_KEY = "TRAVEL"

fun defaultMoneyCategories(userId: Long): List<MoneyCategory> =
    DEFAULT_EXPENSE_KEYS.mapIndexed { i, key -> MoneyCategory(userId = userId, kind = MoneyKind.EXPENSE, systemKey = key, orderIndex = i) } +
        DEFAULT_INCOME_KEYS.mapIndexed { i, key -> MoneyCategory(userId = userId, kind = MoneyKind.INCOME, systemKey = key, orderIndex = i) }

/** One spending line in a month: an expense or a (converted) trip expense, by category. */
data class Spend(val categoryId: Long?, val amountMinor: Long)

/** Budget state for one category (or all spending): what's spent and left this month. */
data class BudgetLine(val categoryId: Long?, val budgetMinor: Long, val spentMinor: Long) {
    val leftMinor: Long get() = budgetMinor - spentMinor
    val fraction: Float get() = if (budgetMinor <= 0) 0f else (spentMinor.toFloat() / budgetMinor).coerceAtLeast(0f)
}

/** Spent per category id (null = uncategorized). */
fun spentByCategory(spends: List<Spend>): Map<Long?, Long> =
    spends.groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.amountMinor } }

/**
 * The month's overall budget: the explicit total if set, else the sum of category budgets
 * (null when there are none).
 */
fun totalBudget(categoryBudgets: Map<Long, Long>, explicitTotal: Long?): Long? =
    explicitTotal ?: categoryBudgets.values.sum().takeIf { categoryBudgets.isNotEmpty() }

/** Saved share of a goal, 0..1. */
fun goalFraction(savedMinor: Long, targetMinor: Long): Float =
    if (targetMinor <= 0) 0f else (savedMinor.toFloat() / targetMinor).coerceIn(0f, 1f)

/** What a goal needs per month to reach its target by [deadline] (null if no deadline, reached or past). */
fun monthlyNeededForGoal(savedMinor: Long, targetMinor: Long, deadline: Long?, todayStart: Long): Long? {
    if (deadline == null || savedMinor >= targetMinor || deadline < todayStart) return null
    val months = (monthKeyOf(deadline) - monthKeyOf(todayStart) + 1).coerceAtLeast(1)
    return (targetMinor - savedMinor + months - 1) / months
}
