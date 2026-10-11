package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** Every trip — a top-level destination next to the Planner. */
@Serializable
object TripListRoute

/** One trip: overview plus Itinerary / Bookings / Packing / Budget. */
@Serializable
data class TripDetailRoute(val tripId: Long)
