package com.ridvan.target.data.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat
import com.ridvan.target.data.local.dao.BookDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Can Target read Lilies Lib's library provider right now? */
enum class LiliesAccess { READY, NEEDS_PERMISSION, UNSUPPORTED }

sealed interface LiliesSyncResult {
    /** [updated] books changed, [missing] linked books Lilies Lib no longer has (left as they are). */
    data class Done(val updated: Int, val missing: Int) : LiliesSyncResult
    data object NeedsPermission : LiliesSyncResult
    data object Unsupported : LiliesSyncResult
}

/** Refreshes books picked from Lilies Lib from its read-only provider (docs/lilies-lib-contract.md, "Library provider"). */
object LiliesSync {
    /** Debug builds fall back to Target's own fake provider (src/debug) when Lilies Lib has none. */
    private fun authority(context: Context): String? =
        listOf(LiliesLibContract.PROVIDER_AUTHORITY, "${context.packageName}.fakelilies")
            .firstOrNull { context.packageManager.resolveContentProvider(it, 0) != null }

    fun access(context: Context): LiliesAccess {
        val authority = authority(context) ?: return LiliesAccess.UNSUPPORTED
        if (authority != LiliesLibContract.PROVIDER_AUTHORITY) return LiliesAccess.READY
        val defined = try {
            context.packageManager.getPermissionInfo(LiliesLibContract.PERMISSION_READ_LIBRARY, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        return when {
            !defined -> LiliesAccess.READY // provider without the permission: let the query decide
            ContextCompat.checkSelfPermission(context, LiliesLibContract.PERMISSION_READ_LIBRARY) == PackageManager.PERMISSION_GRANTED -> LiliesAccess.READY
            else -> LiliesAccess.NEEDS_PERMISSION
        }
    }

    /** Syncs every linked book of [userId], or only [bookId]. Never adds or deletes books. */
    suspend fun sync(context: Context, bookDao: BookDao, userId: Long, bookId: Long? = null): LiliesSyncResult = withContext(Dispatchers.IO) {
        when (access(context)) {
            LiliesAccess.UNSUPPORTED -> return@withContext LiliesSyncResult.Unsupported
            LiliesAccess.NEEDS_PERMISSION -> return@withContext LiliesSyncResult.NeedsPermission
            LiliesAccess.READY -> Unit
        }
        val linked = bookDao.getExternal(userId, LiliesLibContract.SOURCE).filter { bookId == null || it.id == bookId }
        if (linked.isEmpty()) return@withContext LiliesSyncResult.Done(0, 0)
        val fresh = try {
            readBooks(context)
        } catch (_: SecurityException) {
            return@withContext LiliesSyncResult.NeedsPermission
        } ?: return@withContext LiliesSyncResult.Unsupported
        var updated = 0
        var missing = 0
        linked.forEach { book ->
            val row = fresh[book.externalId]
            if (row == null) {
                missing++
            } else {
                val refreshed = book.refreshedFrom(row)
                if (refreshed != book) {
                    bookDao.update(refreshed)
                    updated++
                }
            }
        }
        LiliesSyncResult.Done(updated, missing)
    }

    /** Every book the provider lists, by id; null when the provider can't be read. */
    private fun readBooks(context: Context): Map<String, PickedBook>? {
        val authority = authority(context) ?: return null
        val uri = Uri.parse("content://$authority/${LiliesLibContract.PROVIDER_BOOKS_PATH}")
        val cursor = try {
            context.contentResolver.query(uri, null, null, null, null)
        } catch (e: SecurityException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
        return cursor.use { c ->
            buildMap {
                while (c.moveToNext()) {
                    parsePickedBook { column -> c.valueOf(column) }?.let { put(it.externalId, it) }
                }
            }
        }
    }

    private fun Cursor.valueOf(column: String): Any? {
        val index = getColumnIndex(column)
        if (index < 0) return null
        return when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_STRING -> getString(index)
            else -> null
        }
    }
}
