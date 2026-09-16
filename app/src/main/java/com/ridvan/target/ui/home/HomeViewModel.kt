package com.ridvan.target.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.ui.common.startOfTodayMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class UpcomingEvent(
    val examId: Long,
    val sectionId: Long?,
    val label: String,
    val date: Long,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userDao = targetApplication.database.userDao()
    private val examDao = targetApplication.database.examDao()
    private val sectionDao = targetApplication.database.sectionDao()
    private val preferences = targetApplication.preferences
    private val userId = preferences.currentUserId

    val currentUser: StateFlow<User?> = run {
        if (userId != null) userDao.getById(userId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val upcomingEvents: StateFlow<List<UpcomingEvent>> = combine(
        userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()),
        userId?.let { sectionDao.getByUserId(it) } ?: flowOf(emptyList()),
    ) { exams, sections ->
        val today = startOfTodayMillis()
        val examEvents = exams
            .filter { !it.exam.hasSections && it.exam.examDate != null && it.exam.examDate >= today }
            .map { UpcomingEvent(it.exam.id, null, homeExamLabel(it), it.exam.examDate!!) }
        val examLabels = exams.associate { it.exam.id to homeExamLabel(it) }
        val sectionEvents = sections
            .filter { it.date != null && it.date >= today }
            .mapNotNull { section ->
                examLabels[section.examId]?.let { examLabel ->
                    UpcomingEvent(section.examId, section.id, "$examLabel – ${section.name}", section.date!!)
                }
            }
        (examEvents + sectionEvents).sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * "Goethe - A2 - German" for a Language Exam (publisher - level - language, each part
 * omitted if unset); the plain exam name for every other type. Dash-joined and
 * language-inclusive specifically for Home's Upcoming card, by explicit request — the
 * shorter `examDisplayLabel` (name + level, no language) is what ExamListScreen/
 * ExamDetailScreen show instead, since those already surface the language separately.
 */
private fun homeExamLabel(item: ExamWithType): String {
    if (item.examTypeName != LANGUAGE_EXAM_TYPE_NAME) return item.exam.name
    return listOfNotNull(
        item.exam.name,
        item.exam.level?.trim()?.takeIf { it.isNotEmpty() },
        item.languageName,
    ).joinToString(" - ")
}
