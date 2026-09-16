package com.ridvan.target.ui.switchaccount

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.ui.common.ErrorMessage
import com.ridvan.target.ui.common.HelpTooltip
import com.ridvan.target.ui.common.text

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwitchAccountScreen(
    onSwitched: () -> Unit,
    onBack: () -> Unit,
    viewModel: SwitchAccountViewModel = viewModel(),
) {
    val otherUsers by viewModel.otherUsers.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var pendingDeleteUser by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_switch_account)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    HelpTooltip(
                        bodyRes = R.string.help_tooltip_switch_account_delete,
                        contentDescriptionRes = R.string.cd_help_switch_account_delete,
                    )
                },
            )
        },
    ) { innerPadding ->
        if (otherUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.switch_account_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(otherUsers, key = { it.id }) { user ->
                    ListItem(
                        headlineContent = { Text(user.preferredName) },
                        supportingContent = { Text(user.username) },
                        modifier = Modifier.combinedClickable(
                            onClick = { selectedUser = user },
                            onLongClick = { pendingDeleteUser = user },
                        ),
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    selectedUser?.let { user ->
        PasswordConfirmDialog(
            user = user,
            errorMessage = errorMessage,
            onConfirm = { password -> viewModel.switchTo(user, password, onSuccess = onSwitched) },
            onDismiss = {
                selectedUser = null
                viewModel.clearError()
            },
        )
    }

    pendingDeleteUser?.let { user ->
        DeleteAccountConfirmDialog(
            user = user,
            onConfirm = {
                viewModel.deleteAccount(user)
                pendingDeleteUser = null
            },
            onDismiss = { pendingDeleteUser = null },
        )
    }
}

@Composable
private fun DeleteAccountConfirmDialog(
    user: User,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.switch_account_delete_title)) },
        text = { Text(stringResource(R.string.switch_account_delete_message, user.preferredName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun PasswordConfirmDialog(
    user: User,
    errorMessage: ErrorMessage?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.switch_account_login_as, user.preferredName)) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.field_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let {
                    Text(it.text(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) { Text(stringResource(R.string.action_log_in_caps)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
