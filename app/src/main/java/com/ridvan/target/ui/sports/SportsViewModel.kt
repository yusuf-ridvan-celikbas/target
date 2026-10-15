package com.ridvan.target.ui.sports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExerciseWithSets
import com.ridvan.target.data.local.dao.LiftRow
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutExercise
import com.ridvan.target.data.local.entity.WorkoutSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the log/edit dialog produced. */
data class WorkoutForm(
    val sport: Sport,
    val startedAt: Long,
    val durationSeconds: Int,
    val distanceMeters: Int?,
    val notes: String?,
    val effort: Int?,
    val exercises: List<ExerciseWithSets>,
) {
    fun toWorkout(userId: Long, id: Long = 0, isLive: Boolean = false) = Workout(
        id = id,
        userId = userId,
        sport = sport,
        startedAt = startedAt,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters.takeIf { sport.isDistance },
        notes = notes,
        effort = effort,
        isLive = isLive,
    )
}

/** A deleted workout kept in memory so Undo can put back the same row, exercises included. */
class DeletedWorkout(val workout: Workout, val exercises: List<ExerciseWithSets>)

fun newExercise(name: String, sets: List<WorkoutSet>) = ExerciseWithSets(WorkoutExercise(workoutId = 0, name = name), sets)

class SportsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as TargetApplication).database.workoutDao()
    private val userId = application.let { it as TargetApplication }.preferences.currentUserId

    val workouts: StateFlow<List<Workout>> = (userId?.let { dao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<SportGoal>> = (userId?.let { dao.getGoals(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liftRows: StateFlow<List<LiftRow>> = (userId?.let { dao.getLiftRows(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _period = MutableStateFlow(SportsPeriod.WEEK)
    val period: StateFlow<SportsPeriod> = _period.asStateFlow()
    fun setPeriod(period: SportsPeriod) { _period.value = period }

    fun addWorkout(form: WorkoutForm) {
        val uid = userId ?: return
        viewModelScope.launch { dao.insertWithExercises(form.toWorkout(uid), form.exercises) }
    }

    /** Deletes [workout] and hands back what Undo needs. */
    fun delete(workout: Workout, onDeleted: (DeletedWorkout) -> Unit) {
        viewModelScope.launch {
            val exercises = dao.loadExercises(workout.id)
            dao.delete(workout)
            onDeleted(DeletedWorkout(workout, exercises))
        }
    }

    fun restore(deleted: DeletedWorkout) {
        viewModelScope.launch { dao.insertWithExercises(deleted.workout, deleted.exercises) }
    }

    fun addGoal(goal: SportGoal) {
        val uid = userId ?: return
        viewModelScope.launch { dao.insertGoal(goal.copy(userId = uid)) }
    }

    fun deleteGoal(goal: SportGoal) {
        viewModelScope.launch { dao.deleteGoal(goal) }
    }
}
