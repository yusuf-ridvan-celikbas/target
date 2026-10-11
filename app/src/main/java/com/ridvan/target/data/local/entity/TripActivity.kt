package com.ridvan.target.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One itinerary entry on one day of a trip. Timed ones get a reminder, like timed Planner events. */
@Entity(
    tableName = "trip_activities",
    foreignKeys = [
        ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tripId")],
)
data class TripActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    val category: TripActivityCategory = TripActivityCategory.OTHER,
    /** Start-of-day millis; within the trip's dates. */
    val date: Long,
    /** Minute-of-day; null = no set time. */
    val startMinuteOfDay: Int? = null,
    /** Only with a start time. */
    val durationMinutes: Int? = null,
    @Embedded(prefix = "place_") val place: TripPlace = TripPlace(),
    val notes: String? = null,
    val isDone: Boolean = false,
)
