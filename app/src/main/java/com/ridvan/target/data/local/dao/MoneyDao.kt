package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ridvan.target.data.local.entity.MoneyBudget
import com.ridvan.target.data.local.entity.MoneyCategory
import com.ridvan.target.data.local.entity.MoneyEntry
import com.ridvan.target.data.local.entity.MoneyGoal
import com.ridvan.target.data.local.entity.MoneyRecurring
import com.ridvan.target.data.local.entity.TripExpense
import kotlinx.coroutines.flow.Flow

/** A trip expense with what's needed to show it under Money's Travel category. */
data class TripExpenseForMoney(
    @Embedded val expense: TripExpense,
    val tripName: String,
    val tripCurrency: String,
    val homeCurrency: String?,
    val exchangeRate: Double?,
)

@Dao
interface MoneyDao {
    // Categories
    @Query("SELECT * FROM money_categories WHERE userId = :userId ORDER BY kind, orderIndex, id")
    fun getCategories(userId: Long): Flow<List<MoneyCategory>>

    @Query("SELECT COUNT(*) FROM money_categories WHERE userId = :userId")
    suspend fun countCategories(userId: Long): Int

    @Insert
    suspend fun insertCategories(categories: List<MoneyCategory>)

    @Insert
    suspend fun insertCategory(category: MoneyCategory): Long

    @Update
    suspend fun updateCategory(category: MoneyCategory)

    /** Seeds the default categories the first time Money is opened (idempotent). */
    @Transaction
    suspend fun seedCategoriesIfEmpty(userId: Long, defaults: List<MoneyCategory>) {
        if (countCategories(userId) == 0) insertCategories(defaults)
    }

    // Entries
    @Query("SELECT * FROM money_entries WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getEntries(userId: Long): Flow<List<MoneyEntry>>

    @Insert
    suspend fun insertEntry(entry: MoneyEntry): Long

    @Update
    suspend fun updateEntry(entry: MoneyEntry)

    @Delete
    suspend fun deleteEntry(entry: MoneyEntry)

    @Query("SELECT COUNT(*) FROM money_entries WHERE recurringId = :recurringId AND date >= :from AND date < :to")
    suspend fun countRecurringEntries(recurringId: Long, from: Long, to: Long): Int

    // Budgets
    @Query("SELECT * FROM money_budgets WHERE userId = :userId")
    fun getBudgets(userId: Long): Flow<List<MoneyBudget>>

    @Query("DELETE FROM money_budgets WHERE userId = :userId")
    suspend fun deleteBudgets(userId: Long)

    @Insert
    suspend fun insertBudgets(budgets: List<MoneyBudget>)

    @Transaction
    suspend fun replaceBudgets(userId: Long, budgets: List<MoneyBudget>) {
        deleteBudgets(userId)
        insertBudgets(budgets)
    }

    // Recurring
    @Query("SELECT * FROM money_recurring WHERE userId = :userId ORDER BY dayOfMonth")
    fun getRecurring(userId: Long): Flow<List<MoneyRecurring>>

    @Query("SELECT * FROM money_recurring WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveRecurringOnce(userId: Long): List<MoneyRecurring>

    @Insert
    suspend fun insertRecurring(recurring: MoneyRecurring): Long

    @Update
    suspend fun updateRecurring(recurring: MoneyRecurring)

    @Delete
    suspend fun deleteRecurring(recurring: MoneyRecurring)

    // Goals
    @Query("SELECT * FROM money_goals WHERE userId = :userId ORDER BY createdAt")
    fun getGoals(userId: Long): Flow<List<MoneyGoal>>

    @Insert
    suspend fun insertGoal(goal: MoneyGoal): Long

    @Update
    suspend fun updateGoal(goal: MoneyGoal)

    @Delete
    suspend fun deleteGoal(goal: MoneyGoal)

    // Trips
    @Query(
        """
        SELECT e.*, t.name AS tripName, t.currency AS tripCurrency, t.homeCurrency, t.exchangeRate
        FROM trip_expenses e JOIN trips t ON t.id = e.tripId
        WHERE t.userId = :userId
        """
    )
    fun getTripExpenses(userId: Long): Flow<List<TripExpenseForMoney>>
}
