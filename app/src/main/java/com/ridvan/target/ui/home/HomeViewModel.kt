package com.ridvan.target.ui.home

import com.ridvan.target.ui.sports.goalProgress
import com.ridvan.target.ui.sports.minutesOf
import com.ridvan.target.ui.planner.currentBook
import com.ridvan.target.ui.reading.BookProgress
import com.ridvan.target.ui.reading.DailyGoal
import com.ridvan.target.ui.reading.dailyGoal
import com.ridvan.target.ui.reading.dayStart
import com.ridvan.target.ui.reading.isReading
import com.ridvan.target.ui.reading.readingPace
import com.ridvan.target.ui.reading.bookProgressFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.data.home.DEFAULT_HOME_LAYOUT
import com.ridvan.target.data.home.HomeItem
import com.ridvan.target.data.home.ShortcutTarget
import com.ridvan.target.data.home.parseHomeLayout
import com.ridvan.target.data.home.serializeHomeLayout
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.routines.RoutineProgress
import com.ridvan.target.ui.routines.routineProgress
import com.ridvan.target.ui.routines.runsOn
import com.ridvan.target.ui.planner.PlannerPreviewOccurrence
import com.ridvan.target.ui.planner.StudiedPreview
import com.ridvan.target.ui.planner.studiedPreview
import com.ridvan.target.ui.planner.upcomingEventOccurrences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home's reading line; the page counts come from [StudiedPreview]'s week window. */
/** [goal] is today's share of the current book's finish goal, when it has one; [pagesPerHour] estimates its time. */
data class HomeReading(val currentBook: BookProgress?, val goal: DailyGoal? = null, val pagesPerHour: Double? = null)

/** [todayCount]/[todayMinutes] cover today's workouts; [goals] are every Sports goal with its progress. */
data class HomeSports(val todayCount: Int, val todayMinutes: Int, val goals: List<HomeGoalLine>)

data class HomeGoalLine(val goal: com.ridvan.target.data.local.entity.SportGoal, val progress: com.ridvan.target.ui.sports.GoalProgress)

/** This month in Money (null when Money has no currency yet). */
data class HomeMoney(val currency: String, val spentMinor: Long, val incomeMinor: Long, val budgetMinor: Long?)

data class TodayRoutine(val routine: com.ridvan.target.data.local.entity.Routine, val progress: RoutineProgress)

data class UpcomingEvent(
    val examId: Long,
    val sectionId: Long?,
    val label: String,
    val date: Long,
    /** Set for a planned mock exam (date = its scheduled start). */
    val mockExamId: Long? = null,
)

/** How far ahead Home's Planner preview looks — enough to matter without turning into the
 *  full Planner; a daily-recurring event could otherwise flood a wider window. */
