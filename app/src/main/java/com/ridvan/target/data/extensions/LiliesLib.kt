package com.ridvan.target.data.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/** Android side of the Lilies Lib extension: install check, picker intent, result parsing, download link. */
object LiliesLib {
    /** Needs the `<queries>` entry in the manifest on Android 11+. */
    fun isInstalled(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(LiliesLibContract.PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(LiliesLibContract.PACKAGE, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * The picker to launch, or null when nothing answers [LiliesLibContract.ACTION_PICK_BOOK] (Lilies Lib missing
     * or too old). Debug builds fall back to Target's own fake picker (src/debug) so the flow can be tested.
     */
    fun pickerIntent(context: Context): Intent? {
        fun forPackage(pkg: String) = Intent(LiliesLibContract.ACTION_PICK_BOOK)
            .setPackage(pkg)
            .putExtra(LiliesLibContract.EXTRA_CONTRACT_VERSION, LiliesLibContract.CONTRACT_VERSION)
        return listOf(LiliesLibContract.PACKAGE, context.packageName)
            .map(::forPackage)
            .firstOrNull { it.resolveActivity(context.packageManager) != null }
    }

    /** "Send to Lilies Lib" for [book], or null when nothing answers ACTION_ADD_BOOK (debug: the fake picker does). */
    fun addBookIntent(context: Context, book: com.ridvan.target.data.local.entity.Book): Intent? =
        listOf(LiliesLibContract.PACKAGE, context.packageName)
            .map { pkg ->
                Intent(LiliesLibContract.ACTION_ADD_BOOK).setPackage(pkg)
                    .putExtra(LiliesLibContract.EXTRA_CONTRACT_VERSION, LiliesLibContract.CONTRACT_VERSION)
                    .putExtra(LiliesLibContract.EXTRA_TITLE, book.title)
                    .apply {
                        book.author?.let { putExtra(LiliesLibContract.EXTRA_AUTHOR, it) }
                        book.totalPages?.let { putExtra(LiliesLibContract.EXTRA_PAGE_COUNT, it) }
                        book.publisher?.let { putExtra(LiliesLibContract.EXTRA_PUBLISHER, it) }
                        book.publishedYear?.let { putExtra(LiliesLibContract.EXTRA_PUBLISHED_YEAR, it) }
                    }
            }
            .firstOrNull { it.resolveActivity(context.packageManager) != null }

    fun parseAddResult(resultCode: Int, data: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        val extras = data?.extras ?: return null
        @Suppress("DEPRECATION")
        return parseAddedBookId { key -> extras.get(key) }
    }

    fun parseResult(resultCode: Int, data: Intent?): PickedBook? {
        if (resultCode != Activity.RESULT_OK) return null
        val extras = data?.extras ?: return null
        @Suppress("DEPRECATION")
        return parsePickedBook { key -> extras.get(key) }
    }

    /** Opens the release APK link in the browser. False when no browser is there. */
    fun openDownload(context: Context): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LiliesLibContract.DOWNLOAD_URL)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
