package com.ridvan.target.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.PracticeLogWithTopicContext
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsPeriod { DAILY, WEEKLY, MONTHLY, ALL_TIME }

data class ChartBucket(val label: String, val solved: Int, val unsolved: Int)

data class StatsSummary(
    val tests: Int,
    val solved: Int,
    val unsolved: Int,
    val durationMinutes: Int,
) {
    val totalQuestions: Int get() = solved + unsolved
    val net: Double get() = solved - unsolved / 4.0
    val accuracyPercent: Int get() = if (totalQuestions == 0) 0 else solved * 100 / totalQuestions
    val minutesPerQuestion: Double get() = if (totalQuestions == 0) 0.0 else durationMinutes.toDouble() / totalQuestions
    val minutesPerTest: Double get() = if (tests == 0) 0.0 else durationMinutes.toDouble() / tests
}

data class TopicBreakdownEntry(
    val topicId: Long,
    val topicName: String,
    val courseName: String,
    val tests: Int,
    val solved: Int,
    val unsolved: Int,
    val durationMinutes: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userId = targetApplication.preferences.currentUserId
    private val examDao = targetApplication.database.examDao()
    private val courseDao = targetApplication.database.courseDao()
    private val examCourseDao = targetApplication.database.examCourseDao()
    private val topicDao = targetApplication.database.topicDao()
    private val practiceLogDao = targetApplication.database.practiceLogDao()

    private val allLogs: StateFlow<List<PracticeLogWithTopicContext>> =
        (userId?.let { practiceLogDao.getAllForUser(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exams: StateFlow<List<Exam>> = (userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()))
        .map { list -> list.map { it.exam } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allCourses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedExamId = MutableStateFlow<Long?>(null)
    val selectedExamId: StateFlow<Long?> = _selectedExamId.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    val selectedCourseId: StateFlow<Long?> = _selectedCourseId.asStateFlow()

    private val _selectedTopicId = MutableStateFlow<Long?>(null)
    val selectedTopicId: StateFlow<Long?> = _selectedTopicId.asStateFlow()

    private val _period = MutableStateFlow(StatsPeriod.ALL_TIME)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    private val examCourseIds: StateFlow<Set<Long>?> = _selectedExamId.flatMapLatest { examId ->
        if (examId == null) flowOf(null) else examCourseDao.getByExamId(examId).map { list -> list.map { it.examCourse.courseId }.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coursesForFilter: StateFlow<List<Course>> = combine(allCourses, examCourseIds) { courses, ids ->
        if (ids == null) courses else courses.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topicsForFilter: StateFlow<List<Topic>> = _selectedCourseId.flatMapLatest { courseId ->
        courseId?.let { topicDao.getByCourseId(it) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filteredLogs: StateFlow<List<PracticeLogWithTopicContext>> = combine(
        allLogs, _selectedExamId, _selectedCourseId, _selectedTopicId, examCourseIds,
    ) { logs, examId, courseId, topicId, examIds ->
        logs.filter { row ->
            (examId == null || (examIds != null && row.courseId in examIds)) &&
                (courseId == null || row.courseId == courseId) &&
                (topicId == null || row.topicId == topicId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<StatsSummary> = combine(filteredLogs, _period) { logs, period ->
        summarize(windowForPeriod(logs, period))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSummary(0, 0, 0, 0))

    val chartBuckets: StateFlow<List<ChartBucket>> = combine(filteredLogs, _period) { logs, period ->
        buildBuckets(logs, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val breakdown: StateFlow<List<TopicBreakdownEntry>> = combine(filteredLogs, _period, allCourses) { logs, period, courses ->
        val courseNames = courses.associateBy({ it.id }, { it.name })
        windowForPeriod(logs, period).groupBy { it.topicId }.map { (topicId, rows) ->
            TopicBreakdownEntry(
                topicId = topicId,
                topicName = rows.first().topicName,
                courseName = courseNames[rows.first().courseId].orEmpty(),
                tests = rows.sumOf { it.practiceLog.testsSolved },
                solved = rows.sumOf { it.practiceLog.solvedCount },
                unsolved = rows.sumOf { it.practiceLog.unsolvedCount },
                durationMinutes = rows.sumOf { it.practiceLog.durationMinutes },
            )
        }.sortedByDescending { it.solved + it.unsolved }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setExam(id: Long?) {
        _selectedExamId.value = id
        _selectedCourseId.value = null
        _selectedTopicId.value = null
    }

    fun setCourse(id: Long?) {
        _selectedCourseId.value = id
        _selectedTopicId.value = null
    }

    fun setTopic(id: Long?) {
        _selectedTopicId.value = id
    }

    fun setPeriod(newPeriod: StatsPeriod) {
        _period.value = newPeriod
    }

    private fun summarize(rows: List<PracticeLogWithTopicContext>): StatsSummary = StatsSummary(
        tests = rows.sumOf { it.practiceLog.testsSolved },
        solved = rows.sumOf { it.practiceLog.solvedCount },
        unsolved = rows.sumOf { it.practiceLog.unsolvedCount },
        durationMinutes = rows.sumOf { it.practiceLog.durationMinutes },
    )

    private fun startOf(calendar: Calendar, period: StatsPeriod): Calendar {
        val c = calendar.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        when (period) {
            StatsPeriod.DAILY -> {}
            StatsPeriod.WEEKLY -> c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
            StatsPeriod.MONTHLY, StatsPeriod.ALL_TIME -> c.set(Calendar.DAY_OF_MONTH, 1)
        }
        return c
    }

    private fun windowStartMillis(period: StatsPeriod): Long {
        if (period == StatsPeriod.ALL_TIME) return 0L
        val start = startOf(Calendar.getInstance(), period)
        when (period) {
            StatsPeriod.DAILY -> start.add(Calendar.DAY_OF_YEAR, -13)
            StatsPeriod.WEEKLY -> start.add(Calendar.WEEK_OF_YEAR, -7)
            StatsPeriod.MONTHLY -> start.add(Calendar.MONTH, -5)
            StatsPeriod.ALL_TIME -> {}
        }
        return start.timeInMillis
    }

    private fun windowForPeriod(logs: List<PracticeLogWithTopicContext>, period: StatsPeriod): List<PracticeLogWithTopicContext> {
        if (period == StatsPeriod.ALL_TIME) return logs
        val startMillis = windowStartMillis(period)
        return logs.filter { it.practiceLog.loggedAt >= startMillis }
    }

    private fun buildBuckets(logs: List<PracticeLogWithTopicContext>, period: StatsPeriod): List<ChartBucket> {
        val windowed = windowForPeriod(logs, period)
        val cal = Calendar.getInstance()
        val grouped = windowed.groupBy { row ->
            cal.timeInMillis = row.practiceLog.loggedAt
            startOf(cal, period).timeInMillis
        }

        val cursor = if (period == StatsPeriod.ALL_TIME) {
            val earliest = windowed.minOfOrNull { it.practiceLog.loggedAt } ?: System.currentTimeMillis()
            val c = Calendar.getInstance()
            c.timeInMillis = earliest
            startOf(c, period)
        } else {
            val c = Calendar.getInstance()
            c.timeInMillis = windowStartMillis(period)
            c
        }
        val end = startOf(Calendar.getInstance(), period)

        val keys = mutableListOf<Long>()
        while (!cursor.after(end)) {
            keys.add(cursor.timeInMillis)
            when (period) {
                StatsPeriod.DAILY -> cursor.add(Calendar.DAY_OF_YEAR, 1)
                StatsPeriod.WEEKLY -> cursor.add(Calendar.WEEK_OF_YEAR, 1)
                StatsPeriod.MONTHLY, StatsPeriod.ALL_TIME -> cursor.add(Calendar.MONTH, 1)
            }
        }
        val cappedKeys = if (period == StatsPeriod.ALL_TIME) keys.takeLast(24) else keys

        return cappedKeys.map { key ->
            val rows = grouped[key].orEmpty()
            ChartBucket(
                label = bucketLabel(key, period),
                solved = rows.sumOf { it.practiceLog.solvedCount },
                unsolved = rows.sumOf { it.practiceLog.unsolvedCount },
            )
        }
    }

    private fun bucketLabel(millis: Long, period: StatsPeriod): String {
        val pattern = when (period) {
            StatsPeriod.DAILY, StatsPeriod.WEEKLY -> "d MMM"
            StatsPeriod.MONTHLY, StatsPeriod.ALL_TIME -> "MMM yy"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }
}
