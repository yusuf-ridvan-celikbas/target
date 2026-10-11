package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.TripActivity
import kotlinx.coroutines.flow.Flow

/** An activity with its trip's name — for the Planner agenda and reminders. */
data class TripActivityWithTrip(
    @Embedded val activity: TripActivity,
    val tripName: String,
)

@Dao
interface TripActivityDao {
    @Insert
    suspend fun insert(activity: TripActivity): Long

    @Update
    suspend fun update(activity: TripActivity)

    @Delete
    suspend fun delete(activity: TripActivity)

    @Query("UPDATE trip_activities SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    /** Untimed ones last within a day. */
    @Query("SELECT * FROM trip_activities WHERE tripId = :tripId ORDER BY date ASC, startMinuteOfDay IS NULL, startMinuteOfDay ASC, id ASC")
    fun getByTripId(tripId: Long): Flow<List<TripActivity>>

    @Query(
        """
        SELECT trip_activities.*, trips.name AS tripName FROM trip_activities
        INNER JOIN trips ON trips.id = trip_activities.tripId
        WHERE trips.userId = :userId
        """
    )
    fun getAllWithTripByUserId(userId: Long): Flow<List<TripActivityWithTrip>>
}