private const val HOME_PLANNER_WINDOW_DAYS = 13L
private const val HOME_PLANNER_MAX_ITEMS = 6

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userDao = targetApplication.database.userDao()
    private val examDao = targetApplication.database.examDao()
    private val sectionDao = targetApplication.database.sectionDao()
    private val plannerEventDao = targetApplication.database.plannerEventDao()
    private val plannerEventCompletionDao = targetApplication.database.plannerEventCompletionDao()
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val practiceLogDao = targetApplication.database.practiceLogDao()
    private val preferences = targetApplication.preferences
    private val userId = preferences.currentUserId

    val currentUser: StateFlow<User?> = run {
        if (userId != null) userDao.getById(userId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The account's Home cards and shortcuts, in order. */
    val homeLayout: StateFlow<List<HomeItem>> = currentUser
        .map { parseHomeLayout(it?.homeLayout) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_HOME_LAYOUT)

    /** Names of what item shortcuts can point at (also what "Add shortcut" offers). */
    val shortcutTargets: StateFlow<Map<ShortcutTarget, List<Pair<Long, String>>>> = run {
        val db = targetApplication.database
        if (userId == null) flowOf(emptyMap())
        else combine(
            examDao.getAllWithTypeByUserId(userId),
            db.bookDao().getByUserId(userId),
            db.routineDao().getByUserId(userId),
            db.tripDao().getByUserId(userId),
        ) { exams, books, routines, trips ->
            mapOf(
                ShortcutTarget.EXAM to exams.map { it.exam.id to it.exam.name },
                ShortcutTarget.BOOK to books.map { it.id to it.title },
                ShortcutTarget.ROUTINE to routines.map { it.id to it.name },
                ShortcutTarget.TRIP to trips.map { it.id to it.name },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val money: StateFlow<HomeMoney?> = run {
        val dao = targetApplication.database.moneyDao()
        if (userId == null) flowOf(null)
        else combine(currentUser, dao.getEntries(userId), dao.getBudgets(userId), dao.getTripExpenses(userId)) { user, entries, budgets, trips ->
            val code = user?.moneyCurrency ?: return@combine null
            val key = com.ridvan.target.ui.money.monthKeyOf(System.currentTimeMillis())
            val from = com.ridvan.target.ui.money.monthStart(key)
            val to = com.ridvan.target.ui.money.monthStart(key + 1)
            val inMonth = entries.filter { it.date in from until to }
            val tripSpent = trips.filter { it.expense.date in from until to }.sumOf {
                com.ridvan.target.ui.money.tripAmountInMoneyCurrency(it.expense.amountMinor, it.tripCurrency, it.homeCurrency, it.exchangeRate, code) ?: 0L
            }
            val categoryBudgets = budgets.filter { it.categoryId != null }.associate { it.categoryId!! to it.amountMinor }
            HomeMoney(
                currency = code,
                spentMinor = inMonth.filter { it.kind == com.ridvan.target.data.local.entity.MoneyKind.EXPENSE }.sumOf { it.amountMinor } + tripSpent,
                incomeMinor = inMonth.filter { it.kind == com.ridvan.target.data.local.entity.MoneyKind.INCOME }.sumOf { it.amountMinor },
                budgetMinor = com.ridvan.target.ui.money.totalBudget(categoryBudgets, budgets.firstOrNull { it.categoryId == null }?.amountMinor),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveHomeLayout(items: List<HomeItem>) {
        val id = userId ?: return
        viewModelScope.launch { userDao.setHomeLayout(id, serializeHomeLayout(items)) }
    }

    fun resetHomeLayout() {
        val id = userId ?: return
        viewModelScope.launch { userDao.setHomeLayout(id, null) }
    }

    private val mockExamDao = targetApplication.database.mockExamDao()

    val upcomingEvents: StateFlow<List<UpcomingEvent>> = combine(
        userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { sectionDao.getByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { mockExamDao.getScheduledForUser(it) } ?: flowOf(emptyList()),
    ) { exams, sections, plannedMocks ->
        val today = startOfTodayMillis()
        val examEvents = exams
            .filter { !it.exam.hasSections && it.exam.examDate != null && it.exam.examDate >= today }
            .map { UpcomingEvent(it.exam.id, null, homeExamLabel(it), it.exam.examDate!!) }
        val examLabels = exams.associate { it.exam.id to homeExamLabel(it) }
        val sectionEvents = sections
            .filter { it.date != null && it.date >= today }
            .mapNotNull { section ->
                examLabels[section.examId]?.let { examLabel ->
                    UpcomingEvent(section.examId, section.id, "$examLabel – ${section.name}", section.date!!)
                }
            }
        val sectionNames = sections.associate { it.id to it.name }
        val mockEvents = plannedMocks
            .filter { it.scheduledAt != null && it.scheduledAt >= today }
            .mapNotNull { mock ->
                examLabels[mock.examId]?.let { examLabel ->
                    val owner = mock.sectionId?.let { sectionNames[it] }?.let { "$examLabel – $it" } ?: examLabel
                    UpcomingEvent(mock.examId, mock.sectionId, "${mock.name} · $owner", mock.scheduledAt!!, mockExamId = mock.id)
                }
            }
        (examEvents + sectionEvents + mockEvents).sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingPlannerEvents: StateFlow<List<PlannerPreviewOccurrence>> = combine(
        userId?.let { plannerEventDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { plannerEventCompletionDao.getAllByUserId(it) } ?: flowOf(emptyList()),
    ) { events, completions ->
        upcomingEventOccurrences(events, completions, HOME_PLANNER_WINDOW_DAYS, HOME_PLANNER_MAX_ITEMS)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val routineDao = targetApplication.database.routineDao()

    /** Active routines scheduled today, by start time, with how many steps are ticked. */
    val todaysRoutines: StateFlow<List<TodayRoutine>> = if (userId == null) {
        flowOf(emptyList<TodayRoutine>())
    } else {
        combine(routineDao.getByUserId(userId), routineDao.getAllStepsByUserId(userId), routineDao.getAllCompletionsByUserId(userId)) { routines, steps, completions ->
            val today = startOfTodayMillis()
            val stepsBy = steps.groupBy { it.routineId }
            routines.filter { it.runsOn(today) }
                .sortedWith(compareBy({ it.startMinuteOfDay ?: Int.MAX_VALUE }, { it.name.lowercase() }))
                .map { TodayRoutine(it, routineProgress(stepsBy[it.id].orEmpty(), completions, today)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val bookDao = targetApplication.database.bookDao()

    /** Hidden (null) until there's a book or a reading session to talk about. */
    val reading: StateFlow<HomeReading?> = combine(
        bookProgressFlow(bookDao, focusSessionDao, userId),
        userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
    ) { books, sessions ->
        if (books.isEmpty() && sessions.none { it.session.bookId != null }) {
            null
        } else {
            val book = currentBook(books, sessions)
            val today = dayStart(System.currentTimeMillis())
            val reading = sessions.map { it.session }.filter { it.isReading }
            val own = reading.filter { it.bookId == book?.book?.id }
            HomeReading(
                currentBook = book,
                goal = book?.let { dailyGoal(it.book, own, today) },
                pagesPerHour = readingPace(own, reading, today).pagesPerHour,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val studied: StateFlow<StudiedPreview> = combine(
        userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { practiceLogDao.getAllForPlanner(it) } ?: flowOf(emptyList()),
        userId?.let { mockExamDao.getSummariesForUser(it) } ?: flowOf(emptyList()),
    ) { focus, practice, mocks -> studiedPreview(focus, practice, mocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudiedPreview(emptyList(), 0, 0))

    private val workoutDao = targetApplication.database.workoutDao()

    /** Today's workouts and every goal's progress; null when there is nothing to show. */
    val sports: StateFlow<HomeSports?> = combine(
        userId?.let { workoutDao.getByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { workoutDao.getGoals(it) } ?: flowOf(emptyList()),
    ) { workouts, goals ->
        val now = System.currentTimeMillis()
        val today = workouts.filter { it.startedAt >= startOfTodayMillis() && it.startedAt < startOfTodayMillis() + 24L * 60 * 60 * 1000 }
        val lines = goals.map { HomeGoalLine(it, goalProgress(it, workouts, now)) }
        if (today.isEmpty() && lines.isEmpty()) null else HomeSports(today.size, minutesOf(today.sumOf { it.durationSeconds }), lines)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workOnNext: StateFlow<List<WorkOnNextItem>> =
        (userId?.let { targetApplication.database.studyResourceTopicDao().getWorkOnNextRows(it) } ?: flowOf(emptyList()))
            .map { rankWorkOnNext(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePlannerOccurrenceDone(occurrence: PlannerPreviewOccurrence) {
        viewModelScope.launch {
            if (occurrence.isCompleted) {
                plannerEventCompletionDao.deleteByEventAndDate(occurrence.event.id, occurrence.occurrenceDateMillis)
            } else {
                plannerEventCompletionDao.insert(
                    PlannerEventCompletion(plannerEventId = occurrence.event.id, occurrenceDate = occurrence.occurrenceDateMillis)
                )
            }
        }
    }
}

/**
 * "Goethe - A2 - German" for a Language Exam (publisher - level - language, each part
 * omitted if unset); the plain exam name for every other type. Dash-joined and
 * language-inclusive specifically for Home's Upcoming card, by explicit request — the
 * shorter `examDisplayLabel` (name + level, no language) is what ExamListScreen/
 * ExamDetailScreen show instead, since those already surface the language separately.
 */
private fun homeExamLabel(item: ExamWithType): String {
    if (item.examTypeName != LANGUAGE_EXAM_TYPE_NAME) return item.exam.name
    return listOfNotNull(
        item.exam.name,
        item.exam.level?.trim()?.takeIf { it.isNotEmpty() },
        item.languageName,
    ).joinToString(" - ")
}
