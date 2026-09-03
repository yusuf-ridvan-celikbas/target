package com.ridvan.target.ui.examdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.CourseDao
import com.ridvan.target.data.local.dao.ExamCourseDao
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamTypeDao
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.dao.LanguageDao
import com.ridvan.target.data.local.dao.SectionDao
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamCourse
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.ui.navigation.ExamDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val examId: Long = savedStateHandle.toRoute<ExamDetailRoute>().examId
    private val targetApplication = application as TargetApplication
    private val database = targetApplication.database
    private val examDao: ExamDao = database.examDao()
    private val examTypeDao: ExamTypeDao = database.examTypeDao()
    private val sectionDao: SectionDao = database.sectionDao()
    private val courseDao: CourseDao = database.courseDao()
    private val examCourseDao: ExamCourseDao = database.examCourseDao()
    private val languageDao: LanguageDao = database.languageDao()
    private val userId = targetApplication.preferences.currentUserId

    val exam: StateFlow<Exam?> = examDao.getById(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val examTypes: StateFlow<List<ExamType>> = examTypeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sections: StateFlow<List<Section>> = sectionDao.getByExamId(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val courses: StateFlow<List<ExamCourseWithCourse>> = examCourseDao.getByExamId(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCoursesToAdd: StateFlow<List<Course>> = combine(
        userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()),
        courses,
    ) { allCourses, attached ->
        val attachedIds = attached.map { it.examCourse.courseId }.toSet()
        allCourses.filterNot { it.id in attachedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateExam(
        name: String,
        examTypeId: Long,
        hasSections: Boolean,
        examDate: Long?,
        studyStartDate: Long?,
        languageId: Long?,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = exam.value ?: return@launch
            val isLanguageExam = examTypes.value.firstOrNull { it.id == examTypeId }?.name == LANGUAGE_EXAM_TYPE_NAME
            examDao.update(
                current.copy(
                    name = trimmed,
                    examTypeId = examTypeId,
                    examDate = if (hasSections) null else examDate,
                    studyStartDate = studyStartDate,
                    languageId = if (isLanguageExam) languageId else null,
                ),
            )
        }
    }

    fun deleteExam() {
        viewModelScope.launch {
            exam.value?.let { examDao.delete(it) }
        }
    }

    fun addSection(name: String, date: Long?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sectionDao.insert(Section(examId = examId, name = trimmed, date = date, orderIndex = sections.value.size))
        }
    }

    fun addCourse(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null) return
        viewModelScope.launch {
            val existing = courseDao.getByUserId(userId).first().firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            val courseId = existing?.id ?: courseDao.insert(Course(name = trimmed, userId = userId))
            if (courses.value.none { it.examCourse.courseId == courseId }) {
                examCourseDao.insert(ExamCourse(examId = examId, courseId = courseId))
            }
        }
    }

    fun addExistingCourses(courseIds: Set<Long>) {
        if (courseIds.isEmpty()) return
        viewModelScope.launch {
            courseIds.forEach { courseId ->
                if (courses.value.none { it.examCourse.courseId == courseId }) {
                    examCourseDao.insert(ExamCourse(examId = examId, courseId = courseId))
                }
            }
        }
    }

    fun removeCourse(examCourse: ExamCourse) {
        viewModelScope.launch { examCourseDao.delete(examCourse) }
    }
}
