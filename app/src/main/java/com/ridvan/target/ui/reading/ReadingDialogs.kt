package com.ridvan.target.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.Book

private const val MAX_PAGE_DIGITS = 5

private fun digitsOnly(value: String): String = value.filter { it.isDigit() }.take(MAX_PAGE_DIGITS)

/** Add (initial == null) or edit a book. Edit also offers "Finished" and Delete. */
@Composable
fun BookDialog(
    initial: Book?,
    onConfirm: (title: String, author: String?, totalPages: Int?, isFinished: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var author by remember { mutableStateOf(initial?.author.orEmpty()) }
    var totalText by remember { mutableStateOf(initial?.totalPages?.toString().orEmpty()) }
    var finished by remember { mutableStateOf(initial?.isFinished ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.reading_add_book else R.string.reading_edit_book)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reading_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.reading_field_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = digitsOnly(it) },
                    label = { Text(stringResource(R.string.reading_field_total_pages)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (initial != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = finished, onCheckedChange = { finished = it })
                        Text(stringResource(R.string.reading_finished))
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            stringResource(R.string.reading_delete_book),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(
                        title.trim(),
                        author.trim().ifEmpty { null },
                        totalText.toIntOrNull()?.takeIf { it > 0 },
                        finished,
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/**
 * "What page are you on?" — after a reading session, or to fix a session's pages later. With
 * [startEditable] the start page can be changed too (detail screen); otherwise it's shown as a hint.
 * The page reached is capped at [totalPages] and can't go below the start page.
 */
@Composable
fun ReadingPagesDialog(
    bookTitle: String?,
    startPage: Int,
    endPage: Int?,
    totalPages: Int?,
    startEditable: Boolean,
    onConfirm: (startPage: Int, endPage: Int) -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.common_cancel),
) {
    var startText by remember { mutableStateOf(startPage.toString()) }
    var endText by remember { mutableStateOf((endPage ?: startPage).toString()) }
    val start = startText.toIntOrNull() ?: 0
    val end = endText.toIntOrNull()
    val cap = totalPages ?: Int.MAX_VALUE
    val valid = end != null && end >= start && end <= cap && start <= cap

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (startEditable) R.string.reading_edit_pages_title else R.string.reading_page_reached_title))
        },
        text = {
            Column {
                bookTitle?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                if (startEditable) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = digitsOnly(it) },
                        label = { Text(stringResource(R.string.reading_field_start_page)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                } else {
                    Text(
                        stringResource(R.string.reading_started_at_page, startPage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = endText,
                    onValueChange = { value ->
                        val digits = digitsOnly(value)
                        // Never past the book's last page.
                        endText = digits.toIntOrNull()?.let { if (it > cap) cap.toString() else digits } ?: digits
                    },
                    label = { Text(stringResource(R.string.reading_field_page_reached)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    if (end != null && end >= start) {
                        stringResource(R.string.reading_pages_read_live, end - start)
                    } else {
                        stringResource(R.string.reading_page_before_start)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (end != null && end >= start) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
                totalPages?.let {
                    Text(
                        stringResource(R.string.reading_book_last_page, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(start, end ?: start) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/** "Page 120 of 300 · 40%" / "Page 120" — shared by book pickers and lists. */
@Composable
fun bookProgressLabel(progress: BookProgress): String {
    val total = progress.book.totalPages
    val percent = progress.percent
    return if (total != null && percent != null) {
        stringResource(R.string.reading_book_progress, progress.currentPage, total, percent)
    } else {
        stringResource(R.string.reading_book_progress_no_total, progress.currentPage)
    }
}
