package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.TripBooking
import kotlinx.coroutines.flow.Flow

/** A booking with its trip's name — for the Planner agenda and reminders. */
data class TripBookingWithTrip(
    @Embedded val booking: TripBooking,
    val tripName: String,
)

@Dao
interface TripBookingDao {
    @Insert
    suspend fun insert(booking: TripBooking): Long

    @Update
    suspend fun update(booking: TripBooking)

    @Delete
    suspend fun delete(booking: TripBooking)

    @Query("SELECT * FROM trip_bookings WHERE tripId = :tripId ORDER BY startDate ASC, startMinuteOfDay IS NULL, startMinuteOfDay ASC, id ASC")
    fun getByTripId(tripId: Long): Flow<List<TripBooking>>

    @Query(
        """
        SELECT trip_bookings.*, trips.name AS tripName FROM trip_bookings
        INNER JOIN trips ON trips.id = trip_bookings.tripId
        WHERE trips.userId = :userId
        """
    )
    fun getAllWithTripByUserId(userId: Long): Flow<List<TripBookingWithTrip>>
}
