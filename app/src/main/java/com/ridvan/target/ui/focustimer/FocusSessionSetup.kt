package com.ridvan.target.ui.focustimer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.ui.common.DropdownField
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.Topic
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.reading.BookCover
import com.ridvan.target.ui.reading.BookProgress
import com.ridvan.target.ui.reading.bookProgressLabel

// Setup pieces shared by Focus Timer and Flow Timer.

/** The optional None/Course/Language (+ Topic) link picked before a study session starts. */
@Composable
internal fun SessionLinkCard(
    linkMode: SessionLinkMode,
    onSelectLinkMode: (SessionLinkMode) -> Unit,
    courses: List<Course>,
    selectedCourseId: Long?,
    onSelectCourse: (Long?) -> Unit,
    languages: List<Language>,
    selectedLanguageId: Long?,
    onSelectLanguage: (Long?) -> Unit,
    topics: List<Topic>,
    selectedTopicId: Long?,
    onSelectTopic: (Long?) -> Unit,
) {
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.focustimer_link_section_title), style = MaterialTheme.typography.titleMedium)
            SegmentedToggle(
                options = listOf(
                    SegmentedToggleOption(SessionLinkMode.NONE, stringResource(R.string.common_none)),
                    SegmentedToggleOption(SessionLinkMode.COURSE, stringResource(R.string.label_course)),
                    SegmentedToggleOption(SessionLinkMode.LANGUAGE, stringResource(R.string.label_language)),
                ),
                selected = linkMode,
                onSelect = onSelectLinkMode,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            if (linkMode == SessionLinkMode.COURSE) {
                Text(stringResource(R.string.label_course), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                DropdownField(
                    selectedLabel = courses.firstOrNull { it.id == selectedCourseId }?.name ?: stringResource(R.string.common_select),
                ) { closeMenu ->
                    courses.forEach { course ->
                        DropdownMenuItem(text = { Text(course.name) }, onClick = { onSelectCourse(course.id); closeMenu() })
                    }
                }
            } else if (linkMode == SessionLinkMode.LANGUAGE) {
                Text(stringResource(R.string.label_language), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                DropdownField(
                    selectedLabel = languages.firstOrNull { it.id == selectedLanguageId }?.name ?: stringResource(R.string.common_select),
                ) { closeMenu ->
                    languages.forEach { language ->
                        DropdownMenuItem(text = { Text(language.name) }, onClick = { onSelectLanguage(language.id); closeMenu() })
                    }
                }
            }

            if (linkMode != SessionLinkMode.NONE && (selectedCourseId != null || selectedLanguageId != null)) {
                Text(stringResource(R.string.label_link_to_topic), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                DropdownField(
                    selectedLabel = topics.firstOrNull { it.id == selectedTopicId }?.name ?: stringResource(R.string.common_none),
                ) { closeMenu ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.common_none)) }, onClick = { onSelectTopic(null); closeMenu() })
                    topics.forEach { topic ->
                        DropdownMenuItem(text = { Text(topic.name) }, onClick = { onSelectTopic(topic.id); closeMenu() })
                    }
                }
            }
        }
    }
}

/**
 * A rounded-square outline around the countdown that is eaten away clockwise from the top
 * centre as the round runs down ([progress] = fraction of the round still remaining).
 * Updates arrive once a second, so the fraction is animated linearly to look continuous.
 */
@Composable
internal fun CountdownBorder(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "countdownBorder",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .drawBehind {
                val strokeWidth = 8.dp.toPx()
                val inset = strokeWidth / 2
                val left = inset
                val top = inset
                val right = size.width - inset
                val bottom = size.height - inset
                val radius = 28.dp.toPx().coerceAtMost((bottom - top) / 2)
                // Built by hand (not addRoundRect) so the path starts at top centre and runs clockwise.
                val outline = Path().apply {
                    moveTo(size.width / 2, top)
                    lineTo(right - radius, top)
                    arcTo(Rect(right - 2 * radius, top, right, top + 2 * radius), -90f, 90f, false)
                    lineTo(right, bottom - radius)
                    arcTo(Rect(right - 2 * radius, bottom - 2 * radius, right, bottom), 0f, 90f, false)
                    lineTo(left + radius, bottom)
                    arcTo(Rect(left, bottom - 2 * radius, left + 2 * radius, bottom), 90f, 90f, false)
                    lineTo(left, top + radius)
                    arcTo(Rect(left, top, left + 2 * radius, top + 2 * radius), 180f, 90f, false)
                    lineTo(size.width / 2, top)
                }
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                drawPath(outline, trackColor, style = stroke)
                if (animatedProgress > 0f) {
                    val measure = PathMeasure().apply { setPath(outline, false) }
                    val length = measure.length
                    val remaining = Path()
                    // The elapsed part [0, elapsed) is gone; what's left runs to the end of the path.
                    measure.getSegment((1f - animatedProgress) * length, length, remaining, true)
                    drawPath(remaining, color, style = stroke)
                }
            }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        content = content,
    )
}

/** Replaces the Link-to card for a reading preset: which book, and where it's up to. */
@Composable
internal fun BookCard(
    books: List<BookProgress>,
    selectedBookId: Long?,
    onSelectBook: (Long) -> Unit,
    onAddBook: () -> Unit,
) {
    // Finished books drop out of the picker (unless already selected); they stay on the Reading page.
    val readable = books.filter { !it.book.isFinished || it.book.id == selectedBookId }
    val selected = readable.firstOrNull { it.book.id == selectedBookId }
    GroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reading_book), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAddBook) { Text(stringResource(R.string.reading_add_book)) }
            }
            if (readable.isEmpty()) {
                Text(stringResource(R.string.reading_no_books), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                DropdownField(selectedLabel = selected?.book?.title ?: stringResource(R.string.common_select)) { closeMenu ->
                    readable.forEach { progress ->
                        DropdownMenuItem(
                            leadingIcon = {
                                BookCover(progress.book, compact = true, elevation = 1.dp, modifier = Modifier.width(26.dp))
                            },
                            text = {
                                Column {
                                    Text(progress.book.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        bookProgressLabel(progress),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = { onSelectBook(progress.book.id); closeMenu() },
                        )
                    }
                }
                Text(
                    if (selected != null) bookProgressLabel(selected) else stringResource(R.string.reading_select_book_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
