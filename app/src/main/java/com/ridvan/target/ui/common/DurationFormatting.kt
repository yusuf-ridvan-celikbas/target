package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R

/**
 * A duration in minutes, without empty parts: "38m", "2h", "1h 5m" (Turkish "38dk", "2sa", "1sa 5dk").
 * Used everywhere a total or session length is shown, instead of the old always-"0h 38m" form.
 */
@Composable
fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes_only, minutes)
        minutes == 0 -> stringResource(R.string.duration_hours_only, hours)
        else -> stringResource(R.string.duration_format, hours, minutes)
    }
}
