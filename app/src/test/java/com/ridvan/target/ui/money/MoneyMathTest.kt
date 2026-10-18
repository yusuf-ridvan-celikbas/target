package com.ridvan.target.ui.money

import com.ridvan.target.data.local.entity.MoneyKind
import com.ridvan.target.data.local.entity.MoneyRecurring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MoneyMathTest {
    private fun day(y: Int, m: Int, d: Int): Long = Calendar.getInstance().run { clear(); set(y, m - 1, d); timeInMillis }

    @Test
    fun monthKeysRoundTrip() {
        val key = monthKeyOf(day(2026, 10, 18))
        assertEquals(2026 * 12 + 9, key)
        assertEquals(day(2026, 10, 1), monthStart(key))
        assertEquals(day(2027, 2, 28), dayInMonth(monthKeyOf(day(2027, 2, 1)), 31))
    }

    @Test
    fun recurringDueDaysClampAndStopAtToday() {
        val rent = MoneyRecurring(userId = 1, kind = MoneyKind.EXPENSE, amountMinor = 100, dayOfMonth = 31, startMonth = monthKeyOf(day(2026, 1, 1)))
        val due = recurringDueDays(rent, day(2026, 3, 30))
        assertEquals(listOf(day(2026, 1, 31), day(2026, 2, 28)), due)
        assertEquals(emptyList<Long>(), recurringDueDays(rent.copy(isActive = false), day(2026, 3, 31)))
    }

    @Test
    fun tripConversion() {
        assertEquals(500L, tripAmountInMoneyCurrency(500, "TRY", null, null, "TRY"))
        assertEquals(3650L, tripAmountInMoneyCurrency(100, "EUR", "TRY", 36.5, "TRY"))
        assertNull(tripAmountInMoneyCurrency(100, "EUR", "USD", 1.1, "TRY"))
        assertNull(tripAmountInMoneyCurrency(100, "EUR", "TRY", null, "TRY"))
    }

    @Test
    fun budgetsAndSpending() {
        val spent = spentByCategory(listOf(Spend(1, 300), Spend(1, 200), Spend(null, 50)))
        assertEquals(500L, spent[1])
        assertEquals(50L, spent[null])
        assertEquals(1500L, totalBudget(mapOf(1L to 1000L, 2L to 500L), null))
        assertEquals(2000L, totalBudget(mapOf(1L to 1000L), 2000L))
        assertNull(totalBudget(emptyMap(), null))
        assertEquals(-200L, BudgetLine(1, 300, 500).leftMinor)
    }

    @Test
    fun goals() {
        assertEquals(0.5f, goalFraction(500, 1000), 1e-6f)
        // Oct 18 → Dec 31: 3 months (Oct, Nov, Dec) for 900 left.
        assertEquals(300L, monthlyNeededForGoal(100, 1000, day(2026, 12, 31), day(2026, 10, 18)))
        assertNull(monthlyNeededForGoal(1000, 1000, day(2026, 12, 31), day(2026, 10, 18)))
    }
}
