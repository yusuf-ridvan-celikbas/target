package com.ridvan.target.ui.courselist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.ui.navigation.CourseListByTypeRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListByTypeViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val examTypeId: Long = savedStateHandle.toRoute<CourseListByTypeRoute>().examTypeId
    private val targetApplication = application as TargetApplication
    private val courseDao = targetApplication.database.courseDao()
    private val examTypeDao = targetApplication.database.examTypeDao()
    private val userId = targetApplication.preferences.currentUserId

    val examTypeName: StateFlow<String> = examTypeDao.getAll()
        .map { types -> types.firstOrNull { it.id == examTypeId }?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val courses: StateFlow<List<Course>> =
        (userId?.let { courseDao.getByUserIdAndExamTypeId(it, examTypeId) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCourse(name: String, icon: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null) return
        viewModelScope.launch {
            courseDao.insert(Course(name = trimmed, userId = userId, icon = icon, examTypeId = examTypeId))
        }
    }

    fun updateCourse(course: Course, newName: String, icon: String?) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { courseDao.update(course.copy(name = trimmed, icon = icon)) }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { courseDao.delete(course) }
    }
}
