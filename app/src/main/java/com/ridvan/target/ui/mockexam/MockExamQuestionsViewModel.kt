package com.ridvan.target.ui.mockexam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.MockExamPartWithCourse
import com.ridvan.target.data.local.entity.MockExamQuestion
import com.ridvan.target.data.local.entity.QuestionResult
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.MockExamQuestionsRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ridvan.target.ui.common.topicIdForName

class MockExamQuestionsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val partId: Long = savedStateHandle.toRoute<MockExamQuestionsRoute>().partId
    private val database = (application as TargetApplication).database
    private val mockExamDao = database.mockExamDao()

    val part: StateFlow<MockExamPartWithCourse?> = mockExamDao.getPartWithCourse(partId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Marked questions by number. */
    val questions: StateFlow<Map<Int, MockExamQuestion>> = mockExamDao.getQuestions(partId)
        .map { list -> list.associateBy { it.number } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** The course's topics, or for a named part the language exam's language topics. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val topics: StateFlow<List<Topic>> = part
        .flatMapLatest { p ->
            when {
                p == null -> flowOf(emptyList())
                p.part.courseId != null -> database.topicDao().getByCourseId(p.part.courseId)
                else -> mockExamDao.getById(p.part.mockExamId).flatMapLatest { mock ->
                    if (mock == null) flowOf(emptyList())
                    else database.examDao().getById(mock.examId).flatMapLatest { exam ->
                        exam?.languageId?.let { database.topicDao().getByLanguageId(it) } ?: flowOf(emptyList())
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds a topic to the part's course (or the language exam's language) and hands back its id. */
    fun createTopic(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val p = part.value?.part ?: return@launch
            val languageId = if (p.courseId != null) null else {
                val examId = mockExamDao.getById(p.mockExamId).first()?.examId ?: return@launch
                database.examDao().getById(examId).first()?.languageId ?: return@launch
            }
            onCreated(topicIdForName(database.topicDao(), name, p.courseId, languageId))
        }
    }

    private fun question(number: Int) = questions.value[number] ?: MockExamQuestion(mockExamPartId = partId, number = number)

    fun setResult(number: Int, result: QuestionResult?) = save(listOf(question(number).copy(result = result)))

    fun setTopic(number: Int, topicId: Long?) = save(listOf(question(number).copy(topicId = topicId)))

    /** Every question without a result becomes correct. */
    fun markRestCorrect() {
        val count = part.value?.part?.questionCount ?: return
        save((1..count).map(::question).filter { it.result == null }.map { it.copy(result = QuestionResult.CORRECT) })
    }

    private fun save(changed: List<MockExamQuestion>) {
        if (changed.isEmpty()) return
        viewModelScope.launch { mockExamDao.saveQuestions(partId, changed) }
    }
}
