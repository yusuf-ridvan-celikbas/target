package com.ridvan.target.data.trips

import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripPlace

/*
 * Where online helpers for the Trip Planner will plug in later. Today every trip detail is typed by hand
 * and the app stays fully offline (no INTERNET permission), so TargetApplication.tripLookups is
 * [TripLookups.OFFLINE]: every service is null and the trip screens show no lookup actions at all.
 *
 * Adding one later (needs the developer's go-ahead, since it means network access):
 *  - implement the interface, set it on TargetApplication.tripLookups;
 *  - the forms already take what these return: a TripPlace fills an activity's/booking's place fields
 *    (including latitude/longitude, which are stored but never entered by hand), and an unsaved TripBooking
 *    pre-fills TripBookingDialog via its `initial` parameter (keep externalRef set to the provider's id);
 *  - show the matching "Look up…" action only when that service is non-null.
 */

/** Text search for a place ("Louvre", "Hotel Adlon Berlin"). */
fun interface PlaceSearch {
    suspend fun search(query: String): List<TripPlace>
}

/** Fills a booking from a reference, e.g. a flight number + date. Null when nothing matched. */
fun interface BookingLookup {
    suspend fun lookup(type: TripBookingType, reference: String, dayStartMillis: Long?): TripBooking?
}

/** Exchange rate from one ISO 4217 currency to another; null when unknown. */
fun interface ExchangeRates {
    suspend fun rate(from: String, to: String): Double?
}

data class TripLookups(
    val places: PlaceSearch? = null,
    val bookings: BookingLookup? = null,
    val rates: ExchangeRates? = null,
) {
    companion object {
        val OFFLINE = TripLookups()
    }
}
