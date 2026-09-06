package com.ridvan.target.ui.studyresourcedetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.ui.studyresource.StudyResourceFormDialog
import com.ridvan.target.ui.studyresource.studyResourceTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyResourceDetailScreen(
    onBack: () -> Unit,
    viewModel: StudyResourceDetailViewModel = viewModel(),
) {
    val studyResource by viewModel.studyResource.collectAsStateWithLifecycle()
    val subjectName by viewModel.subjectName.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studyResource?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit study resource")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete study resource")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            Text("Subject: $subjectName")
            Text("Type: ${studyResourceTypeLabel(studyResource?.type)}")
            studyResource?.publisher?.let { publisher ->
                Text("Publisher: $publisher")
            }
        }
    }

    if (showEditDialog && studyResource != null) {
        StudyResourceFormDialog(
            title = "Edit study resource",
            subjectLabel = subjectName,
            initialName = studyResource!!.name,
            initialType = studyResource!!.type,
            initialPublisher = studyResource!!.publisher.orEmpty(),
            onConfirm = { name, type, publisher ->
                viewModel.updateStudyResource(name, type, publisher)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete study resource?") },
            text = { Text("This removes \"${studyResource?.name}\".") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudyResource()
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
