package com.ridvan.target.ui.examdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.export.buildProgressExportRows
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
import com.ridvan.target.data.local.entity.MockExam
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.MockExamStatus
import com.ridvan.target.data.local.dao.MockExamSummary
import com.ridvan.target.ui.mockexam.MockCourseOption
import com.ridvan.target.ui.mockexam.MockExamDraft
import com.ridvan.target.ui.mockexam.NewMockPart
import com.ridvan.target.ui.navigation.ExamDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExamDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val route = savedStateHandle.toRoute<ExamDetailRoute>()
    private val examId: Long = route.examId

    /** True once when opened from a Home "New mock exam" shortcut: the new-deneme form should open. */
    private var pendingNewMockExam = route.openNewMockExam && savedStateHandle.get<Boolean>("newMockExamShown") != true
    private val handle = savedStateHandle

    fun consumeOpenNewMockExam(): Boolean {
        if (!pendingNewMockExam) return false
        pendingNewMockExam = false
        handle["newMockExamShown"] = true
        return true
    }
    private val targetApplication = application as TargetApplication
    private val database = targetApplication.database
    private val examDao: ExamDao = database.examDao()
    private val examTypeDao: ExamTypeDao = database.examTypeDao()
    private val sectionDao: SectionDao = database.sectionDao()
    private val courseDao: CourseDao = database.courseDao()
    private val examCourseDao: ExamCourseDao = database.examCourseDao()
    private val languageDao: LanguageDao = database.languageDao()
    private val topicDao = database.topicDao()
    private val practiceLogDao = database.practiceLogDao()
    private val userId = targetApplication.preferences.currentUserId
    private val mockExamDao = database.mockExamDao()

    val mockExams: StateFlow<List<MockExamSummary>> = mockExamDao.getSummariesByExam(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Prefill for a new deneme of the exam or of one section (sectioned exams): the last one's publisher,
     * limit and question counts there. Language exams ([namedParts]) get free-named parts instead of courses.
     */
    fun loadMockExamDraft(sectionId: Long?, namedParts: Boolean, defaultName: (Int) -> String, onResult: (MockExamDraft) -> Unit) {
        viewModelScope.launch {
            val latest = mockExamDao.getLatest(examId, sectionId)
            val latestParts = mockExamDao.getLatestParts(examId, sectionId)
            val options = when {
                namedParts -> emptyList()
                sectionId != null -> database.sectionCourseDao().getBySectionId(sectionId).first()
                    .map { MockCourseOption(it.sectionCourse.courseId, it.courseName, it.courseIcon) }
                else -> examCourseDao.getByExamId(examId).first().map { MockCourseOption(it.examCourse.courseId, it.courseName, it.courseIcon) }
            }
            onResult(
                MockExamDraft(
                    name = defaultName(mockExams.value.count { it.mockExam.sectionId == sectionId } + 1),
                    publisher = latest?.publisher.orEmpty(),
                    timeLimitMinutes = latest?.timeLimitMinutes,
                    sectionId = sectionId,
                    courses = options,
                    namedParts = namedParts,
                    questionCounts = latestParts.filter { it.courseId != null }.associate { it.courseId!! to it.questionCount },
                    namedCounts = latestParts.filter { it.courseId == null }.map { it.name.orEmpty() to it.questionCount },
                )
            )
        }
    }

    /**
     * Creates the deneme: planned (started later from its page, optionally at [scheduledAt]) or already taken.
     * Scoring carries over from the last deneme of the same exam/section, matched per course or part name.
     */
    fun createMockExam(
        draft: MockExamDraft,
        name: String,
        publisher: String,
        timeLimitMinutes: Int,
        parts: List<NewMockPart>,
        alreadyTaken: Boolean,
        scheduledAt: Long?,
        onCreated: (Long) -> Unit,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || timeLimitMinutes <= 0 || parts.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val latest = mockExamDao.getLatest(examId, draft.sectionId)
            val latestParts = mockExamDao.getLatestParts(examId, draft.sectionId)
            fun previousTest(part: NewMockPart) = latestParts.firstOrNull {
                if (part.courseId != null) it.courseId == part.courseId else it.courseId == null && it.name == part.name
            }?.scoringTest
            val newParts = parts.mapIndexed { index, part ->
                MockExamPart(
                    mockExamId = 0,
                    courseId = part.courseId,
                    name = part.name,
                    questionCount = part.questionCount,
                    orderIndex = index,
                    scoringTest = previousTest(part),
                )
            }
            val id = mockExamDao.insertWithParts(
                MockExam(
                    examId = examId,
                    name = trimmed,
                    publisher = publisher.trim().ifEmpty { null },
                    timeLimitMinutes = timeLimitMinutes,
                    durationMinutes = if (alreadyTaken) timeLimitMinutes else 0,
                    takenAt = now,
                    finishedAt = if (alreadyTaken) now else null,
                    status = if (alreadyTaken) MockExamStatus.FINISHED else MockExamStatus.PLANNED,
                    scheduledAt = if (alreadyTaken) null else scheduledAt,
                    sectionId = draft.sectionId,
                    scoringSystem = latest?.scoringSystem,
                ),
                newParts,
            )
            onCreated(id)
        }
    }

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

    private val coursesForExamType: Flow<List<Course>> = exam.filterNotNull().flatMapLatest { currentExam ->
        userId?.let { courseDao.getByUserIdAndExamTypeId(it, currentExam.examTypeId) } ?: flowOf(emptyList())
    }

    val availableCoursesToAdd: StateFlow<List<Course>> = combine(
        coursesForExamType,
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
        level: String?,
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
                    level = if (isLanguageExam) level else null,
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
        val currentExam = exam.value
        if (trimmed.isEmpty() || userId == null || currentExam == null) return
        viewModelScope.launch {
            val existing = courseDao.getByUserIdAndExamTypeId(userId, currentExam.examTypeId).first()
                .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            val courseId = existing?.id
                ?: courseDao.insert(Course(name = trimmed, userId = userId, examTypeId = currentExam.examTypeId))
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

    fun fetchProgressExportRows(onResult: (List<List<String>>) -> Unit) {
        viewModelScope.launch {
            val courseList = courses.value.map { it.examCourse.courseId to it.courseName }
            onResult(buildProgressExportRows(courseList, topicDao, practiceLogDao))
        }
    }
}
