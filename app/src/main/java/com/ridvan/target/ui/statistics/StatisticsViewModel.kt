package com.ridvan.target.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.PracticeExamEntryWithContext
import com.ridvan.target.data.local.dao.PracticeExamTopicAggregate
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

enum class StatsSource { PRACTICE_SESSIONS, PRACTICE_EXAMS }

data class ChartBucket(val label: String, val solved: Int, val unsolved: Int)

data class StatsSummary(
    val tests: Int,
    val solved: Int,
    val unsolved: Int,
    val blank: Int,
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
    val blank: Int,
    val durationMinutes: Int,
)

data class WeakTopicEntry(
    val topicId: Long,
    val topicName: String,
    val courseName: String,
    val correctCount: Int,
    val wrongCount: Int,
) {
    val total: Int get() = correctCount + wrongCount
    val accuracyPercent: Int get() = if (total == 0) 0 else correctCount * 100 / total
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userId = targetApplication.preferences.currentUserId
    private val examDao = targetApplication.database.examDao()
    private val examTypeDao = targetApplication.database.examTypeDao()
    private val courseDao = targetApplication.database.courseDao()
    private val examCourseDao = targetApplication.database.examCourseDao()
    private val topicDao = targetApplication.database.topicDao()
    private val practiceLogDao = targetApplication.database.practiceLogDao()
    private val practiceExamEntryDao = targetApplication.database.practiceExamEntryDao()
    private val practiceExamEntryTopicResultDao = targetApplication.database.practiceExamEntryTopicResultDao()

    private val allLogs: StateFlow<List<PracticeLogWithTopicContext>> =
        (userId?.let { practiceLogDao.getAllForUser(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allExamEntries: StateFlow<List<PracticeExamEntryWithContext>> =
        (userId?.let { practiceExamEntryDao.getAllForUser(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allWeakTopics: StateFlow<List<PracticeExamTopicAggregate>> =
        (userId?.let { practiceExamEntryTopicResultDao.getTopicAggregatesForUser(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val examsWithType: StateFlow<List<ExamWithType>> =
        (userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val examTypeOrder: StateFlow<List<String>> = examTypeDao.getAll()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val examGroups: StateFlow<List<Pair<String, List<Exam>>>> = combine(examsWithType, examTypeOrder) { withType, order ->
        val byType = withType.groupBy({ it.examTypeName }, { it.exam })
        order.mapNotNull { typeName -> byType[typeName]?.let { typeName to it } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    private val _source = MutableStateFlow(StatsSource.PRACTICE_SESSIONS)
    val source: StateFlow<StatsSource> = _source.asStateFlow()

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
        val windowed = windowForPeriod(logs, period) { it.practiceLog.loggedAt }
        StatsSummary(
            tests = windowed.sumOf { it.practiceLog.testsSolved },
            solved = windowed.sumOf { it.practiceLog.solvedCount },
            unsolved = windowed.sumOf { it.practiceLog.unsolvedCount },
            blank = windowed.sumOf { row ->
                val qc = row.practiceLog.questionCount
                if (qc != null) (qc - row.practiceLog.solvedCount - row.practiceLog.unsolvedCount).coerceAtLeast(0) else 0
            },
            durationMinutes = windowed.sumOf { it.practiceLog.durationMinutes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSummary(0, 0, 0, 0, 0))

    val chartBuckets: StateFlow<List<ChartBucket>> = combine(filteredLogs, _period) { logs, period ->
        buildBuckets(
            rows = logs,
            period = period,
            timeOf = { it.practiceLog.loggedAt },
            solvedOf = { it.practiceLog.solvedCount },
            unsolvedOf = { it.practiceLog.unsolvedCount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val breakdown: StateFlow<List<TopicBreakdownEntry>> = combine(filteredLogs, _period, allCourses) { logs, period, courses ->
        val courseNames = courses.associateBy({ it.id }, { it.name })
        windowForPeriod(logs, period) { it.practiceLog.loggedAt }.groupBy { it.topicId }.map { (topicId, rows) ->
            TopicBreakdownEntry(
                topicId = topicId,
                topicName = rows.first().topicName,
                courseName = courseNames[rows.first().courseId].orEmpty(),
                tests = rows.sumOf { it.practiceLog.testsSolved },
                solved = rows.sumOf { it.practiceLog.solvedCount },
                unsolved = rows.sumOf { it.practiceLog.unsolvedCount },
                blank = rows.sumOf { row ->
                    val qc = row.practiceLog.questionCount
                    if (qc != null) (qc - row.practiceLog.solvedCount - row.practiceLog.unsolvedCount).coerceAtLeast(0) else 0
                },
                durationMinutes = rows.sumOf { it.practiceLog.durationMinutes },
            )
        }.sortedByDescending { it.solved + it.unsolved }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filteredExamEntries: StateFlow<List<PracticeExamEntryWithContext>> = combine(
        allExamEntries, _selectedExamId, _selectedCourseId, examCourseIds,
    ) { entries, examId, courseId, examIds ->
        entries.filter { row ->
            (examId == null || (examIds != null && row.courseId in examIds)) &&
                (courseId == null || row.courseId == courseId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val examSummary: StateFlow<StatsSummary> = combine(filteredExamEntries, _period) { entries, period ->
        val windowed = windowForPeriod(entries, period) { it.entry.createdAt }
        StatsSummary(
            tests = windowed.size,
            solved = windowed.sumOf { it.entry.correctCount },
            unsolved = windowed.sumOf { it.entry.wrongCount },
            blank = windowed.sumOf { (it.entry.questionCount - it.entry.correctCount - it.entry.wrongCount).coerceAtLeast(0) },
            durationMinutes = windowed.sumOf { it.entry.durationMinutes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSummary(0, 0, 0, 0, 0))

    val examChartBuckets: StateFlow<List<ChartBucket>> = combine(filteredExamEntries, _period) { entries, period ->
        buildBuckets(
            rows = entries,
            period = period,
            timeOf = { it.entry.createdAt },
            solvedOf = { it.entry.correctCount },
            unsolvedOf = { it.entry.wrongCount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weakTopics: StateFlow<List<WeakTopicEntry>> = combine(
        allWeakTopics, _selectedExamId, _selectedCourseId, examCourseIds,
    ) { aggregates, examId, courseId, examIds ->
        aggregates
            .filter { row ->
                (examId == null || (examIds != null && row.courseId in examIds)) &&
                    (courseId == null || row.courseId == courseId)
            }
            .map {
                WeakTopicEntry(
                    topicId = it.topicId,
                    topicName = it.topicName,
                    courseName = it.courseName,
                    correctCount = it.totalCorrectCount,
                    wrongCount = it.totalWrongCount,
                )
            }
            .sortedBy { it.accuracyPercent }
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

    fun setSource(newSource: StatsSource) {
        _source.value = newSource
    }

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

    private fun <T> windowForPeriod(rows: List<T>, period: StatsPeriod, timeOf: (T) -> Long): List<T> {
        if (period == StatsPeriod.ALL_TIME) return rows
        val startMillis = windowStartMillis(period)
        return rows.filter { timeOf(it) >= startMillis }
    }

    private fun <T> buildBuckets(
        rows: List<T>,
        period: StatsPeriod,
        timeOf: (T) -> Long,
        solvedOf: (T) -> Int,
        unsolvedOf: (T) -> Int,
    ): List<ChartBucket> {
        val windowed = windowForPeriod(rows, period, timeOf)
        val cal = Calendar.getInstance()
        val grouped = windowed.groupBy { row ->
            cal.timeInMillis = timeOf(row)
            startOf(cal, period).timeInMillis
        }

        val cursor = if (period == StatsPeriod.ALL_TIME) {
            val earliest = windowed.minOfOrNull { timeOf(it) } ?: System.currentTimeMillis()
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
            val rowsInBucket = grouped[key].orEmpty()
            ChartBucket(
                label = bucketLabel(key, period),
                solved = rowsInBucket.sumOf(solvedOf),
                unsolved = rowsInBucket.sumOf(unsolvedOf),
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
