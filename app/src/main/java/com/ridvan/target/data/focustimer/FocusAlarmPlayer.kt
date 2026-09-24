package com.ridvan.target.data.focustimer

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays one Focus Timer sound (work/break start/end — see FocusSoundEvent): a ringtone (system
 * default, or a user-chosen ringtone/audio file Uri from AppPreferences.focusSoundUris) plus an
 * optional short vibration. Foreground-only by design (see CLAUDE.md's Focus Timer scope) — this is
 * called directly from the running countdown, not from a scheduled AlarmManager alarm.
 */
object FocusAlarmPlayer {
    private var activeRingtone: Ringtone? = null

    fun playAlarm(context: Context, soundUri: String?, vibrate: Boolean) {
        activeRingtone?.stop()
        val uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
        activeRingtone = uri?.let { runCatching { RingtoneManager.getRingtone(context, it) }.getOrNull() }
        activeRingtone?.play()

        if (vibrate) {
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(effect)
            }
        }
    }
}
