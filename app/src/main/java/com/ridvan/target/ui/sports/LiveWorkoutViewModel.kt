package com.ridvan.target.ui.sports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExerciseWithSets
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.ui.focustimer.GetReadyCountdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RunningWorkout(val sport: Sport, val startedAt: Long, val isPaused: Boolean)

/** A stopped workout waiting for its distance (distance sports) or exercises (gym). */
data class FinishedWorkout(val sport: Sport, val startedAt: Long, val durationSeconds: Int)

/** Sessions shorter than this are taken to be an accidental Start and dropped. */
private const val MIN_SAVED_SECONDS = 10

/**
 * Live workout stopwatch, shaped like Flow Timer: wall-clock based, Start -> confirm -> get-ready countdown,
 * Pause/Resume, Stop. After Stop it asks for distance or exercises; leaving the screen mid-way saves what exists.
 */
class LiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as TargetApplication).database.workoutDao()
    private val userId = application.let { it as TargetApplication }.preferences.currentUserId

    private val _running = MutableStateFlow<RunningWorkout?>(null)
    val running: StateFlow<RunningWorkout?> = _running.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _finished = MutableStateFlow<FinishedWorkout?>(null)
    val finished: StateFlow<FinishedWorkout?> = _finished.asStateFlow()

    /** Set after a save so the screen can say so once. */
    private val _savedMessage = MutableStateFlow(false)
    val savedMessage: StateFlow<Boolean> = _savedMessage.asStateFlow()

    private val getReady = GetReadyCountdown(viewModelScope)
    val getReadyRemainingMillis: StateFlow<Long?> = getReady.remainingMillis

    private var tickerJob: Job? = null
    private var lastTickAt = 0L

    fun requestStart(sport: Sport) {
        if (_running.value != null || _finished.value != null) return
        getReady.start { startWorkout(sport) }
    }

    fun cancelGetReady() = getReady.cancel()

    private fun startWorkout(sport: Sport) {
        val now = System.currentTimeMillis()
        lastTickAt = now
        _elapsedMillis.value = 0L
        _running.value = RunningWorkout(sport, now, isPaused = false)
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                tick()
                delay(1_000)
            }
        }
    }

    private fun tick() {
        val workout = _running.value ?: return
        val now = System.currentTimeMillis()
        if (workout.isPaused) lastTickAt = now else accrue(now)
    }

    /** Wall-clock based, so a brief app switch still counts correctly on the next tick. */
    private fun accrue(now: Long) {
        _elapsedMillis.value += (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
    }

    fun pause() {
        val workout = _running.value ?: return
        if (workout.isPaused) return
        accrue(System.currentTimeMillis())
        _running.value = workout.copy(isPaused = true)
    }

    fun resume() {
        val workout = _running.value ?: return
        if (!workout.isPaused) return
        lastTickAt = System.currentTimeMillis()
        _running.value = workout.copy(isPaused = false)
    }

    fun stop() {
        val workout = _running.value ?: return
        if (!workout.isPaused) accrue(System.currentTimeMillis())
        tickerJob?.cancel()
        tickerJob = null
        val seconds = (_elapsedMillis.value / 1000).toInt()
        _running.value = null
        _elapsedMillis.value = 0L
        if (seconds < MIN_SAVED_SECONDS) return
        val finished = FinishedWorkout(workout.sport, workout.startedAt, seconds)
        if (workout.sport.isDistance || workout.sport == Sport.GYM) {
            _finished.value = finished
        } else {
            save(finished, null, emptyList())
        }
    }

    /** Saves the stopped workout with whatever the user entered (nothing is fine). */
    fun finish(distanceMeters: Int?, exercises: List<ExerciseWithSets>) {
        val finished = _finished.value ?: return
        _finished.value = null
        save(finished, distanceMeters, exercises)
    }

    fun acknowledgeSaved() { _savedMessage.value = false }

    private fun save(finished: FinishedWorkout, distanceMeters: Int?, exercises: List<ExerciseWithSets>) {
        val uid = userId ?: return
        val workout = Workout(
            userId = uid,
            sport = finished.sport,
            startedAt = finished.startedAt,
            durationSeconds = finished.durationSeconds,
            distanceMeters = distanceMeters.takeIf { finished.sport.isDistance },
            isLive = true,
        )
        // Fresh scope: also used from onCleared(), where viewModelScope is already cancelled.
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertWithExercises(workout, if (finished.sport == Sport.GYM) exercises else emptyList())
            _savedMessage.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        // Leaving via Back destroys this ViewModel — keep what was counted rather than losing it.
        _finished.value?.let { save(it, null, emptyList()) }
        _running.value?.let { workout ->
            if (!workout.isPaused) accrue(System.currentTimeMillis())
            val seconds = (_elapsedMillis.value / 1000).toInt()
            if (seconds >= MIN_SAVED_SECONDS) save(FinishedWorkout(workout.sport, workout.startedAt, seconds), null, emptyList())
        }
    }
}
