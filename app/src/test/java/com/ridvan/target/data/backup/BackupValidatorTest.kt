package com.ridvan.target.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupValidatorTest {
    private val good = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(20)
    private val tables = BackupValidator.REQUIRED_TABLES + "books"

    @Test fun acceptsValidAndOlder() {
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 30, 30))
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 27, 30))
        assertEquals(BackupValidator.Result.OK, BackupValidator.validate(good, tables, 20, 30))
    }

    @Test fun rejectsNonSqlite() {
        assertEquals(BackupValidator.Result.NOT_SQLITE, BackupValidator.validate("hello world, not sqlite".toByteArray(), tables, 30, 30))
        assertEquals(BackupValidator.Result.NOT_SQLITE, BackupValidator.validate(ByteArray(3), tables, 30, 30))
    }

    @Test fun rejectsOtherDatabase() {
        assertEquals(BackupValidator.Result.NOT_TARGET, BackupValidator.validate(good, setOf("users"), 30, 30))
    }

    @Test fun rejectsNewerVersion() {
        assertEquals(BackupValidator.Result.TOO_NEW, BackupValidator.validate(good, tables, 31, 30))
    }
}
