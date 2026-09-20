package com.ridvan.target

import android.app.Application
import androidx.room.InvalidationTracker
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.TargetDatabase
import com.ridvan.target.data.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TargetApplication : Application() {
    val database: TargetDatabase by lazy { TargetDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureChannel(this)
        applicationScope.launch {
            database.examTypeDao().seedDefaultsIfEmpty()
            NotificationScheduler.reschedule(this@TargetApplication)
        }
        // Any write to a table a reminder is computed from reschedules the pending alarm
        // automatically, so individual ViewModels (ExamDetail/SectionDetail/PlannerHome, etc.)
        // never need to remember to call NotificationScheduler themselves — Room's own
        // invalidation tracking already powers every Flow-backed query in the app, this just
        // taps the same mechanism. Preference-only changes (enabled/lead time) and userId
        // changes (login/logout/switch) aren't table writes, so those reschedule from
        // AppPreferences itself instead — see its currentUserId setter.
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("exams", "sections", "planner_events", "planner_event_completions") {
                override fun onInvalidated(tables: Set<String>) {
                    applicationScope.launch { NotificationScheduler.reschedule(this@TargetApplication) }
                }
            }
        )
    }
}
