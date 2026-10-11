package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Money spent on a trip, in the trip's own currency. */
@Entity(
    tableName = "trip_expenses",
    foreignKeys = [
        ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tripId")],
)
data class TripExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    /** Minor units (cents/kuruş) of the trip's currency. */
    val amountMinor: Long,
    val category: TripExpenseCategory = TripExpenseCategory.OTHER,
    /** Start-of-day millis. */
    val date: Long,
    val notes: String? = null,
)
