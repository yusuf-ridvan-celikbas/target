package com.ridvan.target.ui.examdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.data.local.entity.TopicStatus
import com.ridvan.target.ui.navigation.ExamDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val examId: Long = savedStateHandle.toRoute<ExamDetailRoute>().examId
    private val database = (application as TargetApplication).database
    private val examDao: ExamDao = database.examDao()
    private val topicDao: TopicDao = database.topicDao()

    val exam: StateFlow<Exam?> = examDao.getById(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topics: StateFlow<List<Topic>> = topicDao.getTopLevelByExamId(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTopic(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            topicDao.insert(Topic(examId = examId, name = trimmed, orderIndex = topics.value.size))
        }
    }

    fun cycleStatus(topic: Topic) {
        val next = when (topic.status) {
            TopicStatus.NOT_STARTED -> TopicStatus.IN_PROGRESS
            TopicStatus.IN_PROGRESS -> TopicStatus.DONE
            TopicStatus.DONE -> TopicStatus.NOT_STARTED
        }
        viewModelScope.launch {
            topicDao.update(
                topic.copy(
                    status = next,
                    lastStudiedAt = if (next == TopicStatus.NOT_STARTED) topic.lastStudiedAt else System.currentTimeMillis(),
                ),
            )
        }
    }
}
