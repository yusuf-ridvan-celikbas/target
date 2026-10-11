package com.ridvan.target.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A flight/train/hotel/etc. reservation. Hand-entered today; a booking-lookup service can later hand back
 * an unsaved TripBooking to pre-fill the same form (data/trips/TripLookups.kt), recording what it came
 * from in [externalRef]. Its price isn't stored here: the form can log it as a TripExpense instead, so
 * spending has a single source.
 */
@Entity(
    tableName = "trip_bookings",
    foreignKeys = [
        ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tripId")],
)
data class TripBooking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val type: TripBookingType,
    /** e.g. "TK 1823" or the hotel's name. */
    val title: String,
    /** Airline, rail company, hotel chain, rental company… */
    val provider: String? = null,
    val confirmationCode: String? = null,
    /** Departure / check-in day (start-of-day millis) and optional time. */
    val startDate: Long,
    val startMinuteOfDay: Int? = null,
    /** Arrival / check-out day and time, both optional. */
    val endDate: Long? = null,
    val endMinuteOfDay: Int? = null,
    /** Departure place for transport, the property for a stay. */
    @Embedded(prefix = "place_") val place: TripPlace = TripPlace(),
    /** Arrival place — transport only. */
    @Embedded(prefix = "arrival_") val arrivalPlace: TripPlace = TripPlace(),
    val notes: String? = null,
    /** Provider id when filled in by a lookup service; null for hand-entered bookings. */
    val externalRef: String? = null,
)
