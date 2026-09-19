package com.ridvan.target.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlannerMonthGrid(
    anchorDate: LocalDate,
    items: List<PlannerAgendaItem>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthStart = anchorDate.withDayOfMonth(1)
    val gridStart = mondayOf(monthStart)
    val datesWithItems = items.map { it.date }.toSet()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.sortedBy { it.value }.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOffset in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + dayOffset).toLong())
                    MonthGridCell(
                        date = date,
                        inCurrentMonth = date.month == anchorDate.month && date.year == anchorDate.year,
                        hasItems = date in datesWithItems,
                        isSelected = date == selectedDate,
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGridCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    hasItems: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                    !inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> LocalContentColor.current
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasItems) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}
