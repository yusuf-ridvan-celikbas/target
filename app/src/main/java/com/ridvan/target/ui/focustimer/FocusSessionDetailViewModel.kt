package com.ridvan.target.ui.focustimer

import com.ridvan.target.data.local.entity.Book
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusSessionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val sessionId: Long = checkNotNull(savedStateHandle.get<Long>("sessionId"))
    private val targetApplication = application as TargetApplication
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val courseDao = targetApplication.database.courseDao()
    private val languageDao = targetApplication.database.languageDao()
    private val topicDao = targetApplication.database.topicDao()
    private val bookDao = targetApplication.database.bookDao()
    private val userId = targetApplication.preferences.currentUserId

    val item: StateFlow<FocusSessionWithLinks?> = focusSessionDao.getByIdWithLinks(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val courses: StateFlow<List<Course>> = (userId?.let { courseDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The session's book, for its page count when fixing pages. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val book: StateFlow<Book?> = item
        .flatMapLatest { current -> current?.session?.bookId?.let { bookDao.getById(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updatePages(startPage: Int, endPage: Int) {
        val session = item.value?.session ?: return
        viewModelScope.launch {
            focusSessionDao.setPages(session.id, startPage, endPage)
            val total = book.value?.totalPages
            if (session.bookId != null && total != null && endPage >= total) bookDao.markFinished(session.bookId)
        }
    }

    fun topicsForCourse(courseId: Long): Flow<List<Topic>> = topicDao.getByCourseId(courseId)
    fun topicsForLanguage(languageId: Long): Flow<List<Topic>> = topicDao.getByLanguageId(languageId)

    /** Exactly one of courseId/languageId, or neither; topicId belongs to whichever is set. */
    fun updateLink(courseId: Long?, languageId: Long?, topicId: Long?) {
        val session = item.value?.session ?: return
        viewModelScope.launch {
            focusSessionDao.update(session.copy(courseId = courseId, languageId = languageId, topicId = topicId))
        }
    }

    fun updateNotes(notes: String) {
        val session = item.value?.session ?: return
        viewModelScope.launch {
            focusSessionDao.update(session.copy(notes = notes.trim().ifEmpty { null }))
        }
    }

    /** Re-times a hand-logged session; its link and notes are left as they are. */
    fun updateTimes(form: ManualFocusSessionForm) {
        val session = item.value?.session ?: return
        viewModelScope.launch {
            focusSessionDao.update(
                session.copy(
                    startedAt = form.startedAt,
                    endedAt = form.endedAt,
                    totalWorkMinutes = form.workMinutes,
                    totalBreakMinutes = form.breakMinutes,
                ),
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val session = item.value?.session ?: return
        viewModelScope.launch {
            focusSessionDao.delete(session)
            onDeleted()
        }
    }
}
