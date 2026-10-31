package com.ridvan.target.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.navigation.RoutineDetailRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A routine with its step count and, when it is on today, today's progress. */
data class RoutineListItem(val routine: Routine, val stepCount: Int, val today: RoutineProgress?)

class RoutineListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as TargetApplication).database.routineDao()
    private val userId = (application as TargetApplication).preferences.currentUserId

    val routines: StateFlow<List<RoutineListItem>> = if (userId == null) {
        flowOf(emptyList<RoutineListItem>())
    } else {
        combine(dao.getByUserId(userId), dao.getAllStepsByUserId(userId), dao.getAllCompletionsByUserId(userId)) { routines, steps, completions ->
            val today = startOfTodayMillis()
            val stepsBy = steps.groupBy { it.routineId }
            routines.map { routine ->
                val own = stepsBy[routine.id].orEmpty()
                RoutineListItem(routine, own.size, routineProgress(own, completions, today).takeIf { routine.runsOn(today) })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Inserts the routine and hands back its id (to open it straight away). */
    fun addRoutine(routine: Routine, onCreated: (Long) -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch { onCreated(dao.insert(routine.copy(userId = uid))) }
    }
}

class RoutineDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val dao = (application as TargetApplication).database.routineDao()
    private val route = savedStateHandle.toRoute<RoutineDetailRoute>()
    val routineId: Long = route.routineId

    /** The day whose checklist is shown. */
    val dayMillis: Long = route.dateMillis.takeIf { it > 0 } ?: startOfTodayMillis()

    private fun <T> state(flow: Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val routine: StateFlow<Routine?> = state(dao.getById(routineId), null)
    val steps: StateFlow<List<RoutineStep>> = state(dao.getSteps(routineId), emptyList())

    /** Ids of the steps ticked on [dayMillis]. */
    val doneStepIds: StateFlow<Set<Long>> = state(
        dao.getCompletions(routineId).map { list -> list.filter { it.date == dayMillis }.map { it.routineStepId }.toSet() },
        emptySet(),
    )

    /** Streak up to today (not [dayMillis]): scheduled days in a row with every step ticked. */
    val streak: StateFlow<com.ridvan.target.ui.common.Streak> = state(
        kotlinx.coroutines.flow.combine(dao.getById(routineId), dao.getSteps(routineId), dao.getCompletions(routineId)) { r, s, c ->
            r?.let { com.ridvan.target.ui.common.routineStreak(it, s, c, startOfTodayMillis()) } ?: com.ridvan.target.ui.common.Streak.NONE
        },
        com.ridvan.target.ui.common.Streak.NONE,
    )

    fun setStepDone(step: RoutineStep, done: Boolean) {
        viewModelScope.launch {
            if (done) dao.insertCompletion(RoutineStepCompletion(routineStepId = step.id, date = dayMillis))
            else dao.deleteCompletion(step.id, dayMillis)
        }
    }

    fun updateRoutine(updated: Routine) {
        viewModelScope.launch { dao.update(updated) }
    }

    fun deleteRoutine() {
        val current = routine.value ?: return
        viewModelScope.launch { dao.delete(current) }
    }

    /** Adds a step at the end, or renames [existing]. */
    fun saveStep(name: String, existing: RoutineStep?) {
        viewModelScope.launch {
            if (existing == null) dao.insertStep(RoutineStep(routineId = routineId, name = name, orderIndex = dao.nextOrderIndex(routineId)))
            else dao.updateSteps(listOf(existing.copy(name = name)))
        }
    }

    fun deleteStep(step: RoutineStep) {
        viewModelScope.launch { dao.deleteStep(step) }
    }

    /** Swaps [step] with its neighbour ([delta] -1 = up, +1 = down) and renumbers the list. */
    fun moveStep(step: RoutineStep, delta: Int) {
        val list = steps.value.toMutableList()
        val from = list.indexOfFirst { it.id == step.id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { dao.updateSteps(list.mapIndexed { index, s -> s.copy(orderIndex = index) }) }
    }
}
