package com.ridvan.target.ui.focustimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.focustimer.FocusAlarmPlayer
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FocusPhase { WORK, BREAK }

/**
 * In-memory only — not persisted to Room while running (see CLAUDE.md's Focus Timer scope:
 * foreground-only was the explicit choice, so losing an in-progress run to OS process death is
 * accepted). phaseEndAt is a wall-clock instant, not a decrementing counter, so a brief
 * app-switch-away-and-back still reports the correct remaining time on return.
 */
data class RunningFocusSession(
    val preset: FocusPreset,
    /** Exactly one of courseId/languageId is set, or neither. */
    val courseId: Long?,
    val courseName: String?,
    val languageId: Long?,
    val languageName: String?,
    val topicId: Long?,
    val topicName: String?,
    val phase: FocusPhase,
    val phaseEndAt: Long,
    val isPaused: Boolean,
    val pausedRemainingMillis: Long,
    val cyclesCompleted: Int,
    val startedAt: Long,
)

class FocusTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val focusPresetDao = targetApplication.database.focusPresetDao()
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val courseDao = targetApplication.database.courseDao()
    private val languageDao = targetApplication.database.languageDao()
    private val topicDao = targetApplication.database.topicDao()
    private val appPreferences = targetApplication.preferences
    private val userId = targetApplication.preferences.currentUserId
    private val appContext = application.applicationContext

    val presets: StateFlow<List<FocusPreset>> = (userId?.let { focusPresetDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<FocusSession>> = (userId?.let { focusSessionDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val focusAlarmSoundUri: StateFlow<String?> = appPreferences.focusAlarmSoundUri
    val focusVibrationEnabled: StateFlow<Boolean> = appPreferences.focusVibrationEnabled

    fun setFocusAlarmSoundUri(uri: String?) = appPreferences.setFocusAlarmSoundUri(uri)
    fun setFocusVibrationEnabled(enabled: Boolean) = appPreferences.setFocusVibrationEnabled(enabled)

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)
    fun topicsForLanguage(languageId: Long): Flow<List<Topic>> = topicDao.getByLanguageId(languageId)

    private val _runningSession = MutableStateFlow<RunningFocusSession?>(null)
    val runningSession: StateFlow<RunningFocusSession?> = _runningSession.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var tickerJob: Job? = null
    private var lastTickAt = 0L
    private var accumulatedWorkMillis = 0L
    private var accumulatedBreakMillis = 0L

    fun startSession(
        preset: FocusPreset,
        courseId: Long?,
        courseName: String?,
        languageId: Long?,
        languageName: String?,
        topicId: Long?,
        topicName: String?,
    ) {
        val now = System.currentTimeMillis()
        accumulatedWorkMillis = 0L
        accumulatedBreakMillis = 0L
        lastTickAt = now
        _runningSession.value = RunningFocusSession(
            preset = preset,
            courseId = courseId,
            courseName = courseName,
            languageId = languageId,
            languageName = languageName,
            topicId = topicId,
            topicName = topicName,
            phase = FocusPhase.WORK,
            phaseEndAt = now + preset.workMinutes * 60_000L,
            isPaused = false,
            pausedRemainingMillis = 0L,
            cyclesCompleted = 0,
            startedAt = now,
        )
        _remainingMillis.value = preset.workMinutes * 60_000L
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                tick()
                delay(1_000)
            }
        }
    }

    private fun tick() {
        val session = _runningSession.value ?: return
        val now = System.currentTimeMillis()
        if (session.isPaused) {
            _remainingMillis.value = session.pausedRemainingMillis
            lastTickAt = now
            return
        }
        val delta = (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
        when (session.phase) {
            FocusPhase.WORK -> accumulatedWorkMillis += delta
            FocusPhase.BREAK -> accumulatedBreakMillis += delta
        }
        val remaining = session.phaseEndAt - now
        if (remaining <= 0) {
            FocusAlarmPlayer.playAlarm(appContext, focusAlarmSoundUri.value, focusVibrationEnabled.value)
            val nextPhase = if (session.phase == FocusPhase.WORK) FocusPhase.BREAK else FocusPhase.WORK
            val nextDurationMinutes = if (nextPhase == FocusPhase.WORK) session.preset.workMinutes else session.preset.breakMinutes
            val newCycles = if (session.phase == FocusPhase.WORK) session.cyclesCompleted + 1 else session.cyclesCompleted
            _runningSession.value = session.copy(
                phase = nextPhase,
                phaseEndAt = now + nextDurationMinutes * 60_000L,
                cyclesCompleted = newCycles,
            )
            _remainingMillis.value = nextDurationMinutes * 60_000L
        } else {
            _remainingMillis.value = remaining
        }
    }

    fun pauseSession() {
        val session = _runningSession.value ?: return
        if (session.isPaused) return
        val remaining = (session.phaseEndAt - System.currentTimeMillis()).coerceAtLeast(0)
        _runningSession.value = session.copy(isPaused = true, pausedRemainingMillis = remaining)
    }

    fun resumeSession() {
        val session = _runningSession.value ?: return
        if (!session.isPaused) return
        val now = System.currentTimeMillis()
        lastTickAt = now
        _runningSession.value = session.copy(isPaused = false, phaseEndAt = now + session.pausedRemainingMillis)
    }

    fun stopSession() {
        val session = _runningSession.value ?: return
        tickerJob?.cancel()
        tickerJob = null
        persistSession(session, System.currentTimeMillis())
        _runningSession.value = null
        _remainingMillis.value = 0L
    }

    private fun persistSession(session: RunningFocusSession, endedAt: Long) {
        if (userId == null) return
        val focusSession = FocusSession(
            userId = userId,
            presetId = session.preset.id,
            presetName = session.preset.name,
            workMinutes = session.preset.workMinutes,
            breakMinutes = session.preset.breakMinutes,
            courseId = session.courseId,
            languageId = session.languageId,
            topicId = session.topicId,
            startedAt = session.startedAt,
            endedAt = endedAt,
            cyclesCompleted = session.cyclesCompleted,
            totalWorkMinutes = (accumulatedWorkMillis / 60_000L).toInt(),
            totalBreakMinutes = (accumulatedBreakMillis / 60_000L).toInt(),
        )
        // Fired from stopSession() (still on viewModelScope) or from onCleared() (where
        // viewModelScope is already cancelled) — a fresh scope works for both and matches the
        // existing "one-off IO launch outside any lifecycle" convention (AppPreferences's
        // rescheduleNotifications), rather than needing two different code paths here.
        CoroutineScope(Dispatchers.IO).launch { focusSessionDao.insert(focusSession) }
    }

    fun deleteHistorySession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.delete(session) }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        // Navigating away from this screen (even just the system Back button) destroys this
        // ViewModel — there is no foreground service keeping the timer alive regardless, so a
        // running session would otherwise be silently discarded with nothing recorded. Save
        // what was accumulated so far as a completed history entry instead of losing it.
        _runningSession.value?.let { persistSession(it, System.currentTimeMillis()) }
    }
}
