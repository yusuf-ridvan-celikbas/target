package com.ridvan.target.ui.courselist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun CourseListScreen(
    shellNavigation: ShellNavigation,
    viewModel: CourseListViewModel = viewModel(),
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var deletingCourse by remember { mutableStateOf<Course?>(null) }

    AppShell(
        navigation = shellNavigation,
        title = "Courses",
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No courses yet. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(courses, key = { it.id }) { course ->
                    CourseRow(
                        course = course,
                        onEdit = { editingCourse = course },
                        onDelete = { deletingCourse = course },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = "Add course",
            initialName = "",
            onConfirm = { name ->
                viewModel.addCourse(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingCourse?.let { course ->
        NameDialog(
            title = "Rename course",
            initialName = course.name,
            onConfirm = { name ->
                viewModel.renameCourse(course, name)
                editingCourse = null
            },
            onDismiss = { editingCourse = null },
        )
    }

    deletingCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deletingCourse = null },
            title = { Text("Delete course?") },
            text = { Text("This removes \"${course.name}\" from any exams or sections it's assigned to.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(course)
                    deletingCourse = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCourse = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CourseRow(course: Course, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(course.name) },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename course")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete course")
                }
            }
        },
    )
}

@Composable
private fun NameDialog(title: String, initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
