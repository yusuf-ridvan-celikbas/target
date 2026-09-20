package com.ridvan.target.ui.planner

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.ridvan.target.data.local.entity.RecurrenceUnit
import java.time.DayOfWeek

/**
 * Opens the system Clock app pre-filled to create a real device alarm — separate from this
 * app's own silent AlarmManager reminder notifications (data/notifications/). ACTION_SET_ALARM
 * has no concept of a specific future date, only time-of-day plus an optional weekday repeat,
 * so a one-time event several days out still fires at the *next* occurrence of this time, not
 * the event's real date — callers surface that via a caption, not by hiding the action.
 * Returns false rather than throwing if no app on the device can handle the intent.
 */
fun launchSetDeviceAlarmIntent(
    context: Context,
    title: String,
    minuteOfDay: Int,
    recurrenceUnit: RecurrenceUnit?,
    weekdays: Set<DayOfWeek>,
): Boolean {
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, minuteOfDay / 60)
        putExtra(AlarmClock.EXTRA_MINUTES, minuteOfDay % 60)
        putExtra(AlarmClock.EXTRA_MESSAGE, title)
        if (recurrenceUnit == RecurrenceUnit.WEEK && weekdays.isNotEmpty()) {
            // ISO DayOfWeek.value is Monday=1..Sunday=7; Calendar day constants are
            // Sunday=1..Saturday=7 — (isoValue % 7) + 1 converts between the two.
            putExtra(AlarmClock.EXTRA_DAYS, ArrayList(weekdays.map { (it.value % 7) + 1 }))
        }
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
