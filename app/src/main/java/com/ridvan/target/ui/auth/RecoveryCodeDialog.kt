package com.ridvan.target.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridvan.target.R
import com.ridvan.target.ui.common.FormDialogProperties

/** Shows a freshly generated recovery code once; cannot be dismissed except via the button. */
@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit, extraMessage: String? = null) {
    val clipboard = LocalClipboardManager.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (confirming) {
        AlertDialog(
            onDismissRequest = {},
            properties = FormDialogProperties,
            title = { Text(stringResource(R.string.recovery_confirm_title)) },
            text = { Text(stringResource(R.string.recovery_confirm_message)) },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.recovery_confirm_back))
                }
            },
            confirmButton = {
                TextButton(onClick = onDone) { Text(stringResource(R.string.recovery_confirm_yes)) }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = {},
        properties = FormDialogProperties,
        title = { Text(stringResource(R.string.recovery_save_title)) },
        text = {
            Column {
                extraMessage?.let { Text(it, modifier = Modifier.padding(bottom = 8.dp)) }
                SelectionContainer {
                    Text(
                        code,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    stringResource(R.string.recovery_save_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text(stringResource(R.string.recovery_copy))
            }
        },
        confirmButton = {
            TextButton(onClick = { confirming = true }) { Text(stringResource(R.string.recovery_saved)) }
        },
    )
}
