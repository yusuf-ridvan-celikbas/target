package com.ridvan.target.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.GoalLog
import com.ridvan.target.data.local.entity.GoalMilestone
import com.ridvan.target.data.local.entity.HabitPeriod
import com.ridvan.target.data.local.entity.GoalSource
import com.ridvan.target.ui.common.startOfTodayMillis
import com.ridvan.target.ui.navigation.GoalDetailRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A goal with everything its row needs. */
data class GoalItem(val goal: Goal, val progress: GoalProgress, val pace: GoalPace?, val habitStreak: Int)

/** What the goal form returns; [milestones] only matter for a new MILESTONES goal. */
data class GoalForm(
    val name: String,
    val kind: GoalKind,
    val unit: String?,
    val targetValue: Double?,
    val startValue: Double?,
    val period: HabitPeriod?,
    val source: GoalSource?,
    val startDate: Long,
    val deadline: Long?,
    val notes: String?,
    val milestones: List<String> = emptyList(),
)

fun GoalForm.applyTo(goal: Goal): Goal = goal.copy(
    name = name, unit = unit, targetValue = targetValue, startValue = startValue, period = period,
    source = source, startDate = startDate, deadline = deadline, notes = notes,
)

/** The account's data auto-filled goals count from. */
fun goalSourceDataFlow(app: TargetApplication, userId: Long?): Flow<GoalSourceData> {
    if (userId == null) return flowOf(GoalSourceData())
    val db = app.database
    return combine(
        db.focusSessionDao().getAllWithLinksByUserId(userId),
        db.practiceLogDao().getAllForPlanner(userId),
        db.mockExamDao().getSummariesForUser(userId),
        db.workoutDao().getByUserId(userId),
        db.bookDao().getByUserId(userId),
    ) { focus, practice, mocks, workouts, books ->
        GoalSourceData(focus.map { it.session }, practice.map { it.practiceLog }, mocks, workouts, books)
    }
}

/** Every goal of the account with its progress; open ones first (by deadline), then finished ones. */
fun goalItemsFlow(app: TargetApplication, userId: Long?): Flow<List<GoalItem>> {
    if (userId == null) return flowOf(emptyList())
    val dao = app.database.goalDao()
    return combine(dao.getByUserId(userId), dao.getAllLogsByUserId(userId), dao.getAllMilestonesByUserId(userId), goalSourceDataFlow(app, userId)) { goals, logs, milestones, data ->
        val now = System.currentTimeMillis()
        val logsBy = logs.groupBy { it.goalId }
        val milestonesBy = milestones.groupBy { it.goalId }
        goals.map { goal -> goalItem(goal, logsBy[goal.id].orEmpty(), milestonesBy[goal.id].orEmpty(), data, now) }
    }
}

fun goalItem(goal: Goal, logs: List<GoalLog>, milestones: List<GoalMilestone>, data: GoalSourceData, now: Long): GoalItem {
    val progress = goalProgress(goal, logs, milestones, data, now)
    return GoalItem(
        goal, progress, goalPace(goal, progress, now),
        if (goal.kind == GoalKind.HABIT) habitStreak(goal, logs, now) else 0,
    )
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val dao = app.database.goalDao()
    private val userId = app.preferences.currentUserId

    val goals: StateFlow<List<GoalItem>> = goalItemsFlow(app, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(form: GoalForm) {
        val uid = userId ?: return
        val goal = form.applyTo(Goal(userId = uid, name = form.name, kind = form.kind, startDate = form.startDate))
        viewModelScope.launch { dao.insertWithMilestones(goal, form.milestones) }
    }
}

class GoalDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val app = application as TargetApplication
    private val dao = app.database.goalDao()
    val goalId: Long = savedStateHandle.toRoute<GoalDetailRoute>().goalId

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val goal: StateFlow<Goal?> = dao.getById(goalId).state(null)
    val logs: StateFlow<List<GoalLog>> = dao.getLogs(goalId).state(emptyList())
    val milestones: StateFlow<List<GoalMilestone>> = dao.getMilestones(goalId).state(emptyList())

    val item: StateFlow<GoalItem?> = combine(dao.getById(goalId), dao.getLogs(goalId), dao.getMilestones(goalId), goalSourceDataFlow(app, app.preferences.currentUserId)) { g, l, m, data ->
        g?.let { goalItem(it, l, m, data, System.currentTimeMillis()) }
    }.state(null)

    fun update(form: GoalForm) {
        val current = goal.value ?: return
        viewModelScope.launch { dao.update(form.applyTo(current)) }
    }

    fun setDone(done: Boolean) {
        val current = goal.value ?: return
        viewModelScope.launch { dao.update(current.copy(completedAt = if (done) System.currentTimeMillis() else null)) }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = goal.value ?: return
        viewModelScope.launch {
            dao.delete(current)
            onDeleted()
        }
    }

    fun saveLog(existing: GoalLog?, date: Long, amount: Double, note: String?) {
        viewModelScope.launch {
            if (existing == null) dao.insertLog(GoalLog(goalId = goalId, date = date, amount = amount, note = note))
            else dao.updateLog(existing.copy(date = date, amount = amount, note = note))
        }
    }

    /** A habit done once today. */
    fun doneToday() = saveLog(null, startOfTodayMillis(), 1.0, null)

    fun deleteLog(log: GoalLog) {
        viewModelScope.launch { dao.deleteLog(log) }
    }

    fun addMilestone(name: String) {
        viewModelScope.launch { dao.insertMilestone(GoalMilestone(goalId = goalId, name = name, orderIndex = (milestones.value.maxOfOrNull { it.orderIndex } ?: -1) + 1)) }
    }

    fun setMilestoneDone(milestone: GoalMilestone, done: Boolean) {
        viewModelScope.launch { dao.updateMilestones(listOf(milestone.copy(doneAt = if (done) System.currentTimeMillis() else null))) }
    }

    fun renameMilestone(milestone: GoalMilestone, name: String) {
        viewModelScope.launch { dao.updateMilestones(listOf(milestone.copy(name = name))) }
    }

    fun deleteMilestone(milestone: GoalMilestone) {
        viewModelScope.launch { dao.deleteMilestone(milestone) }
    }

    /** Moves a step one place up ([step] = -1) or down (+1), renumbering the list. */
    fun moveMilestone(milestone: GoalMilestone, step: Int) {
        val list = milestones.value.toMutableList()
        val from = list.indexOfFirst { it.id == milestone.id }
        val to = from + step
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { dao.updateMilestones(list.mapIndexed { index, m -> m.copy(orderIndex = index) }) }
    }
}
