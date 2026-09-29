package com.ridvan.target.ui.reading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Reading page: every book with its progress, and every reading session for the stats. */
class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val bookDao = targetApplication.database.bookDao()
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val userId = targetApplication.preferences.currentUserId

    val books: StateFlow<List<BookProgress>> = bookProgressFlow(bookDao, focusSessionDao, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val readingSessions: StateFlow<List<FocusSession>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .map { items -> items.map { it.session }.filter { it.isReading } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBook(title: String, author: String?, totalPages: Int?) {
        val uid = userId ?: return
        viewModelScope.launch { bookDao.insert(Book(userId = uid, title = title, author = author, totalPages = totalPages)) }
    }
}
