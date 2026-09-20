package com.ridvan.target.data.local

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import com.ridvan.target.R
import com.ridvan.target.data.notifications.NotificationScheduler
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    TURKISH("tr"),
}

/**
 * Drawer banner + app accent color options, one per banner design in target-app-designs.
 * BLUE keeps the app's original Material You dynamic-color behavior exactly as before this
 * feature existed; every other option pins the theme to that banner's own flat color instead
 * (see TargetTheme.kt) — so switching away from Blue is the only thing that opts out of dynamic
 * color, and switching back to Blue fully restores it.
 */
enum class BannerColor(val hex: Long, val drawableRes: Int, val labelRes: Int) {
    BLUE(0xFF006EFFL, R.drawable.drawer_banner, R.string.color_blue),
    DARK_GRAY(0xFF282828L, R.drawable.drawer_banner_darkgray, R.string.color_dark_gray),
    GREEN(0xFF00FF1EL, R.drawable.drawer_banner_green, R.string.color_green),
    PINK(0xFFD900FFL, R.drawable.drawer_banner_pink, R.string.color_pink),
    POWDER_PINK(0xFFF1A3FFL, R.drawable.drawer_banner_powderpink, R.string.color_powder_pink),
    PURPLE(0xFF800080L, R.drawable.drawer_banner_purple, R.string.color_purple),
    RED(0xFFFF002AL, R.drawable.drawer_banner_red, R.string.color_red),
    YELLOW(0xFFFFD900L, R.drawable.drawer_banner_yellow, R.string.color_yellow);

    val color: Color get() = Color(hex)
}

/**
 * How far ahead of a *timed* Planner event's own start to remind — only meaningful for
 * events with a startMinuteOfDay. Untimed events, Birthdays, and non-sectioned Exam/Section
 * dates (none of which have a time-of-day of their own) always remind at a fixed 9:00 AM
 * instead, regardless of this preference — see NotificationScheduler.
 */
enum class NotificationLeadTime(val minutes: Int, val labelRes: Int) {
    AT_TIME(0, R.string.notification_lead_at_time),
    MIN_15(15, R.string.notification_lead_15),
    MIN_30(30, R.string.notification_lead_30),
    HOUR_1(60, R.string.notification_lead_60),
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

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

    private val _bannerColor = MutableStateFlow(
        prefs.getString(KEY_BANNER_COLOR, null)?.let { name ->
            runCatching { BannerColor.valueOf(name) }.getOrNull()
        } ?: BannerColor.BLUE,
    )
    val bannerColor: StateFlow<BannerColor> = _bannerColor.asStateFlow()

    fun setBannerColor(color: BannerColor) {
        prefs.edit().putString(KEY_BANNER_COLOR, color.name).apply()
        _bannerColor.value = color
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
            // Log Out / Switch Account / Login / Register all funnel through this setter, so
            // rescheduling here — rather than at each of those call sites — is enough to keep
            // the pending reminder alarm scoped to whichever account is actually signed in.
            rescheduleNotifications()
        }

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
        rescheduleNotifications()
    }

    private val _notificationLeadTime = MutableStateFlow(
        prefs.getString(KEY_NOTIFICATION_LEAD_TIME, null)?.let { name ->
            runCatching { NotificationLeadTime.valueOf(name) }.getOrNull()
        } ?: NotificationLeadTime.MIN_15,
    )
    val notificationLeadTime: StateFlow<NotificationLeadTime> = _notificationLeadTime.asStateFlow()

    fun setNotificationLeadTime(leadTime: NotificationLeadTime) {
        prefs.edit().putString(KEY_NOTIFICATION_LEAD_TIME, leadTime.name).apply()
        _notificationLeadTime.value = leadTime
        rescheduleNotifications()
    }

    private fun rescheduleNotifications() {
        CoroutineScope(Dispatchers.IO).launch { NotificationScheduler.reschedule(appContext) }
    }

    private companion object {
        const val PREFS_NAME = "target_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_APP_ICON_BLUE = "app_icon_blue"
        const val KEY_BANNER_COLOR = "banner_color"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_USER_ID = "current_user_id"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIFICATION_LEAD_TIME = "notification_lead_time"
        const val NO_USER = -1L
    }
}
