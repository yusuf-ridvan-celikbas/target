package com.ridvan.target.ui.reading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val bookId: Long = checkNotNull(savedStateHandle.get<Long>("bookId"))
    private val targetApplication = application as TargetApplication
    private val bookDao = targetApplication.database.bookDao()
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val userId = targetApplication.preferences.currentUserId

    val progress: StateFlow<BookProgress?> = bookProgressFlow(bookDao, focusSessionDao, userId)
        .map { list -> list.firstOrNull { it.book.id == bookId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** This book's sessions, newest first (the DAO's order). */
    val sessions: StateFlow<List<FocusSessionWithLinks>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .map { items -> items.filter { it.session.bookId == bookId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateBook(book: Book) {
        viewModelScope.launch { bookDao.update(book) }
    }

    fun setStatus(book: Book, status: BookStatus) = updateBook(book.withStatus(status))

    fun setRating(book: Book, rating: Int?) = updateBook(book.copy(rating = rating))

    /** Blank notes save as null. */
    fun setNotes(book: Book, notes: String) = updateBook(book.copy(notes = notes.trim().ifEmpty { null }))

    /** A different random cover colour than the current one. */
    fun shuffleColour(book: Book) {
        val next = (coverPalette.indices - book.coverColor.mod(coverPalette.size)).random()
        updateBook(book.copy(coverColor = next))
    }

    /** Sessions keep their pages and time; they just lose the link (SET_NULL). */
    fun deleteBook(book: Book, onDeleted: () -> Unit) {
        viewModelScope.launch {
            bookDao.delete(book)
            onDeleted()
        }
    }

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.delete(session) }
    }
}
