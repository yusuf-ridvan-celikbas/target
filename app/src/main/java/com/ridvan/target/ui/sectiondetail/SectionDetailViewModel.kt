package com.ridvan.target.ui.sectiondetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamCourseDao
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.data.local.dao.SectionCourseDao
import com.ridvan.target.data.local.dao.SectionCourseWithCourse
import com.ridvan.target.data.local.dao.SectionDao
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.data.local.entity.SectionCourse
import com.ridvan.target.ui.navigation.SectionDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SectionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val sectionId: Long = savedStateHandle.toRoute<SectionDetailRoute>().sectionId
    private val database = (application as TargetApplication).database
    private val sectionDao: SectionDao = database.sectionDao()
    private val examCourseDao: ExamCourseDao = database.examCourseDao()
    private val sectionCourseDao: SectionCourseDao = database.sectionCourseDao()

    val section: StateFlow<Section?> = sectionDao.getById(sectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val assignedCourses: StateFlow<List<SectionCourseWithCourse>> = sectionCourseDao.getBySectionId(sectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val examCoursePool: StateFlow<List<ExamCourseWithCourse>> = section
        .filterNotNull()
        .distinctUntilChangedBy { it.examId }
        .flatMapLatest { examCourseDao.getByExamId(it.examId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCourses: StateFlow<List<ExamCourseWithCourse>> =
        combine(examCoursePool, assignedCourses) { pool, assigned ->
            val assignedIds = assigned.map { it.sectionCourse.courseId }.toSet()
            pool.filterNot { it.examCourse.courseId in assignedIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun assignCourses(courseIds: Set<Long>) {
        if (courseIds.isEmpty()) return
        viewModelScope.launch {
            courseIds.forEach { courseId ->
                sectionCourseDao.insert(SectionCourse(sectionId = sectionId, courseId = courseId))
            }
        }
    }

    fun removeCourse(sectionCourse: SectionCourse) {
        viewModelScope.launch { sectionCourseDao.delete(sectionCourse) }
    }

    fun updateSection(name: String, date: Long?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = section.value ?: return@launch
            sectionDao.update(current.copy(name = trimmed, date = date))
        }
    }

    fun deleteSection() {
        viewModelScope.launch {
            section.value?.let { sectionDao.delete(it) }
        }
    }
}
