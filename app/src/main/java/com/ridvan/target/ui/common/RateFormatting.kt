package com.ridvan.target.ui.common

import java.util.Locale
import kotlin.math.round

/** A rate to one decimal, without a trailing ".0" for whole numbers: "30", "18.3" (locale decimal mark). */
fun formatRate(value: Double): String {
    val tenths = round(value * 10).toLong()
    return if (tenths % 10 == 0L) (tenths / 10).toString() else String.format(Locale.getDefault(), "%.1f", tenths / 10.0)
}
