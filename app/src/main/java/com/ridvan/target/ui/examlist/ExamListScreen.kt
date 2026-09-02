package com.ridvan.target.ui.examlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.ridvan.target.ui.common.AddOrEditExamDialog
import com.ridvan.target.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(onBack: () -> Unit, onExamClick: (Long) -> Unit, viewModel: ExamListViewModel = viewModel()) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exams") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
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
            onConfirm = { name, examTypeId, hasSections, examDate, studyStartDate ->
                viewModel.addExam(name, examTypeId, hasSections, examDate, studyStartDate)
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
            val secondary = if (item.exam.hasSections) {
                "${item.examTypeName} · Sectioned"
            } else {
                val dateText = item.exam.examDate?.let { formatDate(it) } ?: "No date set"
                "${item.examTypeName} · $dateText"
            }
            Text(secondary)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
