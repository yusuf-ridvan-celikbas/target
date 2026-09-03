package com.ridvan.target.ui.examlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.ui.common.AddOrEditExamDialog
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun ExamListScreen(
    shellNavigation: ShellNavigation,
    onExamClick: (Long) -> Unit,
    viewModel: ExamListViewModel = viewModel(),
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    AppShell(
        navigation = shellNavigation,
        title = "Exams",
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        if (exams.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No exams yet. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(exams, key = { it.exam.id }) { examWithType ->
                    ExamRow(examWithType, onClick = { onExamClick(examWithType.exam.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddOrEditExamDialog(
            examTypes = examTypes,
            languages = languages,
            onConfirm = { name, examTypeId, hasSections, examDate, studyStartDate, languageId ->
                viewModel.addExam(name, examTypeId, hasSections, examDate, studyStartDate, languageId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun ExamRow(item: ExamWithType, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.exam.name) },
        supportingContent = {
            val typeText = if (item.examTypeName == LANGUAGE_EXAM_TYPE_NAME) {
                "${item.examTypeName} · ${item.languageName ?: "No language set"}"
            } else {
                item.examTypeName
            }
            val secondary = if (item.exam.hasSections) {
                "$typeText · Sectioned"
            } else {
                val dateText = item.exam.examDate?.let { formatDate(it) } ?: "No date set"
                "$typeText · $dateText"
            }
            Text(secondary)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
