package com.ridvan.target.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.ridvan.target.data.extensions.LiliesLibContract as C

/**
 * Debug-only stand-in for Lilies Lib's library provider (sync). Same columns as the real one; the fake picker's
 * books come back with changed details so a sync is visible: Dune gets 500 pages, the title-only book gets an author.
 */
class FakeLiliesProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = arrayOf(C.EXTRA_CONTRACT_VERSION, C.EXTRA_BOOK_ID, C.EXTRA_TITLE, C.EXTRA_AUTHOR, C.EXTRA_PAGE_COUNT, C.EXTRA_PUBLISHER, C.EXTRA_PUBLISHED_YEAR)
        return MatrixCursor(columns).apply {
            addRow(arrayOf<Any?>(1, "fake-1", "Dune", "Frank Herbert", 500, "Ace", 1965))
            addRow(arrayOf<Any?>(1, "17", "Kürk Mantolu Madonna", "Sabahattin Ali", 160, "YKY", 1943))
            addRow(arrayOf<Any?>(1, "fake-3", "Untitled notebook", "Synced Author", null, null, null))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
