package com.ridvan.target.ui.focustimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.focustimer.FocusAlarmPlayer
import com.ridvan.target.data.focustimer.FocusSoundEvent
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.reading.BookProgress
import com.ridvan.target.ui.reading.bookProgressFlow
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
import com.ridvan.target.ui.common.launchCreateTopic

/** What a Flow session is linked to, chosen before it starts (book for reading, else course/language + topic). */
data class FlowSessionLink(
    val courseId: Long? = null,
    val courseName: String? = null,
    val languageId: Long? = null,
    val languageName: String? = null,
    val topicId: Long? = null,
    val topicName: String? = null,
    val bookId: Long? = null,
    val bookTitle: String? = null,
    val startPage: Int? = null,
    val totalPages: Int? = null,
)

/** In-memory only, like RunningFocusSession: foreground-only by design, saved on Stop or when torn down. */
data class RunningFlowSession(
    val link: FlowSessionLink,
    val phase: FocusPhase,
    val isPaused: Boolean,
    val breaksTaken: Int,
    val startedAt: Long,
)

/** The live clocks, refreshed every tick: the current stretch, and the session's work/break totals. */
data class FlowClock(val phaseMillis: Long = 0L, val workMillis: Long = 0L, val breakMillis: Long = 0L)

/**
 * Flow Timer: counts up until the user stops it. Work (or reading) runs until the user takes a
 * break; a break counts up too, until "Back to work". No rounds, no prompts, no auto-ending.
 */
class FlowTimerViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val courseDao = targetApplication.database.courseDao()
    private val languageDao = targetApplication.database.languageDao()
    private val topicDao = targetApplication.database.topicDao()
    private val bookDao = targetApplication.database.bookDao()
    private val appPreferences = targetApplication.preferences
    private val userId = appPreferences.currentUserId
    private val appContext = application.applicationContext

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val books: StateFlow<List<BookProgress>> = bookProgressFlow(bookDao, focusSessionDao, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Last 7 calendar days, same window as Focus Timer's card. */
    val recentHistory: StateFlow<List<FocusSessionWithLinks>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .map { items ->
            val cutoff = startOfTodayMillis() - 6 * 24L * 60 * 60 * 1000
            items.filter { it.session.startedAt >= cutoff }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)
    fun topicsForLanguage(languageId: Long): Flow<List<Topic>> = topicDao.getByLanguageId(languageId)

    fun createTopic(name: String, courseId: Long?, languageId: Long?, onCreated: (Long) -> Unit) =
        viewModelScope.launchCreateTopic(topicDao, name, courseId, languageId, onCreated)

    private val _runningSession = MutableStateFlow<RunningFlowSession?>(null)
    val runningSession: StateFlow<RunningFlowSession?> = _runningSession.asStateFlow()

    private val _clock = MutableStateFlow(FlowClock())
    val clock: StateFlow<FlowClock> = _clock.asStateFlow()

    private val _pendingPageEntry = MutableStateFlow<PendingPageEntry?>(null)
    val pendingPageEntry: StateFlow<PendingPageEntry?> = _pendingPageEntry.asStateFlow()

    private val getReady = GetReadyCountdown(viewModelScope)
    val getReadyRemainingMillis: StateFlow<Long?> = getReady.remainingMillis

    private var tickerJob: Job? = null
    private var lastTickAt = 0L

    private fun playSound(event: FocusSoundEvent) {
        FocusAlarmPlayer.playAlarm(appContext, appPreferences.focusSoundUris.value[event], false)
    }

    /** Start was confirmed in the summary pop-up: run the get-ready countdown, then begin counting. */
    fun requestStart(link: FlowSessionLink) {
        if (_runningSession.value != null) return
        getReady.start { startSession(link) }
    }

    fun cancelGetReady() = getReady.cancel()

    private fun startSession(link: FlowSessionLink) {
        val now = System.currentTimeMillis()
        lastTickAt = now
        _clock.value = FlowClock()
        _runningSession.value = RunningFlowSession(
            link = link,
            phase = FocusPhase.WORK,
            isPaused = false,
            breaksTaken = 0,
            startedAt = now,
        )
        playSound(FocusSoundEvent.WORK_START)
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
            lastTickAt = now
            return
        }
        accrue(session.phase, now)
    }

    /** Wall-clock based, so a brief app switch still counts correctly on the next tick. */
    private fun accrue(phase: FocusPhase, now: Long) {
        val delta = (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
        val clock = _clock.value
        _clock.value = when (phase) {
            FocusPhase.WORK -> clock.copy(phaseMillis = clock.phaseMillis + delta, workMillis = clock.workMillis + delta)
            FocusPhase.BREAK -> clock.copy(phaseMillis = clock.phaseMillis + delta, breakMillis = clock.breakMillis + delta)
        }
    }

    /** "Take a break" / "Back to work": the other clock starts counting up from zero. */
    fun switchPhase() {
        val session = _runningSession.value ?: return
        val now = System.currentTimeMillis()
        if (!session.isPaused) accrue(session.phase, now)
        lastTickAt = now
        val next = if (session.phase == FocusPhase.WORK) FocusPhase.BREAK else FocusPhase.WORK
        _runningSession.value = session.copy(
            phase = next,
            isPaused = false,
            breaksTaken = if (next == FocusPhase.BREAK) session.breaksTaken + 1 else session.breaksTaken,
        )
        _clock.value = _clock.value.copy(phaseMillis = 0L)
        playSound(if (next == FocusPhase.BREAK) FocusSoundEvent.BREAK_START else FocusSoundEvent.WORK_START)
    }

    fun pauseSession() {
        val session = _runningSession.value ?: return
        if (session.isPaused) return
        accrue(session.phase, System.currentTimeMillis())
        _runningSession.value = session.copy(isPaused = true)
    }

    fun resumeSession() {
        val session = _runningSession.value ?: return
        if (!session.isPaused) return
        lastTickAt = System.currentTimeMillis()
        _runningSession.value = session.copy(isPaused = false)
    }

    fun stopSession() {
        val session = _runningSession.value ?: return
        val now = System.currentTimeMillis()
        if (!session.isPaused) accrue(session.phase, now)
        tickerJob?.cancel()
        tickerJob = null
        persistSession(session, _clock.value, now, askForPage = true)
        _runningSession.value = null
        _clock.value = FlowClock()
    }

    private fun persistSession(session: RunningFlowSession, clock: FlowClock, endedAt: Long, askForPage: Boolean) {
        if (userId == null) return
        val link = session.link
        val row = FocusSession(
            userId = userId,
            presetName = "",
            workMinutes = 0,
            breakMinutes = 0,
            courseId = link.courseId,
            languageId = link.languageId,
            topicId = link.topicId,
            startedAt = session.startedAt,
            endedAt = endedAt,
            cyclesCompleted = session.breaksTaken,
            totalWorkMinutes = (clock.workMillis / 60_000L).toInt(),
            totalBreakMinutes = (clock.breakMillis / 60_000L).toInt(),
            bookId = link.bookId,
            startPage = link.startPage,
            isFlow = true,
        )
        // Fresh scope: also used from onCleared(), where viewModelScope is already cancelled.
        CoroutineScope(Dispatchers.IO).launch {
            val id = focusSessionDao.insert(row)
            link.bookId?.let { bookDao.clearWantToRead(it) }
            if (askForPage && link.bookId != null) {
                _pendingPageEntry.value = PendingPageEntry(id, link.bookId, link.bookTitle, link.startPage ?: 0, link.totalPages)
            }
        }
    }

    fun savePageReached(entry: PendingPageEntry, startPage: Int, endPage: Int) {
        _pendingPageEntry.value = null
        viewModelScope.launch {
            focusSessionDao.setPages(entry.sessionId, startPage, endPage)
            if (entry.totalPages != null && endPage >= entry.totalPages) bookDao.markFinished(entry.bookId)
        }
    }

    fun dismissPageEntry() {
        _pendingPageEntry.value = null
    }

    fun addBook(book: Book, onCreated: (Long) -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch { onCreated(bookDao.insert(book.copy(userId = uid))) }
    }

    fun addManualSession(form: ManualFocusSessionForm) {
        val uid = userId ?: return
        viewModelScope.launch {
            focusSessionDao.insert(form.toNewSession(uid))
            form.bookId?.let { bookDao.clearWantToRead(it) }
            if (form.finishesBook && form.bookId != null) bookDao.markFinished(form.bookId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        // Leaving via Back destroys this ViewModel — save what was counted rather than losing it.
        _runningSession.value?.let { session ->
            val now = System.currentTimeMillis()
            if (!session.isPaused) accrue(session.phase, now)
            persistSession(session, _clock.value, now, askForPage = false)
        }
    }

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.delete(session) }
    }

    fun restoreSession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.insert(session) }
    }
}
