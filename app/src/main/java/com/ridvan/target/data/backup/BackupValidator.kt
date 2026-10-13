package com.ridvan.target.data.backup

/** Pure checks on a candidate backup file; the Android-side reads live in [BackupManager]. */
object BackupValidator {
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** Tables a genuine Target database must contain. */
    val REQUIRED_TABLES = setOf("users", "exams", "courses", "exam_types")

    enum class Result { OK, NOT_SQLITE, NOT_TARGET, TOO_NEW }

    fun hasSqliteHeader(header: ByteArray): Boolean =
        header.size >= SQLITE_MAGIC.size && SQLITE_MAGIC.indices.all { header[it] == SQLITE_MAGIC[it] }

    fun validate(header: ByteArray, tables: Set<String>, userVersion: Int, currentVersion: Int): Result = when {
        !hasSqliteHeader(header) -> Result.NOT_SQLITE
        !tables.containsAll(REQUIRED_TABLES) -> Result.NOT_TARGET
        userVersion > currentVersion -> Result.TOO_NEW
        else -> Result.OK
    }
}
