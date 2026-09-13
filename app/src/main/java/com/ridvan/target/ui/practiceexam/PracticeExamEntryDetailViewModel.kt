package com.ridvan.target.ui.practiceexam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.PracticeExamEntryTopicResultWithTopic
import com.ridvan.target.data.local.entity.PracticeExamEntry
import com.ridvan.target.data.local.entity.PracticeExamEntryTopicResult
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.PracticeExamEntryDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeExamEntryDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val entryId: Long = savedStateHandle.toRoute<PracticeExamEntryDetailRoute>().entryId
    private val database = (application as TargetApplication).database
    private val practiceExamEntryDao = database.practiceExamEntryDao()
    private val practiceExamEntryTopicResultDao = database.practiceExamEntryTopicResultDao()
    private val studyResourceDao = database.studyResourceDao()
    private val topicDao = database.topicDao()

    val entry: StateFlow<PracticeExamEntry?> = practiceExamEntryDao.getById(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val studyResource: StateFlow<StudyResource?> = entry.filterNotNull().flatMapLatest { current ->
        studyResourceDao.getById(current.studyResourceId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topicResults: StateFlow<List<PracticeExamEntryTopicResultWithTopic>> = practiceExamEntryTopicResultDao.getByEntryId(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val topicsForCourse: Flow<List<Topic>> = studyResource.filterNotNull().flatMapLatest { resource ->
        resource.courseId?.let { topicDao.getByCourseId(it) } ?: flowOf(emptyList())
    }

    val availableTopicsToAdd: StateFlow<List<Topic>> = combine(topicsForCourse, topicResults) { allTopics, added ->
        val addedIds = added.map { it.topicResult.topicId }.toSet()
        allTopics.filterNot { it.id in addedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateResult(correctCount: Int, wrongCount: Int, durationMinutes: Int) {
        viewModelScope.launch {
            val current = entry.value ?: return@launch
            practiceExamEntryDao.update(
                current.copy(correctCount = correctCount, wrongCount = wrongCount, durationMinutes = durationMinutes)
            )
        }
    }

    fun deleteEntry(onDeleted: () -> Unit) {
        viewModelScope.launch {
            entry.value?.let { practiceExamEntryDao.delete(it) }
            onDeleted()
        }
    }

    fun addTopicResults(topicIds: Set<Long>) {
        if (topicIds.isEmpty()) return
        viewModelScope.launch {
            topicIds.forEach { topicId ->
                if (topicResults.value.none { it.topicResult.topicId == topicId }) {
                    practiceExamEntryTopicResultDao.insert(
                        PracticeExamEntryTopicResult(
                            practiceExamEntryId = entryId,
                            topicId = topicId,
                            questionCount = 0,
                            correctCount = 0,
                            wrongCount = 0,
                        )
                    )
                }
            }
        }
    }

    fun updateTopicResult(topicResult: PracticeExamEntryTopicResult, questionCount: Int, correctCount: Int, wrongCount: Int) {
        viewModelScope.launch {
            practiceExamEntryTopicResultDao.update(
                topicResult.copy(questionCount = questionCount, correctCount = correctCount, wrongCount = wrongCount)
            )
        }
    }

    fun removeTopicResult(topicResult: PracticeExamEntryTopicResult) {
        viewModelScope.launch { practiceExamEntryTopicResultDao.delete(topicResult) }
    }
}
