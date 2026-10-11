package com.ridvan.target.data.local.entity

/** What a booking is. Transport types also use an arrival place. */
enum class TripBookingType {
    FLIGHT, TRAIN, BUS, FERRY, CAR_RENTAL, HOTEL, OTHER;

    val isTransport: Boolean get() = this == FLIGHT || this == TRAIN || this == BUS || this == FERRY
}

/** Itinerary activity kinds (icon + label only). */
enum class TripActivityCategory { SIGHTSEEING, FOOD, ACTIVITY, TRANSPORT, SHOPPING, OTHER }

/** Packing list groups; declaration order is display order. */
enum class TripPackingCategory { DOCUMENTS, CLOTHES, TOILETRIES, ELECTRONICS, HEALTH, OTHER }

/** Expense groups; declaration order is display order. */
enum class TripExpenseCategory { TRANSPORT, STAY, FOOD, ACTIVITIES, SHOPPING, OTHER }
