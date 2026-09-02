package com.ridvan.target.ui.courselist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Course
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val courseDao = targetApplication.database.courseDao()
    private val userId = targetApplication.preferences.currentUserId

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCourse(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null) return
        viewModelScope.launch { courseDao.insert(Course(name = trimmed, userId = userId)) }
    }

    fun renameCourse(course: Course, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { courseDao.update(course.copy(name = trimmed)) }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { courseDao.delete(course) }
    }
}
