package com.ridvan.target.ui.focustimer

import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.flow.Flow
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Focus Timer history; filtering beyond the optional owner scope happens in the screen.
 *
 * Backs four routes: FocusHistoryRoute / FocusHistoryHomeRoute (whole history, no args) and
 * CourseWorkHistoryRoute / LanguageWorkHistoryRoute (one owner). The owner is read by argument
 * name rather than a route-typed toRoute<>(), so the same ViewModel serves all of them.
 */
class FocusHistoryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val courseDao = targetApplication.database.courseDao()
    private val languageDao = targetApplication.database.languageDao()
    private val topicDao = targetApplication.database.topicDao()
    private val userId = targetApplication.preferences.currentUserId

    /** On a Work History page, a manually logged session starts linked to that course/language. */
    val scopeCourseId: Long? = savedStateHandle.get<Long>("courseId")
    val scopeLanguageId: Long? = savedStateHandle.get<Long>("languageId")

    /** True on a course's/language's Work History, where the Subject filter is redundant. */
    val isScoped: Boolean = scopeCourseId != null || scopeLanguageId != null

    /** The scoped course's raw name (translate for display) — null when unscoped or a language. */
    val scopeCourseName: StateFlow<String?> = (scopeCourseId?.let { id -> courseDao.getById(id).map { it?.name } } ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val scopeLanguageName: StateFlow<String?> = (scopeLanguageId?.let { id -> languageDao.getById(id).map { it?.name } } ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<FocusSessionWithLinks>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .map { items ->
            when {
                scopeCourseId != null -> items.filter { it.session.courseId == scopeCourseId }
                scopeLanguageId != null -> items.filter { it.session.languageId == scopeLanguageId }
                else -> items
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)
    fun topicsForLanguage(languageId: Long): Flow<List<Topic>> = topicDao.getByLanguageId(languageId)

    fun addManualSession(form: ManualFocusSessionForm) {
        val uid = userId ?: return
        viewModelScope.launch { focusSessionDao.insert(form.toNewSession(uid)) }
    }

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.delete(session) }
    }
}
