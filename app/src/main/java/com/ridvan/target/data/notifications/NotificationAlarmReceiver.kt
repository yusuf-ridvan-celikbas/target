package com.ridvan.target.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a scheduled reminder's moment arrives — shows it, then re-arms the alarm for
 *  whatever's next (see NotificationScheduler.fireDueAndReschedule). */
class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationScheduler.fireDueAndReschedule(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
