package com.ridvan.target.ui.statistics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun StatisticsScreen(
    shellNavigation: ShellNavigation,
    onTopicClick: (Long) -> Unit,
    viewModel: StatisticsViewModel = viewModel(),
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val courses by viewModel.coursesForFilter.collectAsStateWithLifecycle()
    val topics by viewModel.topicsForFilter.collectAsStateWithLifecycle()
    val selectedExamId by viewModel.selectedExamId.collectAsStateWithLifecycle()
    val selectedCourseId by viewModel.selectedCourseId.collectAsStateWithLifecycle()
    val selectedTopicId by viewModel.selectedTopicId.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val chartBuckets by viewModel.chartBuckets.collectAsStateWithLifecycle()
    val breakdown by viewModel.breakdown.collectAsStateWithLifecycle()

    AppShell(navigation = shellNavigation, title = stringResource(R.string.statistics_title)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ExamFilterField(exams, selectedExamId, onSelect = viewModel::setExam)
            CourseFilterField(courses, selectedCourseId, onSelect = viewModel::setCourse)
            TopicFilterField(topics, selectedTopicId, enabled = selectedCourseId != null, onSelect = viewModel::setTopic)

            PeriodToggle(period = period, onSelect = viewModel::setPeriod, modifier = Modifier.padding(top = 16.dp))

            if (summary.tests == 0 && summary.totalQuestions == 0) {
                Text(stringResource(R.string.statistics_empty), modifier = Modifier.padding(top = 16.dp))
            } else {
                val durationText = stringResource(R.string.duration_format, summary.durationMinutes / 60, summary.durationMinutes % 60)
                Text(
                    stringResource(
                        R.string.progress_aggregate,
                        summary.tests,
                        summary.solved,
                        summary.unsolved,
                        "%.2f".format(summary.net),
                        durationText,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.stat_accuracy_label, summary.accuracyPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(
                        R.string.stat_pace_label,
                        "%.1f".format(summary.minutesPerQuestion),
                        "%.1f".format(summary.minutesPerTest),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )

                TrendBarChart(buckets = chartBuckets, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

                if (breakdown.isNotEmpty()) {
                    Text(
                        stringResource(R.string.label_breakdown),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        breakdown.forEach { entry ->
                            BreakdownRow(entry, onClick = { onTopicClick(entry.topicId) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(entry: TopicBreakdownEntry, onClick: () -> Unit) {
    val durationText = stringResource(R.string.duration_format, entry.durationMinutes / 60, entry.durationMinutes % 60)
    ListItem(
        headlineContent = { Text(stringResource(R.string.topic_with_course_title, entry.topicName, courseDisplayName(entry.courseName))) },
        supportingContent = {
            Text(stringResource(R.string.session_row_summary, entry.tests, entry.solved, entry.unsolved, durationText))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PeriodToggle(period: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        PeriodSegment(stringResource(R.string.period_daily), period == StatsPeriod.DAILY, { onSelect(StatsPeriod.DAILY) }, Modifier.weight(1f))
        PeriodSegment(stringResource(R.string.period_weekly), period == StatsPeriod.WEEKLY, { onSelect(StatsPeriod.WEEKLY) }, Modifier.weight(1f).padding(start = 4.dp))
        PeriodSegment(stringResource(R.string.period_monthly), period == StatsPeriod.MONTHLY, { onSelect(StatsPeriod.MONTHLY) }, Modifier.weight(1f).padding(start = 4.dp))
        PeriodSegment(stringResource(R.string.period_all_time), period == StatsPeriod.ALL_TIME, { onSelect(StatsPeriod.ALL_TIME) }, Modifier.weight(1f).padding(start = 4.dp))
    }
}

@Composable
private fun PeriodSegment(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "periodToggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "periodToggleContentColor",
    )
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 2.dp),
        )
    }
}

@Composable
private fun ExamFilterField(exams: List<Exam>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.filter_all_exams)
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.label_exam), style = MaterialTheme.typography.labelSmall)
            Text(exams.firstOrNull { it.id == selectedId }?.name ?: allLabel, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            exams.forEach { exam ->
                DropdownMenuItem(text = { Text(exam.name) }, onClick = { onSelect(exam.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CourseFilterField(courses: List<Course>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.filter_all_courses)
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.label_course), style = MaterialTheme.typography.labelSmall)
            Text(
                courses.firstOrNull { it.id == selectedId }?.name?.let { courseDisplayName(it) } ?: allLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            courses.forEach { course ->
                DropdownMenuItem(text = { Text(courseDisplayName(course.name)) }, onClick = { onSelect(course.id); expanded = false })
            }
        }
    }
}

@Composable
private fun TopicFilterField(topics: List<Topic>, selectedId: Long?, enabled: Boolean, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.filter_all_topics)
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true }) {
            Text(stringResource(R.string.label_topic), style = MaterialTheme.typography.labelSmall)
            Text(
                topics.firstOrNull { it.id == selectedId }?.name ?: allLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            topics.forEach { topic ->
                DropdownMenuItem(text = { Text(topic.name) }, onClick = { onSelect(topic.id); expanded = false })
            }
        }
    }
}

@Composable
private fun TrendBarChart(buckets: List<ChartBucket>, modifier: Modifier = Modifier) {
    if (buckets.isEmpty()) return
    val maxValue = buckets.maxOf { it.solved + it.unsolved }.coerceAtLeast(1)
    val solvedColor = MaterialTheme.colorScheme.primary
    val unsolvedColor = MaterialTheme.colorScheme.error
    val labelStep = (buckets.size / 6).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val barCount = buckets.size
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1).coerceAtLeast(0)) / barCount
            buckets.forEachIndexed { index, bucket ->
                val total = bucket.solved + bucket.unsolved
                val totalHeight = size.height * (total.toFloat() / maxValue)
                val solvedHeight = if (total == 0) 0f else totalHeight * (bucket.solved.toFloat() / total)
                val unsolvedHeight = totalHeight - solvedHeight
                val x = index * (barWidth + gap)
                if (unsolvedHeight > 0f) {
                    drawRect(
                        color = unsolvedColor,
                        topLeft = Offset(x, size.height - totalHeight),
                        size = Size(barWidth, unsolvedHeight),
                    )
                }
                if (solvedHeight > 0f) {
                    drawRect(
                        color = solvedColor,
                        topLeft = Offset(x, size.height - solvedHeight),
                        size = Size(barWidth, solvedHeight),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    if (index % labelStep == 0) bucket.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
