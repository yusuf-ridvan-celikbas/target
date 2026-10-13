package com.ridvan.target.ui.focustimer

import com.ridvan.target.ui.reading.isReading
import com.ridvan.target.ui.reading.pagesRead
import com.ridvan.target.data.local.entity.FocusSession
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    /** When set, the row can be swiped away (end to start) to delete the session. */
    onDelete: ((FocusSession) -> Unit)? = null,
) {
    val session = item.session
    if (onDelete == null) {
        FocusHistoryRowContent(item, onClick)
        return
    }
    key(session.id) {
        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete(session)
                    true
                } else {
                    false
                }
            },
        )
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            },
        ) {
            FocusHistoryRowContent(item, onClick, Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
        }
    }
}

/** Returns a delete handler that removes the session and offers an Undo snackbar that restores the same row. */
@Composable
internal fun rememberUndoDelete(
    snackbarHostState: SnackbarHostState,
    delete: (FocusSession) -> Unit,
    restore: (FocusSession) -> Unit,
): (FocusSession) -> Unit {
    val scope = rememberCoroutineScope()
    val message = stringResource(R.string.focus_session_deleted)
    val undo = stringResource(R.string.common_undo)
    return { session ->
        delete(session)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) restore(session)
        }
    }
}

@Composable
private fun FocusHistoryRowContent(item: FocusSessionWithLinks, onClick: () -> Unit, extra: Modifier = Modifier) {
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
        modifier = extra.clickable(onClick = onClick),
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
