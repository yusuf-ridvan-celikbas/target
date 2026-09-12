package com.ridvan.target.ui.topicdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.StudyResourceTopicWithStudyResource
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.TopicDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val topicId: Long = savedStateHandle.toRoute<TopicDetailRoute>().topicId
    private val database = (application as TargetApplication).database
    private val topicDao = database.topicDao()
    private val studyResourceTopicDao = database.studyResourceTopicDao()

    val topic: StateFlow<Topic?> = topicDao.getById(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val contributions: StateFlow<List<StudyResourceTopicWithStudyResource>> =
        studyResourceTopicDao.getByTopicId(topicId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalTestCount: StateFlow<Int> = contributions
        .map { list -> list.sumOf { it.studyResourceTopic.testCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalQuestionCount: StateFlow<Int> = contributions
        .map { list -> list.sumOf { it.studyResourceTopic.questionCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun updateTopic(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = topic.value ?: return@launch
            topicDao.update(current.copy(name = trimmed))
        }
    }

    fun deleteTopic() {
        viewModelScope.launch {
            topic.value?.let { topicDao.delete(it) }
        }
    }
}
