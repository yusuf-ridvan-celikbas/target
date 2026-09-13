package com.ridvan.target.ui.coursedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.export.CourseQuestionBankCsvImporter
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceTopic
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.navigation.CourseDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionBankImportResult(
    val rowsProcessed: Int,
    val resourcesCreated: Int,
    val topicsCreated: Int,
    val attachmentsCreated: Int,
    val attachmentsUpdated: Int,
)

sealed interface QuestionBankImportOutcome {
    data class Imported(val result: QuestionBankImportResult) : QuestionBankImportOutcome
    data object EmptyOrInvalid : QuestionBankImportOutcome
}

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
    private val topicDao = targetApplication.database.topicDao()

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

    fun importQuestionBankCsv(csvContent: String, onResult: (QuestionBankImportOutcome) -> Unit) {
        val current = course.value ?: return
        viewModelScope.launch {
            val csvRows = CourseQuestionBankCsvImporter.parse(csvContent)
            if (csvRows.isEmpty()) {
                onResult(QuestionBankImportOutcome.EmptyOrInvalid)
                return@launch
            }

            var resourcesCreated = 0
            var topicsCreated = 0
            var attachmentsCreated = 0
            var attachmentsUpdated = 0

            val resources = studyResourceDao.getByCourseId(current.id).first().toMutableList()
            val topics = topicDao.getByCourseId(current.id).first().toMutableList()

            csvRows.forEach { row ->
                var resource = resources.firstOrNull { it.name == row.resourceName && it.publisher == row.publisher }
                if (resource == null) {
                    val newId = studyResourceDao.insert(
                        StudyResource(name = row.resourceName, courseId = current.id, type = StudyResourceType.QUESTION_BANK, publisher = row.publisher)
                    )
                    resource = StudyResource(id = newId, name = row.resourceName, courseId = current.id, type = StudyResourceType.QUESTION_BANK, publisher = row.publisher)
                    resources.add(resource)
                    resourcesCreated++
                }

                var topic = topics.firstOrNull { it.name.equals(row.topicName, ignoreCase = true) }
                if (topic == null) {
                    val newId = topicDao.insert(Topic(name = row.topicName, courseId = current.id))
                    topic = Topic(id = newId, name = row.topicName, courseId = current.id)
                    topics.add(topic)
                    topicsCreated++
                }

                val attachments = studyResourceTopicDao.getByStudyResourceId(resource.id).first()
                val existingAttachment = attachments.firstOrNull { it.studyResourceTopic.topicId == topic.id }
                if (existingAttachment != null) {
                    studyResourceTopicDao.update(
                        existingAttachment.studyResourceTopic.copy(testCount = row.testCount, questionCount = row.questionCount)
                    )
                    attachmentsUpdated++
                } else {
                    studyResourceTopicDao.insert(
                        StudyResourceTopic(
                            studyResourceId = resource.id,
                            topicId = topic.id,
                            testCount = row.testCount,
                            questionCount = row.questionCount,
                            orderIndex = attachments.size,
                        )
                    )
                    attachmentsCreated++
                }
            }

            onResult(
                QuestionBankImportOutcome.Imported(
                    QuestionBankImportResult(csvRows.size, resourcesCreated, topicsCreated, attachmentsCreated, attachmentsUpdated)
                )
            )
        }
    }
}
