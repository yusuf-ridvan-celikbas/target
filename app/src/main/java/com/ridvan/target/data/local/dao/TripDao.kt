package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Trip
import kotlinx.coroutines.flow.Flow

/** A trip plus the counts its list card shows. */
data class TripWithTotals(
    @Embedded val trip: Trip,
    val packedCount: Int,
    val packingCount: Int,
    val spentMinor: Long,
    val bookingCount: Int,
)

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getById(id: Long): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startDate ASC")
    fun getByUserId(userId: Long): Flow<List<Trip>>

    // Correlated subqueries, not joins, so the packing and expense sides can't multiply each other.
    @Query(
        """
        SELECT trips.*,
            (SELECT COUNT(*) FROM trip_packing_items p WHERE p.tripId = trips.id AND p.isPacked = 1) AS packedCount,
            (SELECT COUNT(*) FROM trip_packing_items p WHERE p.tripId = trips.id) AS packingCount,
            (SELECT COALESCE(SUM(e.amountMinor), 0) FROM trip_expenses e WHERE e.tripId = trips.id) AS spentMinor,
            (SELECT COUNT(*) FROM trip_bookings b WHERE b.tripId = trips.id) AS bookingCount
        FROM trips WHERE userId = :userId ORDER BY startDate ASC
        """
    )
    fun getWithTotalsByUserId(userId: Long): Flow<List<TripWithTotals>>
}
