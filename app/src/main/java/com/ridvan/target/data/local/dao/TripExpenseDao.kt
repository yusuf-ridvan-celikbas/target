package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.TripExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface TripExpenseDao {
    @Insert
    suspend fun insert(expense: TripExpense): Long

    @Update
    suspend fun update(expense: TripExpense)

    @Delete
    suspend fun delete(expense: TripExpense)

    /** Newest day first. */
    @Query("SELECT * FROM trip_expenses WHERE tripId = :tripId ORDER BY date DESC, id DESC")
    fun getByTripId(tripId: Long): Flow<List<TripExpense>>
}
