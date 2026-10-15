package com.ridvan.target.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.TripWithTotals
import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.data.local.entity.TripActivity
import com.ridvan.target.data.local.entity.TripBooking
import com.ridvan.target.data.local.entity.TripBookingType
import com.ridvan.target.data.local.entity.TripExpense
import com.ridvan.target.data.local.entity.TripExpenseCategory
import com.ridvan.target.data.local.entity.TripPackingItem
import com.ridvan.target.data.local.entity.TripPackingTemplate
import com.ridvan.target.data.local.entity.TripPackingTemplateItem
import com.ridvan.target.ui.navigation.TripDetailRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripListViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val tripDao = app.database.tripDao()
    private val userId = app.preferences.currentUserId

    val trips: StateFlow<List<TripWithTotals>> = (userId?.let { tripDao.getWithTotalsByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Inserts the trip and hands back its id (to open it straight away). */
    fun addTrip(trip: Trip, onCreated: (Long) -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch { onCreated(tripDao.insert(trip.copy(userId = uid))) }
    }
}

class TripDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val database = (application as TargetApplication).database
    private val tripDao = database.tripDao()
    private val activityDao = database.tripActivityDao()
    private val bookingDao = database.tripBookingDao()
    private val packingDao = database.tripPackingItemDao()
    private val expenseDao = database.tripExpenseDao()
    private val templateDao = database.tripPackingTemplateDao()
    private val userId = (application as TargetApplication).preferences.currentUserId
    val tripId: Long = savedStateHandle.toRoute<TripDetailRoute>().tripId

    private fun <T> state(flow: Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val trip: StateFlow<Trip?> = state(tripDao.getById(tripId), null)
    val activities: StateFlow<List<TripActivity>> = state(activityDao.getByTripId(tripId), emptyList())
    val bookings: StateFlow<List<TripBooking>> = state(bookingDao.getByTripId(tripId), emptyList())
    val packingItems: StateFlow<List<TripPackingItem>> = state(packingDao.getByTripId(tripId), emptyList())
    val expenses: StateFlow<List<TripExpense>> = state(expenseDao.getByTripId(tripId), emptyList())

    fun updateTrip(updated: Trip) {
        viewModelScope.launch { tripDao.update(updated) }
    }

    fun deleteTrip() {
        val current = trip.value ?: return
        viewModelScope.launch { tripDao.delete(current) }
    }

    fun saveActivity(activity: TripActivity) {
        viewModelScope.launch { if (activity.id == 0L) activityDao.insert(activity) else activityDao.update(activity) }
    }

    fun deleteActivity(activity: TripActivity) {
        viewModelScope.launch { activityDao.delete(activity) }
    }

    fun setActivityDone(activity: TripActivity, done: Boolean) {
        viewModelScope.launch { activityDao.setDone(activity.id, done) }
    }

    /** A new booking's price (if given) is logged once as an expense in the matching category. */
    fun saveBooking(booking: TripBooking, priceMinor: Long?) {
        viewModelScope.launch {
            if (booking.id == 0L) bookingDao.insert(booking) else bookingDao.update(booking)
            if (priceMinor != null && priceMinor > 0) {
                expenseDao.insert(
                    TripExpense(
                        tripId = tripId,
                        title = booking.title,
                        amountMinor = priceMinor,
                        category = if (booking.type == TripBookingType.HOTEL) TripExpenseCategory.STAY else TripExpenseCategory.TRANSPORT,
                        date = booking.startDate,
                    )
                )
            }
        }
    }

    fun deleteBooking(booking: TripBooking) {
        viewModelScope.launch { bookingDao.delete(booking) }
    }

    fun savePackingItem(item: TripPackingItem) {
        viewModelScope.launch { if (item.id == 0L) packingDao.insert(item) else packingDao.update(item) }
    }

    fun deletePackingItem(item: TripPackingItem) {
        viewModelScope.launch { packingDao.delete(item) }
    }

    fun setPacked(item: TripPackingItem, packed: Boolean) {
        viewModelScope.launch { packingDao.setPacked(item.id, packed) }
    }

    fun unpackAll() {
        viewModelScope.launch { packingDao.unpackAll(tripId) }
    }

    /** Adds the given starter items (names already localized), skipping ones already on the list. */
    fun addStarterItems(items: List<TripPackingItem>) {
        val existing = packingItems.value.map { it.name.lowercase() }.toSet()
        val fresh = items.filter { it.name.lowercase() !in existing }
        if (fresh.isEmpty()) return
        viewModelScope.launch { packingDao.insertAll(fresh) }
    }

    val templates: StateFlow<List<TripPackingTemplate>> =
        state(userId?.let { templateDao.getByUserId(it) } ?: flowOf(emptyList()), emptyList())

    /** Re-inserts a just-deleted item (same id) for Undo. */
    fun restorePackingItem(item: TripPackingItem) {
        viewModelScope.launch { packingDao.insert(item) }
    }

    fun saveTemplate(name: String) {
        val uid = userId ?: return
        val items = packingItems.value.map { TripPackingTemplateItem(templateId = 0, name = it.name, category = it.category, quantity = it.quantity) }
        viewModelScope.launch { templateDao.saveWithItems(TripPackingTemplate(userId = uid, name = name), items) }
    }

    fun deleteTemplate(template: TripPackingTemplate) {
        viewModelScope.launch { templateDao.delete(template.id) }
    }

    /** Adds the template's items (fresh, unpacked), skipping names already on the list. */
    fun addFromTemplate(template: TripPackingTemplate) {
        viewModelScope.launch {
            val existing = packingItems.value.map { it.name.lowercase() }.toSet()
            val fresh = templateDao.getItems(template.id).filter { it.name.lowercase() !in existing }
                .map { TripPackingItem(tripId = tripId, name = it.name, category = it.category, quantity = it.quantity) }
            if (fresh.isNotEmpty()) packingDao.insertAll(fresh)
        }
    }

    fun saveExpense(expense: TripExpense) {
        viewModelScope.launch { if (expense.id == 0L) expenseDao.insert(expense) else expenseDao.update(expense) }
    }

    fun deleteExpense(expense: TripExpense) {
        viewModelScope.launch { expenseDao.delete(expense) }
    }
}
