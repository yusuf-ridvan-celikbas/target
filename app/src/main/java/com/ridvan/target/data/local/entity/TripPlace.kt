package com.ridvan.target.data.local.entity

/**
 * Where something happens — embedded (column-prefixed) in activities and bookings. Name and address are
 * typed by hand today; latitude/longitude stay null until a place-search service fills them
 * (see data/trips/TripLookups.kt).
 */
data class TripPlace(
    val name: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val label: String? get() = listOfNotNull(name?.takeIf { it.isNotBlank() }, address?.takeIf { it.isNotBlank() })
        .joinToString(", ").takeIf { it.isNotBlank() }
}
