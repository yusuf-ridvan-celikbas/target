package com.ridvan.target.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    TURKISH("tr"),
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val systemDarkDefault =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, systemDarkDefault))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    private val _useBlueAppIcon = MutableStateFlow(prefs.getBoolean(KEY_APP_ICON_BLUE, false))
    val useBlueAppIcon: StateFlow<Boolean> = _useBlueAppIcon.asStateFlow()

    fun setUseBlueAppIcon(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_ICON_BLUE, enabled).apply()
        _useBlueAppIcon.value = enabled
    }

    private val systemLanguageDefault =
        if (context.resources.configuration.locales.get(0).language == AppLanguage.TURKISH.languageTag) {
            AppLanguage.TURKISH
        } else {
            AppLanguage.ENGLISH
        }

    private val _appLanguage = MutableStateFlow(
        prefs.getString(KEY_APP_LANGUAGE, null)?.let { tag ->
            AppLanguage.entries.firstOrNull { it.languageTag == tag }
        } ?: systemLanguageDefault,
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    init {
        // Keep java.util.Locale.getDefault() (used by SimpleDateFormat, etc.) in sync with the
        // persisted preference on every app start, not just when the user actively toggles it.
        Locale.setDefault(Locale(_appLanguage.value.languageTag))
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.languageTag).apply()
        Locale.setDefault(Locale(language.languageTag))
        _appLanguage.value = language
    }

    var currentUserId: Long?
        get() = prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER_ID) else putLong(KEY_USER_ID, value)
            }.apply()
        }

    private companion object {
        const val PREFS_NAME = "target_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_APP_ICON_BLUE = "app_icon_blue"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_USER_ID = "current_user_id"
        const val NO_USER = -1L
    }
}
