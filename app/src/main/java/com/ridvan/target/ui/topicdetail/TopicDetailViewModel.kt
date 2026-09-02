package com.ridvan.target.ui.topicdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ResourceDao
import com.ridvan.target.data.local.dao.TopicDao
import com.ridvan.target.data.local.entity.Resource
import com.ridvan.target.data.local.entity.ResourceType
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.TopicDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val topicId: Long = savedStateHandle.toRoute<TopicDetailRoute>().topicId
    private val database = (application as TargetApplication).database
    private val topicDao: TopicDao = database.topicDao()
    private val resourceDao: ResourceDao = database.resourceDao()

    val topic: StateFlow<Topic?> = topicDao.getById(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val resources: StateFlow<List<Resource>> = resourceDao.getByTopicId(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addNote(title: String, content: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        viewModelScope.launch {
            resourceDao.insert(
                Resource(
                    topicId = topicId,
                    type = ResourceType.NOTE,
                    title = trimmedTitle,
                    noteContent = content.trim(),
                ),
            )
        }
    }
}
