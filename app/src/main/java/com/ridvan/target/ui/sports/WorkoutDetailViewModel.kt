package com.ridvan.target.ui.sports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExerciseWithSets
import com.ridvan.target.data.local.entity.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val workoutId: Long = checkNotNull(savedStateHandle.get<Long>("workoutId"))
    private val dao = (application as TargetApplication).database.workoutDao()

    val workout: StateFlow<Workout?> = dao.getById(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<ExerciseWithSets>> = combine(dao.getExercises(workoutId), dao.getSets(workoutId)) { exercises, sets ->
        val byExercise = sets.groupBy { it.exerciseId }
        exercises.map { ExerciseWithSets(it, byExercise[it.id].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(form: WorkoutForm) {
        val current = workout.value ?: return
        viewModelScope.launch {
            dao.update(form.toWorkout(current.userId, current.id, current.isLive))
            dao.replaceExercises(current.id, if (form.sport == com.ridvan.target.data.local.entity.Sport.GYM) form.exercises else emptyList())
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = workout.value ?: return
        viewModelScope.launch {
            dao.delete(current)
            onDone()
        }
    }
}
