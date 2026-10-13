package com.ridvan.target.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.ridvan.target.TargetApplication
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/** Whole-database backup/restore. Preferences are deliberately not part of it. */
object BackupManager {
    private const val DB_NAME = "target.db"
    private const val CURRENT_VERSION = 27

    fun defaultFileName(): String =
        "Target-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".db"

    private fun dbFile(context: Context) = context.getDatabasePath(DB_NAME)

    /** Folds the WAL into target.db so the main file alone is a complete snapshot. */
    private fun checkpoint(context: Context) {
        val db = (context.applicationContext as TargetApplication).database
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    /** Writes the whole database to [uri]. Returns false on any failure. */
    fun backupTo(context: Context, uri: Uri): Boolean = try {
        checkpoint(context)
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: return false
        out.use { o -> dbFile(context).inputStream().use { it.copyTo(o) } }
        (context.applicationContext as TargetApplication).preferences.setLastBackupAt(System.currentTimeMillis())
        true
    } catch (e: Exception) {
        false
    }

    /** Binary counterpart of readTextFromUri: falls back to a virtual document's alternate type. */
    private fun openPicked(context: Context, uri: Uri): InputStream? = try {
        context.contentResolver.openInputStream(uri)
    } catch (e: Exception) {
        try {
            val types = context.contentResolver.getStreamTypes(uri, "*/*")
            val mime = types?.firstOrNull()
            if (mime == null) null
            else context.contentResolver.openTypedAssetFileDescriptor(uri, mime, null)?.createInputStream()
        } catch (e2: Exception) {
            null
        }
    }

    private fun candidateFile(context: Context) = File(context.cacheDir, "restore-candidate.db")

    /** Copies the picked file into cache and validates it. Null means it could not be read. */
    fun stageAndValidate(context: Context, uri: Uri): BackupValidator.Result? {
        val candidate = candidateFile(context)
        return try {
            candidate.delete()
            val input = openPicked(context, uri) ?: return null
            input.use { i -> candidate.outputStream().use { i.copyTo(it) } }
            val header = ByteArray(16)
            candidate.inputStream().use { it.read(header) }
            if (!BackupValidator.hasSqliteHeader(header)) return BackupValidator.Result.NOT_SQLITE
            val db = SQLiteDatabase.openDatabase(candidate.path, null, SQLiteDatabase.OPEN_READONLY)
            val tables = mutableSetOf<String>()
            val version: Int
            try {
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    while (c.moveToNext()) tables += c.getString(0)
                }
                version = db.version
            } finally {
                db.close()
            }
            BackupValidator.validate(header, tables, version, CURRENT_VERSION)
        } catch (e: Exception) {
            BackupValidator.Result.NOT_SQLITE
        }
    }

    /** Replaces the live database with the staged candidate, then restarts the app. */
    fun restoreStagedAndRestart(context: Context): Boolean {
        val candidate = candidateFile(context)
        if (!candidate.exists()) return false
        try {
            val app = context.applicationContext as TargetApplication
            checkpoint(context)
            val safety = File(context.filesDir, "backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            dbFile(context).copyTo(File(safety, "before-restore-$stamp.db"), overwrite = true)

            // Decide now, while the candidate is at hand, whether the signed-in user survives.
            val userId = app.preferences.currentUserId
            val userExists = userId == null || userInCandidate(candidate, userId)

            app.database.close()
            val target = dbFile(context)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            candidate.copyTo(target, overwrite = true)
            candidate.delete()

            if (!userExists) {
                app.preferences.clearSessionRaw()
            }

            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            return false
        }
        exitProcess(0)
    }

    private fun userInCandidate(file: File, userId: Long): Boolean = try {
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            db.rawQuery("SELECT 1 FROM users WHERE id = ?", arrayOf(userId.toString())).use { it.moveToFirst() }
        } finally {
            db.close()
        }
    } catch (e: Exception) {
        true
    }
}
