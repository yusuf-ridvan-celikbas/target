package com.ridvan.target.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {
    private val fake: (String, String) -> String = { c, s -> "$s|$c" }

    @Test
    fun generatesTwelveUnambiguousChars() {
        repeat(200) {
            val c = RecoveryCode.generate()
            assertEquals(12, c.length)
            assertTrue(c.all { it in RecoveryCode.ALPHABET })
        }
        assertTrue(RecoveryCode.ALPHABET.none { it in "0O1IL" })
    }

    @Test
    fun formatsAndNormalizes() {
        assertEquals("ABCD-EFGH-JKMN", RecoveryCode.format("ABCDEFGHJKMN"))
        assertEquals("ABCDEFGHJKMN", RecoveryCode.normalize(" abcd-efgh jkmn "))
    }

    @Test
    fun verifiesRegardlessOfFormatting() {
        val h = RecoveryCode.hash("ABCDEFGHJKMN", "salt", fake)
        assertTrue(RecoveryCode.verify("abcd-efgh-jkmn", "salt", h, fake))
        assertFalse(RecoveryCode.verify("ABCD-EFGH-JKMP", "salt", h, fake))
        assertFalse(RecoveryCode.verify("ABCD-EFGH-JKMN", null, null, fake))
    }
}
