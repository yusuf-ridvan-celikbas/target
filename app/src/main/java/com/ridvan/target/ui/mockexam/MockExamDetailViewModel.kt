package com.ridvan.target.ui.mockexam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.dao.MockExamTopicResultWithTopic
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.MockExamStatus
import com.ridvan.target.data.local.entity.MockExamTopicResult
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.MockExamDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MockExamDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val mockExamId: Long = savedStateHandle.toRoute<MockExamDetailRoute>().mockExamId
    private val database = (application as TargetApplication).database
    private val mockExamDao = database.mockExamDao()
    private val topicDao = database.topicDao()
    private val examDao = database.examDao()

    val mockExam: StateFlow<MockExam?> = mockExamDao.getById(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val exam: StateFlow<Exam?> = mockExam
        .flatMapLatest { mock -> mock?.let { examDao.getById(it.examId) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val parts: StateFlow<List<MockExamPartWithCourse>> = mockExamDao.getParts(mockExamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Saved on the exam, so every mock exam of it shares the setting. Coefficients come as base/coef pairs. */
    fun updateAlesScoring(enabled: Boolean, coefficients: Map<AlesScoreType, Pair<Double?, Double?>>) {
        viewModelScope.launch {
            val current = exam.value ?: return@launch
            examDao.update(
                current.copy(
                    alesScoring = enabled,
                    alesSayBase = coefficients[AlesScoreType.QUANTITATIVE]?.first,
                    alesSayCoef = coefficients[AlesScoreType.QUANTITATIVE]?.second,
                    alesSozBase = coefficients[AlesScoreType.VERBAL]?.first,
                    alesSozCoef = coefficients[AlesScoreType.VERBAL]?.second,
                    alesEaBase = coefficients[AlesScoreType.EQUAL_WEIGHT]?.first,
                    alesEaCoef = coefficients[AlesScoreType.EQUAL_WEIGHT]?.second,
                )
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            mockExam.value?.let { mockExamDao.delete(it) }
            onDeleted()
        }
    }

    fun addTopicResults(partId: Long, topicIds: Set<Long>) {
        viewModelScope.launch {
            val existing = topicResults.value.filter { it.result.mockExamPartId == partId }.map { it.result.topicId }.toSet()
            topicIds.filterNot { it in existing }.forEach { topicId ->
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
