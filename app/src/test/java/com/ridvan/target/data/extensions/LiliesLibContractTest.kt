package com.ridvan.target.data.extensions

import com.ridvan.target.data.extensions.LiliesLibContract as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiliesLibContractTest {
    private fun parse(vararg pairs: Pair<String, Any?>) = parsePickedBook(mapOf(*pairs)::get)

    @Test
    fun fullV1Answer() {
        val book = parse(
            C.EXTRA_CONTRACT_VERSION to 1, C.EXTRA_BOOK_ID to "abc", C.EXTRA_TITLE to " Dune ", C.EXTRA_AUTHOR to "Frank Herbert",
            C.EXTRA_PAGE_COUNT to 412, C.EXTRA_PUBLISHER to "Ace", C.EXTRA_PUBLISHED_YEAR to 1965, C.EXTRA_COVER_URI to "content://x/1",
        )
        assertEquals(PickedBook("abc", "Dune", "Frank Herbert", 412, "Ace", 1965), book)
    }

    @Test
    fun numbersFromReactNativeAsDoubleOrText() {
        val book = parse(C.EXTRA_CONTRACT_VERSION to 1.0, C.EXTRA_BOOK_ID to 17.0, C.EXTRA_TITLE to "T", C.EXTRA_PAGE_COUNT to "160", C.EXTRA_PUBLISHED_YEAR to 1943L)
        assertEquals(PickedBook("17", "T", null, 160, null, 1943), book)
    }

    @Test
    fun missingVersionIsOneAndBadNumbersAreDropped() {
        val book = parse(C.EXTRA_BOOK_ID to "1", C.EXTRA_TITLE to "T", C.EXTRA_AUTHOR to "  ", C.EXTRA_PAGE_COUNT to 0, C.EXTRA_PUBLISHED_YEAR to "soon")
        assertEquals(PickedBook("1", "T", null, null, null, null), book)
    }

    @Test
    fun unusableAnswersGiveNothing() {
        assertNull(parse())
        assertNull(parse(C.EXTRA_BOOK_ID to "1"))
        assertNull(parse(C.EXTRA_TITLE to "T"))
        assertNull(parse(C.EXTRA_BOOK_ID to "1", C.EXTRA_TITLE to "   "))
        assertNull(parse(C.EXTRA_CONTRACT_VERSION to 2, C.EXTRA_BOOK_ID to "1", C.EXTRA_TITLE to "T"))
        assertNull(parse(C.EXTRA_CONTRACT_VERSION to 0, C.EXTRA_BOOK_ID to "1", C.EXTRA_TITLE to "T"))
    }

    @Test
    fun syncLiliesWinsButEmptyNeverErases() {
        val book = com.ridvan.target.data.local.entity.Book(
            id = 5, title = "Old", author = "A", totalPages = 300, publisher = "P", publishedYear = 2000,
            rating = 4, notes = "mine", isWantToRead = true, externalSource = C.SOURCE, externalId = "x",
        )
        val synced = book.refreshedFrom(PickedBook("x", "New", null, 412, "Q", null))
        assertEquals(book.copy(title = "New", totalPages = 412, publisher = "Q"), synced)
    }

    @Test
    fun addAnswerGivesTheNewId() {
        assertEquals("abc", parseAddedBookId(mapOf<String, Any?>(C.EXTRA_CONTRACT_VERSION to 1, C.EXTRA_BOOK_ID to "abc")::get))
        assertEquals("42", parseAddedBookId(mapOf<String, Any?>(C.EXTRA_BOOK_ID to 42.0)::get))
        assertNull(parseAddedBookId(mapOf<String, Any?>(C.EXTRA_CONTRACT_VERSION to 1)::get))
        assertNull(parseAddedBookId(mapOf<String, Any?>(C.EXTRA_CONTRACT_VERSION to 2, C.EXTRA_BOOK_ID to "abc")::get))
    }

    @Test
    fun extensionListRoundTrip() {
        assertEquals(emptySet<TargetExtension>(), parseExtensions(null))
        assertEquals(setOf(TargetExtension.LILIES_LIB), parseExtensions("FOO,LILIES_LIB"))
        assertEquals("LILIES_LIB", serializeExtensions(setOf(TargetExtension.LILIES_LIB)))
        assertNull(serializeExtensions(emptySet()))
    }
}
