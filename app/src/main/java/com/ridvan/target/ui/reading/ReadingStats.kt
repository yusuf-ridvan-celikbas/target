package com.ridvan.target.ui.reading

import com.ridvan.target.data.local.dao.BookDao
import com.ridvan.target.data.local.dao.FocusSessionDao
import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** Pages covered by one reading session; 0 until its end page is known. */
fun FocusSession.pagesRead(): Int {
    val start = startPage ?: return 0
    val end = endPage ?: return 0
    return (end - start).coerceAtLeast(0)
}

val FocusSession.isReading: Boolean get() = bookId != null || startPage != null

/** A book plus what its sessions add up to. [currentPage] is the furthest page any session reached. */
data class BookProgress(
    val book: Book,
    val currentPage: Int,
    val pagesRead: Int,
    val minutes: Int,
    val sessionCount: Int,
) {
    val percent: Int? get() = book.totalPages?.takeIf { it > 0 }?.let { (currentPage * 100 / it).coerceIn(0, 100) }
}

fun bookProgress(books: List<Book>, sessions: List<FocusSession>): List<BookProgress> {
    val byBook = sessions.filter { it.bookId != null }.groupBy { it.bookId }
    return books.map { book ->
        val own = byBook[book.id].orEmpty()
        BookProgress(
            book = book,
            // Furthest page reached, not the latest session's — a back-dated manual log can be
            // "older" than a session that stopped earlier in the book.
            currentPage = own.mapNotNull { it.endPage }.maxOrNull() ?: 0,
            pagesRead = own.sumOf { it.pagesRead() },
            minutes = own.sumOf { it.totalWorkMinutes },
            sessionCount = own.size,
        )
    }
}

/** The signed-in user's books with progress — shared by every screen that picks or lists books. */
fun bookProgressFlow(bookDao: BookDao, focusSessionDao: FocusSessionDao, userId: Long?): Flow<List<BookProgress>> {
    if (userId == null) return flowOf(emptyList())
    return combine(bookDao.getByUserId(userId), focusSessionDao.getAllWithLinksByUserId(userId)) { books, sessions ->
        bookProgress(books, sessions.map { it.session })
    }
}

/** Pages per hour, or null with no reading time yet. */
fun pagesPerHour(pages: Int, minutes: Int): Double? = if (minutes > 0) pages * 60.0 / minutes else null

/** A reading rate to one decimal, without a trailing ".0" for whole numbers: "30", "18.3" (locale decimal mark). */
fun formatRate(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return if (tenths % 10 == 0L) (tenths / 10).toString() else String.format(java.util.Locale.getDefault(), "%.1f", tenths / 10.0)
}
