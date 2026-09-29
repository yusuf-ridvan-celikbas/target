package com.ridvan.target.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.focustimer.FocusHistoryRow
import com.ridvan.target.ui.focustimer.FocusSessionDeleteDialog
import java.util.Locale

/** One book: its progress, what reading it has added up to, and its sessions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: BookDetailViewModel = viewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var pendingSessionDelete by remember { mutableStateOf<FocusSessionWithLinks?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(progress?.book?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = progress != null) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.reading_edit_book))
                    }
                    IconButton(onClick = { showDelete = true }, enabled = progress != null) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.reading_delete_book))
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = progress ?: return@Scaffold
        val book = current.book
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    book.author?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(bookProgressLabel(current), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                    current.percent?.let { percent ->
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            stringResource(if (book.isFinished) R.string.reading_finished else R.string.reading_in_progress),
                            color = if (book.isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.setFinished(book, !book.isFinished) }) {
                            Text(stringResource(if (book.isFinished) R.string.reading_mark_unfinished else R.string.reading_mark_finished))
                        }
                    }
                }
            }

            GroupedCard(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val speed = pagesPerHour(current.pagesRead, current.minutes)
                    Text(
                        stringResource(
                            R.string.reading_book_stats,
                            current.pagesRead,
                            stringResource(R.string.duration_format, current.minutes / 60, current.minutes % 60),
                            current.sessionCount,
                        ),
                    )
                    speed?.let {
                        Text(
                            stringResource(R.string.reading_speed_value, String.format(Locale.getDefault(), "%.1f", it)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.reading_stat_sessions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            if (sessions.isEmpty()) {
                Text(stringResource(R.string.reading_no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                GroupedCard {
                    sessions.forEachIndexed { index, item ->
                        FocusHistoryRow(
                            item = item,
                            onClick = { onSessionClick(item.session.id) },
                            onDeleteClick = { pendingSessionDelete = item },
                        )
                        if (index < sessions.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }

    val current = progress
    if (showEdit && current != null) {
        BookDialog(
            initial = current.book,
            onConfirm = { title, author, totalPages, finished ->
                viewModel.updateBook(current.book, title, author, totalPages, finished)
                showEdit = false
            },
            onDismiss = { showEdit = false },
        )
    }
    if (showDelete && current != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.reading_delete_book_title)) },
            text = { Text(stringResource(R.string.reading_delete_book_message, current.book.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteBook(current.book, onDeleted = onBack)
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    pendingSessionDelete?.let { item ->
        FocusSessionDeleteDialog(
            item = item,
            onConfirm = {
                viewModel.deleteSession(item.session)
                pendingSessionDelete = null
            },
            onDismiss = { pendingSessionDelete = null },
        )
    }
}
