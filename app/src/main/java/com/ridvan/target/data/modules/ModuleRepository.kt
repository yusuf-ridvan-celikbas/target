package com.ridvan.target.data.modules

import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.dao.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The signed-in account's enabled modules, live: follows account switches and Settings changes. */
@OptIn(ExperimentalCoroutinesApi::class)
class ModuleRepository(
    private val userDao: UserDao,
    preferences: AppPreferences,
    private val scope: CoroutineScope,
) {
    val enabled: StateFlow<Set<AppModule>> = preferences.currentUserIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(ALL_MODULES) else userDao.getById(id).map { parseModules(it?.enabledModules) } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, ALL_MODULES)

    suspend fun save(userId: Long, modules: Set<AppModule>) {
        userDao.setEnabledModules(userId, serializeModules(modules))
    }
}
