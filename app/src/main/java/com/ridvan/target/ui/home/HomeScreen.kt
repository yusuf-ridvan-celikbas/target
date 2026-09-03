package com.ridvan.target.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun HomeScreen(
    shellNavigation: ShellNavigation,
    viewModel: HomeViewModel = viewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    AppShell(navigation = shellNavigation) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                buildAnnotatedString {
                    append("Welcome to Target, ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentUser?.preferredName ?: "")
                    }
                    append("!")
                },
            )
        }
    }
}
