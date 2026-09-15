package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R

/** "Today" / "Tomorrow" / "X days left" / "Passed", localized. */
@Composable
fun daysUntilLabel(epochMillis: Long): String {
    val days = daysUntil(epochMillis)
    return when {
        days < 0 -> stringResource(R.string.days_until_passed)
        days == 0L -> stringResource(R.string.days_until_today)
        days == 1L -> stringResource(R.string.days_until_tomorrow)
        else -> stringResource(R.string.days_until_n, days)
    }
}
