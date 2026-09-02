package com.ridvan.target.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
