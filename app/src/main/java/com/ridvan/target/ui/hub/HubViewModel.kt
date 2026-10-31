package com.ridvan.target.ui.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Trip
import com.ridvan.target.ui.common.daysUntil
import com.ridvan.target.ui.home.HomeData
import com.ridvan.target.ui.mockexam.mockExamNet
import com.ridvan.target.ui.planner.StudiedPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The trip a hub row talks about: the one under way ([dayNumber] of [dayCount]) or the next one ([dayNumber] null). */
data class HubTrip(val trip: Trip, val dayNumber: Int?, val dayCount: Int)

/** Live lines for the Study, Plan and Life hubs; each hub collects only what it shows. */
class HubViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val userId = app.preferences.currentUserId
    private val db = app.database
    private val data = HomeData(app, userId)

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
    private fun <T> userFlow(query: (Long) -> Flow<List<T>>): Flow<List<T>> = userId?.let(query) ?: flowOf(emptyList())

    val upcomingEvents = data.upcomingEvents.state(emptyList())
    val plannerEvents = data.upcomingPlannerEvents.state(emptyList())
    val todaysRoutines = data.todaysRoutines.state(emptyList())
    val reading = data.reading.state(null)
    val studied = data.studied.state(StudiedPreview(emptyList(), 0, 0))
    val money = data.money.state(null)

    val courseCount: StateFlow<Int> = userFlow { db.courseDao().getByUserId(it) }.map { it.size }.state(0)
    val languageCount: StateFlow<Int> = userFlow { db.languageDao().getByUserId(it) }.map { it.size }.state(0)

    /** Net of the latest finished mock exam, null when none. */
    val lastMockNet: StateFlow<Double?> = userFlow { db.mockExamDao().getSummariesForUser(it) }
        .map { list -> list.lastOrNull()?.let { mockExamNet(it.correctCount, it.wrongCount) } }
        .state(null)

    val trip: StateFlow<HubTrip?> = userFlow { db.tripDao().getByUserId(it) }.map { trips ->
        val withDays = trips.map { it to daysUntil(it.startDate) }
        val ongoing = withDays.firstOrNull { (trip, start) -> start <= 0 && daysUntil(trip.endDate) >= 0 }
        val next = withDays.filter { it.second > 0 }.minByOrNull { it.second }
        (ongoing ?: next)?.let { (trip, start) ->
            val dayCount = (daysUntil(trip.endDate) - start + 1).toInt()
            HubTrip(trip, if (start <= 0) (1 - start).toInt() else null, dayCount)
        }
    }.state(null)
}
