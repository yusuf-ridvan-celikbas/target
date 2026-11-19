package com.ridvan.target.data.extensions

/**
 * Target ↔ Lilies Lib contract, v1 (pure: no Android types, unit-tested). Full text: docs/lilies-lib-contract.md.
 *
 * Target starts [ACTION_PICK_BOOK] in [PACKAGE] for a result, with [EXTRA_CONTRACT_VERSION]. Lilies Lib answers
 * RESULT_OK with the picked book as intent extras (keys below), or RESULT_CANCELED. Changes within a version are
 * additive only (new optional extras); a breaking change gets a new version number and Target rejects versions
 * it doesn't know as "too new".
 */
object LiliesLibContract {
    const val PACKAGE = "com.anonymous.eighteeneleven"
    const val ACTION_PICK_BOOK = "com.ridvan.target.action.PICK_BOOK"
    const val CONTRACT_VERSION = 1

    /**
     * Send a Target book to Lilies Lib (manual, never automatic): same extras as the picker answer minus book_id;
     * Lilies Lib asks the user to confirm and answers RESULT_OK with [EXTRA_BOOK_ID] (new or matching book).
     */
    const val ACTION_ADD_BOOK = "com.ridvan.target.action.ADD_BOOK"

    /** Value of `books.externalSource` for books picked from Lilies Lib. */
    const val SOURCE = "lilies_lib"

    /** Lilies Lib's latest release APK; opened in the browser, Target itself never downloads. */
    const val DOWNLOAD_URL = "https://github.com/yusuf-ridvan-celikbas/library-app-mobile/releases/latest/download/lilies-lib.apk"

    const val EXTRA_CONTRACT_VERSION = "contract_version"
    const val EXTRA_BOOK_ID = "book_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_AUTHOR = "author"
    const val EXTRA_PAGE_COUNT = "page_count"
    const val EXTRA_PUBLISHER = "publisher"
    const val EXTRA_PUBLISHED_YEAR = "published_year"
    /** Optional content:// URI with a read grant. Target v1 doesn't use it (covers are drawn, not photos). */
    const val EXTRA_COVER_URI = "cover_uri"

    /**
     * Library provider (sync): Lilies Lib's read-only ContentProvider. `content://AUTHORITY/books` returns one row
     * per book, with columns named like the picker extras ([EXTRA_BOOK_ID], [EXTRA_TITLE], ...).
     */
    const val PROVIDER_AUTHORITY = "com.anonymous.eighteeneleven.target"
    const val PROVIDER_BOOKS_PATH = "books"

    /** Declared by Lilies Lib (protectionLevel dangerous): Android asks the user once to allow Target to read it. */
    const val PERMISSION_READ_LIBRARY = "com.anonymous.eighteeneleven.permission.READ_LIBRARY"
}

/** A book as Lilies Lib returned it. [externalId] is Lilies Lib's own stable id, kept as text. */
data class PickedBook(
    val externalId: String,
    val title: String,
    val author: String?,
    val pageCount: Int?,
    val publisher: String?,
    val publishedYear: Int?,
)

/**
 * Reads a picker result through [get] (an extras lookup). Null = nothing usable: missing id/title, or a
 * contract version newer than Target knows. A missing version is read as 1. Numbers may arrive as any
 * number type or as text (React Native bridges differ).
 */
fun parsePickedBook(get: (String) -> Any?): PickedBook? {
    val version = intOf(get(LiliesLibContract.EXTRA_CONTRACT_VERSION)) ?: 1
    if (version < 1 || version > LiliesLibContract.CONTRACT_VERSION) return null
    val id = textOf(get(LiliesLibContract.EXTRA_BOOK_ID)) ?: return null
    val title = textOf(get(LiliesLibContract.EXTRA_TITLE)) ?: return null
    return PickedBook(
        externalId = id,
        title = title,
        author = textOf(get(LiliesLibContract.EXTRA_AUTHOR)),
        pageCount = intOf(get(LiliesLibContract.EXTRA_PAGE_COUNT))?.takeIf { it > 0 },
        publisher = textOf(get(LiliesLibContract.EXTRA_PUBLISHER)),
        publishedYear = intOf(get(LiliesLibContract.EXTRA_PUBLISHED_YEAR))?.takeIf { it in 1..9999 },
    )
}

/** The book_id Lilies Lib answered ACTION_ADD_BOOK with; null when unusable or a version Target doesn't know. */
fun parseAddedBookId(get: (String) -> Any?): String? {
    val version = intOf(get(LiliesLibContract.EXTRA_CONTRACT_VERSION)) ?: 1
    if (version < 1 || version > LiliesLibContract.CONTRACT_VERSION) return null
    return textOf(get(LiliesLibContract.EXTRA_BOOK_ID))
}

/**
 * A synced book: Lilies Lib wins for title/author/pages/publisher/year, but an empty Lilies field never erases what
 * Target has. Everything Target-only (status, rating, notes, goal, cover, sessions) is untouched.
 */
fun com.ridvan.target.data.local.entity.Book.refreshedFrom(fresh: PickedBook): com.ridvan.target.data.local.entity.Book = copy(
    title = fresh.title,
    author = fresh.author ?: author,
    totalPages = fresh.pageCount ?: totalPages,
    publisher = fresh.publisher ?: publisher,
    publishedYear = fresh.publishedYear ?: publishedYear,
)

private fun textOf(value: Any?): String? = when (value) {
    null -> null
    is String -> value
    is Number -> if (value.toDouble() % 1.0 == 0.0) value.toLong().toString() else value.toString()
    else -> value.toString()
}?.trim()?.ifEmpty { null }

private fun intOf(value: Any?): Int? = when (value) {
    is Int -> value
    is Number -> value.toDouble().takeIf { it % 1.0 == 0.0 && it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }?.toInt()
    is String -> value.trim().toDoubleOrNull()?.let { intOf(it) }
    else -> null
}
