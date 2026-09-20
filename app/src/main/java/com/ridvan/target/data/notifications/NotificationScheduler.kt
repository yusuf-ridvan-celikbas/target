package com.ridvan.target.data.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.AppLanguage
import com.ridvan.target.ui.common.examDisplayLabel
import com.ridvan.target.ui.planner.occurrencesInRange
import com.ridvan.target.ui.planner.timeRangeLabel
import com.ridvan.target.ui.planner.toLocalDate
import com.ridvan.target.ui.planner.toStartOfDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Schedules local reminders for Planner events and non-sectioned exam/section dates.
 *
 * No per-notification row is persisted anywhere: rather than arming one alarm per future
 * occurrence (unbounded for a recurring event), [reschedule] recomputes the single *next*
 * reminder moment across every source from scratch every time it's called (on a relevant data
 * change, app start, device boot, or right after the previous alarm fires) and keeps exactly
 * one AlarmManager alarm armed for it — the same "derive, don't persist" convention as the
 * rest of the app's net-score/blank-count-style computed fields. A reminder whose moment has
 * already passed (e.g. the app was closed through it) is simply never generated for a future
 * window, so it's silently skipped rather than fired late.
 *
 * Second deliberate java.time usage point in the app, alongside ui/planner's own — reuses
 * PlannerOccurrences'/PlannerDates' recurrence math directly rather than duplicating it, since
 * core library desugaring is already enabled app-wide for that first usage.
 */
object NotificationScheduler {
    private const val CHANNEL_ID = "reminders"
    private const val ALARM_REQUEST_CODE = 1001

    /** Fixed local reminder time for anything with no time-of-day of its own — an all-day
     *  Planner event (including a Birthday, which is always all-day), a non-sectioned Exam
     *  date, or a Section date. Not user-configurable yet — see the notifications memory. */
    private const val ALL_DAY_REMINDER_HOUR = 9

    /** How far ahead to look for the next occurrence — comfortably past a yearly recurrence. */
    private const val HORIZON_DAYS = 400L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Recomputes the next reminder moment and (re)arms the single pending alarm for it — or
     *  cancels it when notifications are off, no user is signed in, or nothing is due. */
    suspend fun reschedule(context: Context) {
        val app = context.applicationContext as TargetApplication
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmPendingIntent(context)
        val userId = app.preferences.currentUserId

        if (!app.preferences.notificationsEnabled.value || userId == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = Instant.now()
        val next = buildCandidates(app, userId).filter { it.whenInstant.isAfter(now) }.minByOrNull { it.whenInstant }

        if (next == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.whenInstant.toEpochMilli(), pendingIntent)
        } else {
            // Exact-alarm permission not granted yet — still arm an inexact alarm rather than
            // silently reminding never, the user can grant it later from Settings.
            alarmManager.set(AlarmManager.RTC_WAKEUP, next.whenInstant.toEpochMilli(), pendingIntent)
        }
    }

    /** Called from the alarm receiver: shows every reminder whose moment has arrived, then
     *  re-arms the alarm for whatever's next. */
    suspend fun fireDueAndReschedule(context: Context) {
        val app = context.applicationContext as TargetApplication
        val userId = app.preferences.currentUserId
        if (!app.preferences.notificationsEnabled.value || userId == null) return

        val now = Instant.now()
        buildCandidates(app, userId).filter { !it.whenInstant.isAfter(now) }.forEach { showNotification(context, it) }

        reschedule(context)
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private data class ReminderCandidate(
        val whenInstant: Instant,
        val notificationId: Int,
        val title: String,
        val body: String,
    )

    private suspend fun buildCandidates(app: TargetApplication, userId: Long): List<ReminderCandidate> {
        val database = app.database
        val events = database.plannerEventDao().getAllWithLinksByUserId(userId).first()
        val completions = database.plannerEventCompletionDao().getAllByUserId(userId).first()
        val exams = database.examDao().getAllWithTypeByUserId(userId).first()
        val sections = database.sectionDao().getByUserId(userId).first()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val horizonEnd = today.plusDays(HORIZON_DAYS)
        val leadMinutes = app.preferences.notificationLeadTime.value.minutes
        val strings = localizedContext(app, app.preferences.appLanguage.value)
        val completedKeys = completions.map { it.plannerEventId to it.occurrenceDate }.toSet()

        val eventCandidates = events.flatMap { withLinks ->
            val event = withLinks.event
            occurrencesInRange(event, today, horizonEnd).mapNotNull { date ->
                if ((event.id to date.toStartOfDayMillis(zone)) in completedKeys) return@mapNotNull null
                ReminderCandidate(
                    whenInstant = reminderInstant(date, event.startMinuteOfDay, leadMinutes, zone),
                    notificationId = notificationId("planner", event.id, date.toEpochDay()),
                    title = event.title,
                    body = timeRangeLabel(event) ?: event.notes?.takeIf { it.isNotBlank() }
                        ?: strings.getString(R.string.notif_planner_default_body),
                )
            }
        }

        val examLabels = exams.associate { it.exam.id to examDisplayLabel(it.exam) }
        val examCandidates = exams.mapNotNull { item ->
            if (item.exam.hasSections) return@mapNotNull null
            val examDate = item.exam.examDate ?: return@mapNotNull null
            val date = examDate.toLocalDate(zone)
            if (date !in today..horizonEnd) return@mapNotNull null
            ReminderCandidate(
                whenInstant = reminderInstant(date, null, 0, zone),
                notificationId = notificationId("exam", item.exam.id, date.toEpochDay()),
                title = strings.getString(R.string.notif_exam_title),
                body = examLabels[item.exam.id] ?: item.exam.name,
            )
        }

        val sectionCandidates = sections.mapNotNull { section ->
            val sectionDate = section.date ?: return@mapNotNull null
            val date = sectionDate.toLocalDate(zone)
            if (date !in today..horizonEnd) return@mapNotNull null
            val examLabel = examLabels[section.examId] ?: return@mapNotNull null
            ReminderCandidate(
                whenInstant = reminderInstant(date, null, 0, zone),
                notificationId = notificationId("section", section.id, date.toEpochDay()),
                title = strings.getString(R.string.notif_section_title),
                body = "$examLabel – ${section.name}",
            )
        }

        return eventCandidates + examCandidates + sectionCandidates
    }

    private fun reminderInstant(date: LocalDate, minuteOfDay: Int?, leadMinutes: Int, zone: ZoneId): Instant =
        if (minuteOfDay != null) {
            date.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).minusMinutes(leadMinutes.toLong()).toInstant()
        } else {
            date.atTime(ALL_DAY_REMINDER_HOUR, 0).atZone(zone).toInstant()
        }

    private fun notificationId(kind: String, sourceId: Long, epochDay: Long): Int {
        var result = kind.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + epochDay.hashCode()
        return result and 0x7fffffff
    }

    private fun showNotification(context: Context, candidate: ReminderCandidate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(candidate.title)
            .setContentText(candidate.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(candidate.notificationId, notification)
    }

    /** Notification text is built outside any Activity, so it can't rely on
     *  MainActivity.attachBaseContext()'s locale override for values/strings.xml lookups — this
     *  re-applies the same app-language preference to a throwaway Context just for these
     *  getString() calls, the same trick used once already for the drawer banner/theme. */
    private fun localizedContext(base: Context, language: AppLanguage): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale(language.languageTag))
        return base.createConfigurationContext(configuration)
    }
}
