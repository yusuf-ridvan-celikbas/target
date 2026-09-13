package com.ridvan.target.ui.studyresource

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.PracticeLogTotals
import com.ridvan.target.data.local.dao.StudyResourceTargetTotals
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.ui.navigation.CourseStudyResourceRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CourseStudyResourceViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val courseId: Long = savedStateHandle.toRoute<CourseStudyResourceRoute>().courseId
    private val targetApplication = application as TargetApplication
    private val courseDao = targetApplication.database.courseDao()
    private val studyResourceDao = targetApplication.database.studyResourceDao()
    private val studyResourceTopicDao = targetApplication.database.studyResourceTopicDao()
    private val practiceLogDao = targetApplication.database.practiceLogDao()

    val course: StateFlow<Course?> = courseDao.getById(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val studyResources: StateFlow<List<StudyResource>> = studyResourceDao.getByCourseId(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val questionBankResourceIds: Flow<List<Long>> = studyResourceDao.getByCourseId(courseId).map { list ->
        list.filter { it.type == StudyResourceType.QUESTION_BANK }.map { it.id }
    }

    val targetTotals: StateFlow<StudyResourceTargetTotals> = questionBankResourceIds.flatMapLatest { ids ->
        studyResourceTopicDao.getTargetTotals(ids)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyResourceTargetTotals(0, 0))

    val progressTotals: StateFlow<PracticeLogTotals> = questionBankResourceIds.flatMapLatest { ids ->
        practiceLogDao.getProgressTotals(ids)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeLogTotals(0, 0, 0, 0))

    fun addStudyResource(name: String, type: StudyResourceType, publisher: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            studyResourceDao.insert(StudyResource(name = trimmed, courseId = courseId, type = type, publisher = publisher))
        }
    }
}
