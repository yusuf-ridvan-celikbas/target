package com.ridvan.target.ui.topiclist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.TopicWithTotals
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.TopicListRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicListViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val courseId: Long = savedStateHandle.toRoute<TopicListRoute>().courseId
    private val database = (application as TargetApplication).database
    private val topicDao = database.topicDao()
    private val courseDao = database.courseDao()

    val courseName: StateFlow<String> = courseDao.getById(courseId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val topics: StateFlow<List<TopicWithTotals>> = topicDao.getTopicTotalsByCourseId(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTopic(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = topicDao.getByCourseId(courseId).first()
                .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            if (existing == null) {
                topicDao.insert(Topic(name = trimmed, courseId = courseId))
            }
        }
    }
}
