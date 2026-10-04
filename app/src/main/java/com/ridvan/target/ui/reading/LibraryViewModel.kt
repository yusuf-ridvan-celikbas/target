package com.ridvan.target.ui.reading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Book
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Library: every book with its progress. Search/filter/sort happen in the screen over this one list. */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val bookDao = targetApplication.database.bookDao()
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val userId = targetApplication.preferences.currentUserId

    val books: StateFlow<List<BookProgress>> = bookProgressFlow(bookDao, focusSessionDao, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBook(book: Book) {
        val uid = userId ?: return
        viewModelScope.launch { bookDao.insert(book.copy(userId = uid)) }
    }
}
