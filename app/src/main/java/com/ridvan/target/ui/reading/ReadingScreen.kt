package com.ridvan.target.ui.reading

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.FocusSession
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class ReadingPeriod { DAYS_7, DAYS_30, MONTHS_12, ALL }

private data class ReadingBucket(val label: String, val pages: Int)

/** Everything read so far: stats for a chosen period, a pages chart, and the books with progress. */
@Composable
fun ReadingScreen(
    shellNavigation: ShellNavigation,
    onBookClick: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: ReadingViewModel = viewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val sessions by viewModel.readingSessions.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableStateOf(ReadingPeriod.DAYS_7) }
    var showAddBook by remember { mutableStateOf(false) }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.READING,
        title = stringResource(R.string.label_reading),
        floatingActionButton = { AddFab(onClick = { showAddBook = true }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(ReadingPeriod.DAYS_7, stringResource(R.string.focustimer_history_period_7_days)),
                    SegmentedToggleOption(ReadingPeriod.DAYS_30, stringResource(R.string.focustimer_history_period_30_days)),
                    SegmentedToggleOption(ReadingPeriod.MONTHS_12, stringResource(R.string.reading_period_12_months)),
                    SegmentedToggleOption(ReadingPeriod.ALL, stringResource(R.string.focustimer_history_period_all)),
                ),
                selected = period,
                onSelect = { period = it },
                textStyle = MaterialTheme.typography.bodySmall,
            )

            val windowStart = periodStart(period)
            val inPeriod = sessions.filter { windowStart == null || it.startedAt >= windowStart }
            SummaryCard(inPeriod, finishedBooks = books.count { it.book.isFinished })

            Spacer(Modifier.height(12.dp))
            ChartCard(period, sessions)

            Spacer(Modifier.height(16.dp))
            LibraryCard(books, onOpenLibrary = onOpenLibrary, onBookClick = onBookClick)
            // Room for the FAB over the last row.
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showAddBook) {
        BookDialog(
            initial = null,
            onConfirm = { book ->
                viewModel.addBook(book)
                showAddBook = false
            },
            onDismiss = { showAddBook = false },
        )
    }
}

