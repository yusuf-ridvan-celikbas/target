package com.ridvan.target.data.extensions

import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.dao.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Optional companion apps Target can talk to on the phone (never over the network). Off by default. */
enum class TargetExtension { LILIES_LIB }

/** `users.enabledExtensions` is a comma list of [TargetExtension] names; null/blank = none. Unknown names are ignored. */
fun parseExtensions(raw: String?): Set<TargetExtension> =
    raw.orEmpty().split(',').mapNotNull { name -> TargetExtension.entries.firstOrNull { it.name == name.trim() } }.toSet()

fun serializeExtensions(extensions: Set<TargetExtension>): String? =
    TargetExtension.entries.filter { it in extensions }.joinToString(",") { it.name }.ifEmpty { null }

/** The signed-in account's enabled extensions, live (follows account switches). */
@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionRepository(
    private val userDao: UserDao,
    preferences: AppPreferences,
    scope: CoroutineScope,
) {
    val enabled: StateFlow<Set<TargetExtension>> = preferences.currentUserIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(emptySet()) else userDao.getById(id).map { parseExtensions(it?.enabledExtensions) } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    suspend fun setEnabled(userId: Long, extension: TargetExtension, on: Boolean) {
        val current = parseExtensions(userDao.getById(userId).first()?.enabledExtensions)
        userDao.setEnabledExtensions(userId, serializeExtensions(if (on) current + extension else current - extension))
    }
}
