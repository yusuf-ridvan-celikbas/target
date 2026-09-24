package com.ridvan.target.ui.focustimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.focustimer.FocusAlarmPlayer
import com.ridvan.target.data.focustimer.FocusSoundEvent
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusPreset
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.startOfTodayMillis
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FocusPhase { WORK, BREAK }

private const val RECENT_HISTORY_DAYS_BEFORE_TODAY = 6

/** How long the "Start break?" / "Start work?" prompt waits before assuming the user left. */
const val FOCUS_CONFIRM_TIMEOUT_MILLIS = 30_000L

/** "Rest of session" cuts never shorten a work/break round below this. */
private const val MIN_ROUND_MINUTES = 1

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
    /** This session's own round lengths — start as the preset's, grow via "Rest of session" add-time.
     * The preset itself is never changed, and History still snapshots the preset's values. */
    val workMinutes: Int,
    val breakMinutes: Int,
    /** Non-null while a phase has ended and we're waiting for the user to confirm the next one. */
    val awaitingNextPhase: FocusPhase? = null,
    val promptEndsAt: Long = 0L,
    /** Full length of the current round, including any added time — the progress border's 100%. */
    val phaseDurationMillis: Long = 0L,
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

    /** Only the last 7 calendar days (today + the 6 before it) — the full list lives on FocusHistoryScreen. */
    val recentHistory: StateFlow<List<FocusSessionWithLinks>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .map { items ->
            val cutoff = startOfTodayMillis() - RECENT_HISTORY_DAYS_BEFORE_TODAY * 24L * 60 * 60 * 1000
            items.filter { it.session.startedAt >= cutoff }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Read at sound time only — editing these lives in App Settings' Focus Timer Preferences.
    private val focusSoundUris: StateFlow<Map<FocusSoundEvent, String>> = appPreferences.focusSoundUris
    private val focusVibrationEnabled: StateFlow<Boolean> = appPreferences.focusVibrationEnabled

    /** End sounds also vibrate (they're what pulls the user back); start sounds follow a tap, so they don't. */
    private fun playSound(event: FocusSoundEvent, vibrate: Boolean) {
        FocusAlarmPlayer.playAlarm(appContext, focusSoundUris.value[event], vibrate && focusVibrationEnabled.value)
    }

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)
    fun topicsForLanguage(languageId: Long): Flow<List<Topic>> = topicDao.getByLanguageId(languageId)

    private val _runningSession = MutableStateFlow<RunningFocusSession?>(null)
    val runningSession: StateFlow<RunningFocusSession?> = _runningSession.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _promptRemainingMillis = MutableStateFlow(0L)
    val promptRemainingMillis: StateFlow<Long> = _promptRemainingMillis.asStateFlow()

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
            phaseDurationMillis = preset.workMinutes * 60_000L,
            workMinutes = preset.workMinutes,
            breakMinutes = preset.breakMinutes,
            isPaused = false,
            pausedRemainingMillis = 0L,
            cyclesCompleted = 0,
            startedAt = now,
        )
        _remainingMillis.value = preset.workMinutes * 60_000L
        playSound(FocusSoundEvent.WORK_START, vibrate = false)
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
        if (session.awaitingNextPhase != null) {
            // Waiting on "Start break?"/"Start work?" — this time counts toward neither phase.
            lastTickAt = now
            val promptRemaining = session.promptEndsAt - now
            if (promptRemaining <= 0) {
                // No answer: assume the user left the desk, end and save what was done.
                stopSession()
            } else {
                _promptRemainingMillis.value = promptRemaining
            }
            return
        }
        if (session.isPaused) {
            _remainingMillis.value = session.pausedRemainingMillis
            lastTickAt = now
            return
        }
        accrue(session.phase, now)
        val remaining = session.phaseEndAt - now
        if (remaining <= 0) {
            finishPhase(session, now)
        } else {
            _remainingMillis.value = remaining
        }
    }

    /** The user answered the prompt — start the phase it offered. */
    fun confirmNextPhase() {
        val session = _runningSession.value ?: return
        val nextPhase = session.awaitingNextPhase ?: return
        beginPhase(session, nextPhase, System.currentTimeMillis())
    }

    /** "Skip to break"/"Skip to work": end the current phase now and start the other one directly —
     * no confirmation prompt, since the user is evidently at the desk. A skipped work round still
     * counts as a completed cycle. */
    fun skipToNextPhase() {
        val session = _runningSession.value ?: return
        if (session.awaitingNextPhase != null) return
        val now = System.currentTimeMillis()
        if (!session.isPaused) accrue(session.phase, now)
        val next = if (session.phase == FocusPhase.WORK) FocusPhase.BREAK else FocusPhase.WORK
        val cycles = if (session.phase == FocusPhase.WORK) session.cyclesCompleted + 1 else session.cyclesCompleted
        beginPhase(session.copy(cyclesCompleted = cycles), next, now)
    }

    /**
     * Adds [minutes] to the running round, or cuts it when negative. A cut larger than the time
     * left ends the round right away, through the normal "Start break?/Start work?" prompt. With
     * [keepForRestOfSession], every later round of the same phase in this session changes by the
     * same amount too, never below [MIN_ROUND_MINUTES] (the preset itself is untouched).
     */
    fun adjustTime(minutes: Int, keepForRestOfSession: Boolean) {
        val session = _runningSession.value ?: return
        if (minutes == 0 || session.awaitingNextPhase != null) return
        val now = System.currentTimeMillis()
        val deltaMillis = minutes * 60_000L
        val currentRemaining = if (session.isPaused) session.pausedRemainingMillis else (session.phaseEndAt - now).coerceAtLeast(0)
        val newRemaining = (currentRemaining + deltaMillis).coerceAtLeast(0)
        // Shift the round's total by the same amount the remaining time actually moved, so the
        // progress border stays consistent with the elapsed part.
        val newDuration = (session.phaseDurationMillis + (newRemaining - currentRemaining)).coerceAtLeast(newRemaining)
        var updated = if (session.isPaused) {
            session.copy(pausedRemainingMillis = newRemaining, phaseDurationMillis = newDuration)
        } else {
            session.copy(phaseEndAt = now + newRemaining, phaseDurationMillis = newDuration)
        }
        if (keepForRestOfSession) {
            updated = when (session.phase) {
                FocusPhase.WORK -> updated.copy(workMinutes = (updated.workMinutes + minutes).coerceAtLeast(MIN_ROUND_MINUTES))
                FocusPhase.BREAK -> updated.copy(breakMinutes = (updated.breakMinutes + minutes).coerceAtLeast(MIN_ROUND_MINUTES))
            }
        }
        if (newRemaining == 0L) {
            if (!session.isPaused) accrue(session.phase, now)
            finishPhase(updated.copy(isPaused = false), now)
        } else {
            _runningSession.value = updated
            _remainingMillis.value = newRemaining
        }
    }

    /** The round is over: play its end sound, count the cycle, and wait for the user to confirm the next one. */
    private fun finishPhase(session: RunningFocusSession, now: Long) {
        playSound(if (session.phase == FocusPhase.WORK) FocusSoundEvent.WORK_END else FocusSoundEvent.BREAK_END, vibrate = true)
        val nextPhase = if (session.phase == FocusPhase.WORK) FocusPhase.BREAK else FocusPhase.WORK
        val newCycles = if (session.phase == FocusPhase.WORK) session.cyclesCompleted + 1 else session.cyclesCompleted
        _runningSession.value = session.copy(
            cyclesCompleted = newCycles,
            awaitingNextPhase = nextPhase,
            promptEndsAt = now + FOCUS_CONFIRM_TIMEOUT_MILLIS,
        )
        _remainingMillis.value = 0L
        _promptRemainingMillis.value = FOCUS_CONFIRM_TIMEOUT_MILLIS
    }

    private fun beginPhase(session: RunningFocusSession, phase: FocusPhase, now: Long) {
        val durationMillis = (if (phase == FocusPhase.WORK) session.workMinutes else session.breakMinutes) * 60_000L
        lastTickAt = now
        _runningSession.value = session.copy(
            phase = phase,
            phaseEndAt = now + durationMillis,
            phaseDurationMillis = durationMillis,
            isPaused = false,
            pausedRemainingMillis = 0L,
            awaitingNextPhase = null,
            promptEndsAt = 0L,
        )
        _remainingMillis.value = durationMillis
        playSound(if (phase == FocusPhase.WORK) FocusSoundEvent.WORK_START else FocusSoundEvent.BREAK_START, vibrate = false)
    }

    private fun accrue(phase: FocusPhase, now: Long) {
        val delta = (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
        when (phase) {
            FocusPhase.WORK -> accumulatedWorkMillis += delta
            FocusPhase.BREAK -> accumulatedBreakMillis += delta
        }
    }

    fun pauseSession() {
        val session = _runningSession.value ?: return
        if (session.isPaused || session.awaitingNextPhase != null) return
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
        _promptRemainingMillis.value = 0L
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
