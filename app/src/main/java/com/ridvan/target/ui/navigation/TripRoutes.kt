package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** The Plan tab's first page: Planner and Trips. */
@Serializable
object PlanHubRoute

/** Every trip — a page of the Plan tab, next to the Planner. */
@Serializable
object TripListRoute

/** One trip: overview plus Itinerary / Bookings / Packing / Budget. */
@Serializable
data class TripDetailRoute(val tripId: Long)