@Composable
private fun SummaryCard(sessions: List<FocusSession>, finishedBooks: Int) {
    val pages = sessions.sumOf { it.pagesRead() }
    val minutes = sessions.sumOf { it.totalWorkMinutes }
    val speed = pagesPerHour(pages, minutes)
    GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(stringResource(R.string.reading_stat_pages), pages.toString(), Modifier.weight(1f))
                StatCell(
                    stringResource(R.string.reading_stat_time),
                    stringResource(R.string.duration_format, minutes / 60, minutes % 60),
                    Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                StatCell(
                    stringResource(R.string.reading_stat_speed),
                    speed?.let { stringResource(R.string.reading_speed_value, String.format(Locale.getDefault(), "%.1f", it)) } ?: "—",
                    Modifier.weight(1f),
                )
                StatCell(stringResource(R.string.reading_stat_sessions), sessions.size.toString(), Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.reading_stat_finished, finishedBooks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ChartCard(period: ReadingPeriod, sessions: List<FocusSession>) {
    val buckets = buckets(period, sessions)
    val maxPages = buckets.maxOfOrNull { it.pages }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val daily = period == ReadingPeriod.DAYS_7 || period == ReadingPeriod.DAYS_30
    GroupedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(if (daily) R.string.reading_chart_per_day else R.string.reading_chart_per_month),
                style = MaterialTheme.typography.titleSmall,
            )
            if (buckets.isEmpty()) {
                Text(
                    stringResource(R.string.reading_no_sessions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }
            Text(
                stringResource(R.string.reading_chart_max, maxPages),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
                val slot = size.width / buckets.size
                val barWidth = slot * 0.7f
                buckets.forEachIndexed { index, bucket ->
                    val left = index * slot + (slot - barWidth) / 2
                    if (bucket.pages == 0) {
                        // A thin baseline tick, so empty days still read as days.
                        drawRect(emptyColor, Offset(left, size.height - 2f), Size(barWidth, 2f))
                    } else {
                        val h = size.height * bucket.pages / maxPages
                        drawRoundRect(barColor, Offset(left, size.height - h), Size(barWidth, h), CornerRadius(4f, 4f))
                    }
                }
            }
            // Only every Nth label, so 30 daily bars don't turn into unreadable text.
            val step = (buckets.size / 6).coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                buckets.forEachIndexed { index, bucket ->
                    Text(
                        // Counted back from the newest bar, so the current day/month always has its label.
                        if ((buckets.lastIndex - index) % step == 0) bucket.label else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // Only every Nth bar is labelled, so a label may spill into its unlabelled neighbours.
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** A peek at the library: the books being read on one shelf (or the latest added), tap the header for all of them. */
@Composable
private fun LibraryCard(books: List<BookProgress>, onOpenLibrary: () -> Unit, onBookClick: (Long) -> Unit) {
    GroupedCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLibrary).padding(16.dp),
        ) {
            Icon(Icons.Filled.LocalLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.library_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Text(
                pluralStringResource(R.plurals.library_card_count, books.size, books.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (books.isEmpty()) {
            Text(
                stringResource(R.string.reading_no_books),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        } else {
            val reading = books.filter { it.book.status == BookStatus.READING }
            val preview = reading.ifEmpty { books.sortedByDescending { it.book.createdAt } }.take(BOOKS_PER_SHELF)
            BookShelf(preview, onBookClick = onBookClick, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
        }
    }
}

private fun startOfDay(cal: Calendar): Calendar = cal.apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

/** Start of the shown window: today minus 6/29 days, the 1st of the month 11 months back, or null for all time. */
private fun periodStart(period: ReadingPeriod): Long? {
    val cal = startOfDay(Calendar.getInstance())
    return when (period) {
        ReadingPeriod.DAYS_7 -> cal.apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
        ReadingPeriod.DAYS_30 -> cal.apply { add(Calendar.DAY_OF_YEAR, -29) }.timeInMillis
        ReadingPeriod.MONTHS_12 -> cal.apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -11) }.timeInMillis
        ReadingPeriod.ALL -> null
    }
}

/** Pages per day (7/30 days) or per month (12 months / all time, capped at the last 24 months). */
private fun buckets(period: ReadingPeriod, sessions: List<FocusSession>): List<ReadingBucket> {
    if (sessions.isEmpty()) return emptyList()
    return when (period) {
        ReadingPeriod.DAYS_7, ReadingPeriod.DAYS_30 -> {
            val days = if (period == ReadingPeriod.DAYS_7) 7 else 30
            val format = SimpleDateFormat(if (days == 7) "EEE" else "d", Locale.getDefault())
            (days - 1 downTo 0).map { back ->
                val start = startOfDay(Calendar.getInstance()).apply { add(Calendar.DAY_OF_YEAR, -back) }
                val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                ReadingBucket(
                    label = format.format(start.time),
                    pages = sessions.filter { it.startedAt >= start.timeInMillis && it.startedAt < end.timeInMillis }.sumOf { it.pagesRead() },
                )
            }
        }
        ReadingPeriod.MONTHS_12, ReadingPeriod.ALL -> {
            val monthStart = startOfDay(Calendar.getInstance()).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val months = if (period == ReadingPeriod.MONTHS_12) {
                12
            } else {
                val first = Calendar.getInstance().apply { timeInMillis = sessions.minOf { it.startedAt } }
                val span = (monthStart.get(Calendar.YEAR) - first.get(Calendar.YEAR)) * 12 +
                    monthStart.get(Calendar.MONTH) - first.get(Calendar.MONTH) + 1
                span.coerceIn(1, 24)
            }
            // Full year, matching the Statistics charts ("Sep 2026", not a bare "Sep").
            val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            (months - 1 downTo 0).map { back ->
                val start = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -back) }
                val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                ReadingBucket(
                    label = format.format(start.time),
                    pages = sessions.filter { it.startedAt >= start.timeInMillis && it.startedAt < end.timeInMillis }.sumOf { it.pagesRead() },
                )
            }
        }
    }
}
