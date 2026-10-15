package com.ridvan.target.ui.routines

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineCategory
import com.ridvan.target.ui.trips.minuteLabel
import java.text.DateFormatSymbols
import java.util.Locale

fun routineCategoryIcon(category: RoutineCategory): ImageVector = when (category) {
    RoutineCategory.MORNING -> Icons.Filled.WbSunny
    RoutineCategory.EVENING -> Icons.Filled.Bedtime
    RoutineCategory.WORKOUT -> Icons.Filled.FitnessCenter
    RoutineCategory.CUSTOM -> Icons.Filled.Checklist
}

fun routineCategoryLabelRes(category: RoutineCategory): Int = when (category) {
    RoutineCategory.MORNING -> R.string.routine_category_morning
    RoutineCategory.EVENING -> R.string.routine_category_evening
    RoutineCategory.WORKOUT -> R.string.routine_category_workout
    RoutineCategory.CUSTOM -> R.string.routine_category_custom
}

@Composable
fun RoutineIcon(category: RoutineCategory, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(routineCategoryIcon(category), contentDescription = null, tint = tint, modifier = modifier)
}

/** "Mon" for ISO weekday 1 .. "Sun" for 7, in the current locale. */
fun shortWeekdayName(isoDay: Int): String = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays[isoDay % 7 + 1]

/** "Every day" or "Mon, Wed, Fri". */
@Composable
fun repeatDaysLabel(routine: Routine): String {
    val days = parseRepeatDays(routine.repeatDays)
    return if (days.size == 7) stringResource(R.string.routine_every_day) else days.sorted().joinToString(", ") { shortWeekdayName(it) }
}

/** "07:00 · Mon, Wed" style line for lists and the detail header. */
@Composable
fun routineScheduleLine(routine: Routine): String =
    listOfNotNull(routine.startMinuteOfDay?.let { minuteLabel(it) }, repeatDaysLabel(routine)).joinToString(" · ")

@Composable
fun routineProgressText(progress: RoutineProgress): String =
    if (progress.total == 0) stringResource(R.string.routine_no_steps)
    else stringResource(R.string.routine_progress, progress.done, progress.total)
