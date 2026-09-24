package com.ridvan.target.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))

/** 24-hour "HH:mm", matching Home's clock and the Planner's time ranges. */
fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

private fun startOfDay(epochMillis: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

fun startOfTodayMillis(): Long = startOfDay(System.currentTimeMillis()).timeInMillis

/** Whole calendar days between today and [epochMillis] — 0 for today, negative if already passed. */
fun daysUntil(epochMillis: Long): Long {
    val target = startOfDay(epochMillis).timeInMillis
    val today = startOfTodayMillis()
    return (target - today) / (24L * 60 * 60 * 1000)
}
