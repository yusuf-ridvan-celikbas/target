package com.ridvan.target.ui.studyresourcedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.StudyResourceTopicWithTopic
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceTopic
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.StudyResourceDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudyResourceDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val studyResourceId: Long = savedStateHandle.toRoute<StudyResourceDetailRoute>().studyResourceId
    private val database = (application as TargetApplication).database
    private val studyResourceDao = database.studyResourceDao()
    private val courseDao = database.courseDao()
    private val languageDao = database.languageDao()
    private val topicDao = database.topicDao()
    private val studyResourceTopicDao = database.studyResourceTopicDao()

    val studyResource: StateFlow<StudyResource?> = studyResourceDao.getById(studyResourceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val subjectName: StateFlow<String> = studyResource.filterNotNull().flatMapLatest { resource ->
        when {
            resource.courseId != null -> courseDao.getById(resource.courseId).map { it?.name.orEmpty() }
            resource.languageId != null -> languageDao.getById(resource.languageId).map { it?.name.orEmpty() }
            else -> flowOf("")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val attachedTopics: StateFlow<List<StudyResourceTopicWithTopic>> =
        studyResourceTopicDao.getByStudyResourceId(studyResourceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val topicsForCourse: Flow<List<Topic>> = studyResource.filterNotNull().flatMapLatest { resource ->
        resource.courseId?.let { topicDao.getByCourseId(it) } ?: flowOf(emptyList())
    }

    val availableTopicsToAdd: StateFlow<List<Topic>> = combine(
        topicsForCourse,
        attachedTopics,
    ) { allTopics, attached ->
        val attachedIds = attached.map { it.studyResourceTopic.topicId }.toSet()
        allTopics.filterNot { it.id in attachedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateStudyResource(name: String, type: StudyResourceType, publisher: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = studyResource.value ?: return@launch
            studyResourceDao.update(current.copy(name = trimmed, type = type, publisher = publisher))
        }
    }

    fun deleteStudyResource() {
        viewModelScope.launch {
            studyResource.value?.let { studyResourceDao.delete(it) }
        }
    }

    fun addTopic(name: String) {
        val trimmed = name.trim()
        val courseId = studyResource.value?.courseId
        if (trimmed.isEmpty() || courseId == null) return
        viewModelScope.launch {
            val existing = topicDao.getByCourseId(courseId).first()
                .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            val topicId = existing?.id ?: topicDao.insert(Topic(name = trimmed, courseId = courseId))
            if (attachedTopics.value.none { it.studyResourceTopic.topicId == topicId }) {
                studyResourceTopicDao.insert(StudyResourceTopic(studyResourceId = studyResourceId, topicId = topicId))
            }
        }
    }

    fun addExistingTopics(topicIds: Set<Long>) {
        if (topicIds.isEmpty()) return
        viewModelScope.launch {
            topicIds.forEach { topicId ->
                if (attachedTopics.value.none { it.studyResourceTopic.topicId == topicId }) {
                    studyResourceTopicDao.insert(StudyResourceTopic(studyResourceId = studyResourceId, topicId = topicId))
                }
            }
        }
    }

    fun updateTopicCounts(studyResourceTopic: StudyResourceTopic, testCount: Int, questionCount: Int) {
        viewModelScope.launch {
            studyResourceTopicDao.update(studyResourceTopic.copy(testCount = testCount, questionCount = questionCount))
        }
    }

    fun removeTopic(studyResourceTopic: StudyResourceTopic) {
        viewModelScope.launch { studyResourceTopicDao.delete(studyResourceTopic) }
    }
}
