package com.ridvan.target.ui.focustimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.formatDate

/** "Chemistry · Mol Kavramı" / "German · Verbs" / null when the session wasn't linked to anything. */
@Composable
internal fun focusSessionLinkLabel(item: FocusSessionWithLinks): String? {
    val owner = item.courseName?.let { courseDisplayName(it) } ?: item.languageName
    return listOfNotNull(owner, item.topicName).joinToString(" · ").ifEmpty { null }
}

/** Shared by FocusTimerScreen's recent-history card and every FocusHistoryScreen variant; tapping opens FocusSessionDetailScreen. */
@Composable
internal fun FocusHistoryRow(
    item: FocusSessionWithLinks,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val session = item.session
    val workText = stringResource(R.string.duration_format, session.totalWorkMinutes / 60, session.totalWorkMinutes % 60)
    val breakText = stringResource(R.string.duration_format, session.totalBreakMinutes / 60, session.totalBreakMinutes % 60)
    val linkLabel = focusSessionLinkLabel(item)
    ListItem(
        headlineContent = { Text("${session.presetName} — ${formatDate(session.startedAt)}") },
        supportingContent = {
            Column {
                if (linkLabel != null) {
                    Text(linkLabel, color = MaterialTheme.colorScheme.primary)
                }
                Text(stringResource(R.string.focustimer_history_row_subtitle, session.cyclesCompleted, workText, breakText))
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
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_focus_session))
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
        text = { Text(stringResource(R.string.focustimer_delete_session_message, item.session.presetName, formatDate(item.session.startedAt))) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
