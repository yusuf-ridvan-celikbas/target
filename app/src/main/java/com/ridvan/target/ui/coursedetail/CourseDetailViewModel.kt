package com.ridvan.target.ui.coursedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.ui.navigation.CourseDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val courseId: Long = savedStateHandle.toRoute<CourseDetailRoute>().courseId
    private val targetApplication = application as TargetApplication
    private val courseDao = targetApplication.database.courseDao()
    private val examTypeDao = targetApplication.database.examTypeDao()
    private val studyResourceDao = targetApplication.database.studyResourceDao()
    private val studyResourceTopicDao = targetApplication.database.studyResourceTopicDao()

    val course: StateFlow<Course?> = courseDao.getById(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val examTypes: StateFlow<List<ExamType>> = examTypeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateCourse(name: String, icon: String?, examTypeId: Long, category: CourseCategory?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = course.value ?: return@launch
            courseDao.update(current.copy(name = trimmed, icon = icon, examTypeId = examTypeId, category = category))
        }
    }

    fun deleteCourse() {
        viewModelScope.launch {
            course.value?.let { courseDao.delete(it) }
        }
    }

    fun fetchQuestionBankExportRows(onResult: (List<List<String>>) -> Unit) {
        val current = course.value ?: return
        viewModelScope.launch {
            val resources = studyResourceDao.getByCourseId(current.id).first()
                .filter { it.type == StudyResourceType.QUESTION_BANK }
            val rows = resources.flatMap { resource ->
                studyResourceTopicDao.getByStudyResourceId(resource.id).first().map { attached ->
                    listOf(
                        resource.name,
                        resource.publisher.orEmpty(),
                        attached.topicName,
                        attached.studyResourceTopic.testCount.toString(),
                        attached.studyResourceTopic.questionCount.toString(),
                    )
                }
            }
            onResult(rows)
        }
    }
}
