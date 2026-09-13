package com.ridvan.target.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun shareCsvFile(context: Context, file: File, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/**
 * Reads the full text content of a picked document [uri]. Some providers (Google Drive in
 * particular) can hand back a "virtual" document that has no raw byte stream of its own and
 * throws FileNotFoundException from a plain openInputStream — those must be read via one of
 * their declared alternate MIME types instead. Returns null on any failure rather than throwing,
 * since this runs from an ActivityResult callback where an uncaught exception crashes the app.
 */
fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (e: Exception) {
        readVirtualDocumentText(context, uri)
    }
}

private fun readVirtualDocumentText(context: Context, uri: Uri): String? {
    return try {
        val streamTypes = context.contentResolver.getStreamTypes(uri, "*/*") ?: return null
        val mimeType = streamTypes.firstOrNull { it.startsWith("text/") } ?: streamTypes.firstOrNull() ?: return null
        context.contentResolver.openTypedAssetFileDescriptor(uri, mimeType, null)
            ?.createInputStream()?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (e: Exception) {
        null
    }
}
