package com.ridvan.target.ui.settings

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.AppLanguage
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.BannerColor
import com.ridvan.target.data.local.NotificationLeadTime
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences: AppPreferences = (application as TargetApplication).preferences

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkMode

    fun setDarkMode(enabled: Boolean) {
        preferences.setDarkMode(enabled)
    }

    val useBlueAppIcon: StateFlow<Boolean> = preferences.useBlueAppIcon

    fun setUseBlueAppIcon(enabled: Boolean) {
        val app = getApplication<Application>()
        val packageManager = app.packageManager
        val defaultAlias = ComponentName(app, "${app.packageName}.AppIconDefault")
        val blueAlias = ComponentName(app, "${app.packageName}.AppIconBlue")

        packageManager.setComponentEnabledSetting(
            defaultAlias,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        packageManager.setComponentEnabledSetting(
            blueAlias,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        preferences.setUseBlueAppIcon(enabled)
    }

    val appLanguage: StateFlow<AppLanguage> = preferences.appLanguage

    fun setAppLanguage(language: AppLanguage) {
        preferences.setAppLanguage(language)
    }

    val bannerColor: StateFlow<BannerColor> = preferences.bannerColor

    fun setBannerColor(color: BannerColor) {
        preferences.setBannerColor(color)
    }

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
    }

    val notificationLeadTime: StateFlow<NotificationLeadTime> = preferences.notificationLeadTime

    fun setNotificationLeadTime(leadTime: NotificationLeadTime) {
        preferences.setNotificationLeadTime(leadTime)
    }
}
