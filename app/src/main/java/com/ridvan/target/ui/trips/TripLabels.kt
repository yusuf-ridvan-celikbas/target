package com.ridvan.target.ui.trips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Soap
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.TripActivityCategory
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripExpenseCategory
import com.ridvan.target.data.local.entity.TripPackingCategory

@Composable
fun tripBookingTypeLabel(type: TripBookingType): String = stringResource(tripBookingTypeRes(type))

fun tripBookingTypeLabelText(context: android.content.Context, type: TripBookingType): String =
    context.getString(tripBookingTypeRes(type))

private fun tripBookingTypeRes(type: TripBookingType): Int =
    when (type) {
        TripBookingType.FLIGHT -> R.string.trip_booking_flight
        TripBookingType.TRAIN -> R.string.trip_booking_train
        TripBookingType.BUS -> R.string.trip_booking_bus
        TripBookingType.FERRY -> R.string.trip_booking_ferry
        TripBookingType.CAR_RENTAL -> R.string.trip_booking_car_rental
        TripBookingType.HOTEL -> R.string.trip_booking_hotel
        TripBookingType.OTHER -> R.string.trip_category_other
    }

fun tripBookingTypeIcon(type: TripBookingType): ImageVector = when (type) {
    TripBookingType.FLIGHT -> Icons.Filled.Flight
    TripBookingType.TRAIN -> Icons.Filled.Train
    TripBookingType.BUS -> Icons.Filled.DirectionsBus
    TripBookingType.FERRY -> Icons.Filled.DirectionsBoat
    TripBookingType.CAR_RENTAL -> Icons.Filled.DirectionsCar
    TripBookingType.HOTEL -> Icons.Filled.Hotel
    TripBookingType.OTHER -> Icons.Filled.ConfirmationNumber
}

@Composable
fun tripActivityCategoryLabel(category: TripActivityCategory): String = stringResource(
    when (category) {
        TripActivityCategory.SIGHTSEEING -> R.string.trip_activity_sightseeing
        TripActivityCategory.FOOD -> R.string.trip_activity_food
        TripActivityCategory.ACTIVITY -> R.string.trip_activity_activity
        TripActivityCategory.TRANSPORT -> R.string.trip_activity_transport
        TripActivityCategory.SHOPPING -> R.string.trip_activity_shopping
        TripActivityCategory.OTHER -> R.string.trip_category_other
    }
)

fun tripActivityCategoryIcon(category: TripActivityCategory): ImageVector = when (category) {
    TripActivityCategory.SIGHTSEEING -> Icons.Filled.Museum
    TripActivityCategory.FOOD -> Icons.Filled.Restaurant
    TripActivityCategory.ACTIVITY -> Icons.Filled.LocalActivity
    TripActivityCategory.TRANSPORT -> Icons.Filled.DirectionsBus
    TripActivityCategory.SHOPPING -> Icons.Filled.ShoppingBag
    TripActivityCategory.OTHER -> Icons.AutoMirrored.Filled.DirectionsWalk
}

@Composable
fun tripPackingCategoryLabel(category: TripPackingCategory): String = stringResource(
    when (category) {
        TripPackingCategory.DOCUMENTS -> R.string.trip_packing_documents
        TripPackingCategory.CLOTHES -> R.string.trip_packing_clothes
        TripPackingCategory.TOILETRIES -> R.string.trip_packing_toiletries
        TripPackingCategory.ELECTRONICS -> R.string.trip_packing_electronics
        TripPackingCategory.HEALTH -> R.string.trip_packing_health
        TripPackingCategory.OTHER -> R.string.trip_category_other
    }
)

fun tripPackingCategoryIcon(category: TripPackingCategory): ImageVector = when (category) {
    TripPackingCategory.DOCUMENTS -> Icons.Filled.Description
    TripPackingCategory.CLOTHES -> Icons.Filled.Checkroom
    TripPackingCategory.TOILETRIES -> Icons.Filled.Soap
    TripPackingCategory.ELECTRONICS -> Icons.Filled.Devices
    TripPackingCategory.HEALTH -> Icons.Filled.MedicalServices
    TripPackingCategory.OTHER -> Icons.Filled.MoreHoriz
}

@Composable
fun tripExpenseCategoryLabel(category: TripExpenseCategory): String = stringResource(
    when (category) {
        TripExpenseCategory.TRANSPORT -> R.string.trip_expense_transport
        TripExpenseCategory.STAY -> R.string.trip_expense_stay
        TripExpenseCategory.FOOD -> R.string.trip_activity_food
        TripExpenseCategory.ACTIVITIES -> R.string.trip_expense_activities
        TripExpenseCategory.SHOPPING -> R.string.trip_activity_shopping
        TripExpenseCategory.OTHER -> R.string.trip_category_other
    }
)

fun tripExpenseCategoryIcon(category: TripExpenseCategory): ImageVector = when (category) {
    TripExpenseCategory.TRANSPORT -> Icons.Filled.DirectionsBus
    TripExpenseCategory.STAY -> Icons.Filled.Hotel
    TripExpenseCategory.FOOD -> Icons.Filled.Restaurant
    TripExpenseCategory.ACTIVITIES -> Icons.Filled.LocalActivity
    TripExpenseCategory.SHOPPING -> Icons.Filled.ShoppingBag
    TripExpenseCategory.OTHER -> Icons.Filled.MoreHoriz
}

/** "In 9 days" / "Day 3 of 5" / "Ended". */
@Composable
fun tripPhaseLabel(phase: TripPhase): String = when (phase) {
    is TripPhase.Upcoming -> when (phase.daysUntil) {
        0 -> stringResource(R.string.days_until_today)
        1 -> stringResource(R.string.trip_starts_tomorrow)
        else -> stringResource(R.string.trip_starts_in_days, phase.daysUntil)
    }
    is TripPhase.Ongoing -> stringResource(R.string.trip_day_of, phase.dayNumber, phase.dayCount)
    TripPhase.Past -> stringResource(R.string.trip_ended)
}
