package com.ridvan.target.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.common.GroupedCard
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Plain Column, not LazyColumn — this screen is already inside PlannerHomeScreen's own
 * verticalScroll, and a fixed 12-month list doesn't need lazy layout.
 */
@Composable
fun PlannerYearList(
    year: Int,
    items: List<PlannerAgendaItem>,
    onItemClick: (PlannerAgendaItem) -> Unit,
) {
    val byMonth = items.groupBy { YearMonth.from(it.date) }
    // Only the current calendar month starts expanded — unlike the drawer's "Study" group
    // (which defaults open because it holds fixed, cheap nav shortcuts), these 12 sections hold
    // variable real content: expanding all by default is an unscannable wall of text, collapsing
    // all hides the one section most people open Yearly to see first.
    var expandedMonths by remember(year) { mutableStateOf(setOf(YearMonth.now())) }

    Column(modifier = Modifier.fillMaxWidth()) {
        for (monthNumber in 1..12) {
            val month = YearMonth.of(year, monthNumber)
            val monthItems = byMonth[month].orEmpty().sortedBy { it.date }
            val expanded = month in expandedMonths
            GroupedCard(modifier = Modifier.padding(top = 8.dp)) {
                YearMonthHeader(
                    month = month,
                    count = monthItems.size,
                    expanded = expanded,
                    onToggle = {
                        expandedMonths = if (expanded) expandedMonths - month else expandedMonths + month
                    },
                )
                if (expanded) {
                    if (monthItems.isEmpty()) {
                        Text(
                            stringResource(R.string.planner_day_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    } else {
                        monthItems.forEach { item -> YearItemRow(item, onItemClick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearMonthHeader(month: YearMonth, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(monthYearLabel(month), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun YearItemRow(item: PlannerAgendaItem, onItemClick: (PlannerAgendaItem) -> Unit) {
    val label = when (item) {
        is PlannerAgendaItem.EventOccurrence -> item.event.title
        is PlannerAgendaItem.ExamEntry -> item.label
        is PlannerAgendaItem.SectionEntry -> item.label
    }
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onItemClick(item) },
        headlineContent = { Text(label) },
        supportingContent = { Text(shortDateLabel(item.date)) },
    )
}

private fun monthYearLabel(month: YearMonth): String =
    month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

private fun shortDateLabel(date: java.time.LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
