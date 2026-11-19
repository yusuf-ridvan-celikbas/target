package com.ridvan.target.ui.reading

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.extensions.LiliesLib
import com.ridvan.target.data.extensions.LiliesLibContract
import com.ridvan.target.data.extensions.PickedBook
import com.ridvan.target.data.extensions.TargetExtension
import com.ridvan.target.data.local.dao.BookDao
import com.ridvan.target.data.local.entity.Book
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Add from Lilies Lib": null (no button) unless the extension is on for this account and Lilies Lib is
 * installed (debug builds: or the fake picker answers). The returned action launches the picker;
 * [onPicked] only runs for a usable result — cancel or an empty answer does nothing.
 */
@Composable
fun rememberLiliesBookPicker(onPicked: (PickedBook) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val application = context.applicationContext as TargetApplication
    val enabled by application.extensions.enabled.collectAsState()
    val latestOnPicked by rememberUpdatedState(onPicked)
    var available by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        available = LiliesLib.isInstalled(context) || LiliesLib.pickerIntent(context) != null
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        LiliesLib.parseResult(result.resultCode, result.data)?.let(latestOnPicked)
    }
    if (TargetExtension.LILIES_LIB !in enabled || !available) return null
    return {
        val intent = LiliesLib.pickerIntent(context)
        val launched = intent != null && try {
            launcher.launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
        if (!launched) Toast.makeText(context, R.string.reading_lilies_no_picker, Toast.LENGTH_LONG).show()
    }
}

/** Saves [picked] as a new book of [userId] (status Reading, random cover); false when that account already has it. */
suspend fun BookDao.addPickedBook(userId: Long, picked: PickedBook): Boolean {
    if (findExternal(userId, LiliesLibContract.SOURCE, picked.externalId) != null) return false
    insert(
        Book(
            userId = userId,
            title = picked.title,
            author = picked.author,
            totalPages = picked.pageCount,
            publisher = picked.publisher,
            publishedYear = picked.publishedYear,
            coverColor = Random.nextInt(coverPalette.size),
            externalSource = LiliesLibContract.SOURCE,
            externalId = picked.externalId,
        ),
    )
    return true
}

/**
 * Runs [onReady] once Target may read Lilies Lib's library: asks Android's permission first when needed;
 * a toast explains when Lilies Lib can't share its library or the user said no.
 */
@Composable
fun rememberLiliesAccess(onReady: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val latestOnReady by rememberUpdatedState(onReady)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latestOnReady() else Toast.makeText(context, R.string.extensions_sync_needs_permission, Toast.LENGTH_LONG).show()
    }
    return {
        when (com.ridvan.target.data.extensions.LiliesSync.access(context)) {
            com.ridvan.target.data.extensions.LiliesAccess.READY -> latestOnReady()
            com.ridvan.target.data.extensions.LiliesAccess.NEEDS_PERMISSION -> permissionLauncher.launch(LiliesLibContract.PERMISSION_READ_LIBRARY)
            com.ridvan.target.data.extensions.LiliesAccess.UNSUPPORTED ->
                Toast.makeText(context, R.string.extensions_sync_unsupported, Toast.LENGTH_LONG).show()
        }
    }
}

/** Silent background sync when Reading/Library opens, only with Lilies Lib and its auto-sync option on. */
fun kotlinx.coroutines.CoroutineScope.autoSyncLilies(application: TargetApplication, bookDao: BookDao, userId: Long?) {
    val uid = userId ?: return
    launch {
        val on = com.ridvan.target.data.extensions.parseExtensions(application.database.userDao().getById(uid).first()?.enabledExtensions)
        if (TargetExtension.LILIES_LIB in on && TargetExtension.LILIES_LIB_AUTO_SYNC in on) {
            com.ridvan.target.data.extensions.LiliesSync.sync(application, bookDao, uid)
        }
    }
}
