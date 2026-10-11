package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A trip: its own dates, one currency for every amount in it, and an optional budget. Per-user, like
 * Course/Book. Its activities, bookings, packing list and expenses hang off it (all CASCADE). Everything
 * is typed by hand for now; see data/trips/TripLookups.kt for where online helpers will plug in later.
 */
@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val name: String,
    val destination: String? = null,
    /** Start-of-day millis of the first day. */
    val startDate: Long,
    /** Start-of-day millis of the last day (inclusive); never before [startDate]. */
    val endDate: Long,
    /** ISO 4217 code, e.g. "EUR". Every amount in the trip is in this currency. */
    val currency: String,
    /** In minor units (cents/kuruş), so sums never drift; null = no budget set. */
    val budgetMinor: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
