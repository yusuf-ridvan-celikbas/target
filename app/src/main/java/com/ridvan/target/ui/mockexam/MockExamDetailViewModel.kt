package com.ridvan.target.ui.mockexam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.dao.MockExamTopicResultWithTopic
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.MockExamStatus
import com.ridvan.target.data.local.entity.MockExamTopicResult
import com.ridvan.target.data.local.entity.ScoringSystem
import com.ridvan.target.data.local.entity.ScoringTest
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ridvan.target.ui.common.topicIdForName

class MockExamDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    // By argument name: backs both MockExamDetailRoute and StatsMockExamDetailRoute.
    private val mockExamId: Long = checkNotNull(savedStateHandle.get<Long>("mockExamId"))
    private val database = (application as TargetApplication).database
    private val mockExamDao = database.mockExamDao()
    private val topicDao = database.topicDao()

    val mockExam: StateFlow<MockExam?> = mockExamDao.getById(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** For an AYT-scored deneme: the latest finished TYT deneme of the same exam and its nets by test. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tytSource: StateFlow<Pair<MockExam, Map<ScoringTest, Double>>?> = mockExam
        .flatMapLatest { mock ->
            if (mock?.scoringSystem != ScoringSystem.AYT) flowOf(null)
            else mockExamDao.getLatestFinishedTyt(mock.examId).flatMapLatest { tyt ->
                if (tyt == null) flowOf(null)
                else mockExamDao.getParts(tyt.id).map { parts -> tyt to netsByTest(parts.map { it.part }) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val parts: StateFlow<List<MockExamPartWithCourse>> = mockExamDao.getParts(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Parts whose totals are counted from marked questions. */
    val countedPartIds: StateFlow<Set<Long>> = mockExamDao.getCountedPartIds(mockExamId)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val topicResults: StateFlow<List<MockExamTopicResultWithTopic>> = mockExamDao.getTopicResults(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)

    fun updateInfo(name: String, publisher: String, durationMinutes: Int, scheduledAt: Long?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = mockExam.value ?: return@launch
            mockExamDao.update(
                current.copy(
                    name = trimmed,
                    publisher = publisher.trim().ifEmpty { null },
                    durationMinutes = durationMinutes,
                    scheduledAt = if (current.status == MockExamStatus.PLANNED) scheduledAt else current.scheduledAt,
                )
            )
        }
    }

    fun updatePart(part: MockExamPart, questionCount: Int, correctCount: Int, wrongCount: Int) {
        viewModelScope.launch {
            mockExamDao.updatePart(part.copy(questionCount = questionCount, correctCount = correctCount, wrongCount = wrongCount))
        }
    }

    /** Sets the score system and what each part counts as (part id -> test, missing = not counted). */
    fun updateScoring(system: ScoringSystem?, tests: Map<Long, ScoringTest?>) {
        viewModelScope.launch {
            val current = mockExam.value ?: return@launch
            mockExamDao.updateWithParts(
                current.copy(scoringSystem = system),
                parts.value.map { it.part.copy(scoringTest = if (system == null) null else tests[it.part.id]) },
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            mockExam.value?.let { mockExamDao.delete(it) }
            onDeleted()
        }
    }

    /** Adds topic rows for [topicIds] plus [newNames] (new topics of the part's course). */
    fun addTopicResults(partId: Long, courseId: Long?, topicIds: Set<Long>, newNames: List<String>) {
        viewModelScope.launch {
            val existing = topicResults.value.filter { it.result.mockExamPartId == partId }.map { it.result.topicId }.toSet()
            val created = if (courseId == null) emptyList() else newNames.map { topicIdForName(topicDao, it, courseId, null) }
            (topicIds + created).filterNot { it in existing }.forEach { topicId ->
                mockExamDao.insertTopicResult(
                    MockExamTopicResult(mockExamPartId = partId, topicId = topicId, questionCount = 0, correctCount = 0, wrongCount = 0)
                )
            }
        }
    }

    fun updateTopicResult(result: MockExamTopicResult, questionCount: Int, correctCount: Int, wrongCount: Int) {
        viewModelScope.launch {
            mockExamDao.updateTopicResult(result.copy(questionCount = questionCount, correctCount = correctCount, wrongCount = wrongCount))
        }
    }

    fun removeTopicResult(result: MockExamTopicResult) {
        viewModelScope.launch { mockExamDao.deleteTopicResult(result) }
    }
}
