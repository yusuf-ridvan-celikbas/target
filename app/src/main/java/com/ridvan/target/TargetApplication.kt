package com.ridvan.target

import android.app.Application
import com.ridvan.target.data.local.TargetDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TargetApplication : Application() {
    val database: TargetDatabase by lazy { TargetDatabase.getInstance(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            database.examTypeDao().seedDefaultsIfEmpty()
        }
    }
}
