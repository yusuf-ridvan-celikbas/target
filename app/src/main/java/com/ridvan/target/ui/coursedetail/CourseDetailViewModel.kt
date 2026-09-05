package com.ridvan.target.ui.coursedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.ui.navigation.CourseDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val course: StateFlow<Course?> = courseDao.getById(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val examTypes: StateFlow<List<ExamType>> = examTypeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateCourse(name: String, icon: String?, examTypeId: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = course.value ?: return@launch
            courseDao.update(current.copy(name = trimmed, icon = icon, examTypeId = examTypeId))
        }
    }

    fun deleteCourse() {
        viewModelScope.launch {
            course.value?.let { courseDao.delete(it) }
        }
    }
}
