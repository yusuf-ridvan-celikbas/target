package com.ridvan.target.ui.mockexam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.focustimer.FocusAlarmPlayer
import com.ridvan.target.data.focustimer.FocusSoundEvent
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamStatus
import com.ridvan.target.ui.focustimer.GetReadyCountdown
import com.ridvan.target.ui.navigation.MockExamTimerRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Non-stop countdown for a combined mock exam. Wall-clock based on [MockExam.takenAt], so leaving the
 * screen (or the process dying) never loses time; reopening resumes, or finishes it if time already ran out.
 */
class MockExamTimerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val mockExamId: Long = savedStateHandle.toRoute<MockExamTimerRoute>().mockExamId
    private val app = application as TargetApplication
    private val mockExamDao = app.database.mockExamDao()

    val mockExam: StateFlow<MockExam?> = mockExamDao.getById(mockExamId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val parts: StateFlow<List<MockExamPartWithCourse>> = mockExamDao.getParts(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _remainingMillis = MutableStateFlow<Long?>(null)
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    /** Set once the deneme is finished (by time-up or by the user); the screen then opens its results. */
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val getReady = GetReadyCountdown(viewModelScope)

    /** The 10 s get-ready before a planned deneme starts; null when not counting. */
    val getReadyMillis: StateFlow<Long?> = getReady.remainingMillis

    /** Set when the get-ready is cancelled: the deneme stays planned and the screen closes. */
    private val _cancelled = MutableStateFlow(false)
    val cancelled: StateFlow<Boolean> = _cancelled.asStateFlow()

    init {
        viewModelScope.launch {
            val current = mockExamDao.getById(mockExamId).first() ?: return@launch
            when (current.status) {
                MockExamStatus.FINISHED -> _finished.value = true
                MockExamStatus.RUNNING -> runCountdown(current)
                MockExamStatus.PLANNED -> getReady.start {
                    viewModelScope.launch {
                        val started = current.copy(takenAt = System.currentTimeMillis(), status = MockExamStatus.RUNNING)
                        mockExamDao.update(started)
                        runCountdown(started)
                    }
                }
            }
        }
    }

    fun cancelGetReady() {
        getReady.cancel()
        _cancelled.value = true
    }

    private suspend fun runCountdown(current: MockExam) {
        val endAt = current.takenAt + current.timeLimitMinutes * 60_000L
        while (true) {
            val remaining = endAt - System.currentTimeMillis()
            if (remaining <= 0) {
                // Only ring when time-up happens now, not when reopening long after.
                if (remaining > -60_000L) playTimeUp()
                finish(current, current.timeLimitMinutes)
                break
            }
            _remainingMillis.value = remaining
            delay(250)
        }
    }

    fun finishEarly() {
        viewModelScope.launch {
            val current = mockExam.value ?: return@launch
            if (current.status != MockExamStatus.RUNNING) return@launch
            val elapsed = ((System.currentTimeMillis() - current.takenAt) / 60_000.0).roundToInt()
            finish(current, elapsed.coerceIn(1, current.timeLimitMinutes))
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            mockExam.value?.let { mockExamDao.delete(it) }
            onDone()
        }
    }

    private suspend fun finish(current: MockExam, durationMinutes: Int) {
        mockExamDao.update(
            current.copy(durationMinutes = durationMinutes, finishedAt = System.currentTimeMillis(), status = MockExamStatus.FINISHED)
        )
        _finished.value = true
    }

    private fun playTimeUp() {
        val prefs = app.preferences
        FocusAlarmPlayer.playAlarm(
            app,
            prefs.focusSoundUris.value[FocusSoundEvent.WORK_END],
            prefs.focusVibrationEnabled.value,
        )
    }
}
