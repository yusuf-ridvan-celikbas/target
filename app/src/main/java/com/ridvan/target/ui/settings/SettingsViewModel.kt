package com.ridvan.target.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.AppPreferences
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences: AppPreferences = (application as TargetApplication).preferences

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkMode

    fun setDarkMode(enabled: Boolean) {
        preferences.setDarkMode(enabled)
    }
}
