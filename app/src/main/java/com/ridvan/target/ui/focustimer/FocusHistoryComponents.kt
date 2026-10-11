package com.ridvan.target.ui.focustimer

import com.ridvan.target.ui.reading.isReading
import com.ridvan.target.ui.reading.pagesRead
import com.ridvan.target.data.local.entity.FocusSession
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.ridvan.target.R
import com.ridvan.target.ui.common.countText
import com.ridvan.target.ui.common.formatDuration
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.formatTime

/** "Chemistry · Mol Kavramı" / "German · Verbs" / null when the session wasn't linked to anything. */
@Composable
internal fun focusSessionLinkLabel(item: FocusSessionWithLinks): String? {
    val owner = item.courseName?.let { courseDisplayName(it) } ?: item.languageName
    return listOfNotNull(owner, item.topicName).joinToString(" · ").ifEmpty { null }
}

/** The preset a session ran on, or "Manual" for one logged by hand. */
@Composable
internal fun focusSessionPresetLabel(session: FocusSession): String = when {
    session.isManual -> stringResource(R.string.focus_manual_label)
    session.isFlow -> stringResource(R.string.flow_label)
    else -> session.presetName
}

/** What a session is about: its link, else its preset (or "Manual"). */
@Composable
internal fun focusSessionTitle(item: FocusSessionWithLinks): String =
    item.bookTitle ?: focusSessionLinkLabel(item) ?: focusSessionPresetLabel(item.session)

/**
 * "2 cycles · 40m work · 7m break · 12 pages" — only the parts that say something: no cycle count for
 * manual/Flow sessions or a timer session that never finished a round, no break part when there was no
 * break, and pages only once the page reached is known. A reading session says "reading" instead of "work".
 */
@Composable
internal fun focusSessionStatsLine(session: FocusSession): String {
    val parts = mutableListOf<String>()
    if (!session.isManual && !session.isFlow && session.cyclesCompleted > 0) {
        parts += countText(R.plurals.count_cycles, session.cyclesCompleted)
    }
    val workText = formatDuration(session.totalWorkMinutes)
    parts += stringResource(if (session.isReading) R.string.focus_stats_reading else R.string.focus_stats_work, workText)
    if (session.totalBreakMinutes > 0) {
        parts += stringResource(R.string.focus_stats_break, formatDuration(session.totalBreakMinutes))
    }
    if (session.endPage != null) parts += countText(R.plurals.count_pages, session.pagesRead())
    return parts.joinToString(" · ")
}

/** Shared by FocusTimerScreen's recent-history card and every FocusHistoryScreen variant; tapping opens FocusSessionDetailScreen. */
@Composable
internal fun FocusHistoryRow(
    item: FocusSessionWithLinks,
    onClick: () -> Unit,
) {
    val session = item.session
    val linkLabel = focusSessionLinkLabel(item)
    ListItem(
        // Date and start time lead, matching FocusSessionDetailScreen's title; the preset moves into the stats line.
        headlineContent = {
            Text(stringResource(R.string.focus_session_title, formatDate(session.startedAt), formatTime(session.startedAt)))
        },
        supportingContent = {
            Column {
                val subject = item.bookTitle ?: linkLabel
                if (subject != null) {
                    Text(subject, color = MaterialTheme.colorScheme.primary)
                }
                Text("${focusSessionPresetLabel(session)} · ${focusSessionStatsLine(session)}")
                session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
internal fun FocusSessionDeleteDialog(
    item: FocusSessionWithLinks,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focustimer_delete_session_title)) },
        text = {
            val whenLabel = stringResource(R.string.focus_session_title, formatDate(item.session.startedAt), formatTime(item.session.startedAt))
            Text(stringResource(R.string.focustimer_delete_session_message, focusSessionPresetLabel(item.session), whenLabel))
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
