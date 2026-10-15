package com.ridvan.target.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupValidatorTest {
    private val good = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(20)
    private val tables = BackupValidator.REQUIRED_TABLES + "books"

    @Test fun acceptsValidAndOlder() {
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 31, 31))
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 27, 31))
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 20, 31))
    }

    @Test fun rejectsNonSqlite() {
        assertEquals(BackupValidator.Result.NOT_SQLITE, BackupValidator.validate("hello world, not sqlite".toByteArray(), tables, 31, 31))
        assertEquals(BackupValidator.Result.NOT_SQLITE, BackupValidator.validate(ByteArray(3), tables, 31, 31))
    }

    @Test fun rejectsOtherDatabase() {
        assertEquals(BackupValidator.Result.NOT_TARGET, BackupValidator.validate(good, setOf("users"), 31, 31))
    }

    @Test fun rejectsNewerVersion() {
        assertEquals(BackupValidator.Result.TOO_NEW, BackupValidator.validate(good, tables, 32, 31))
    }
}
