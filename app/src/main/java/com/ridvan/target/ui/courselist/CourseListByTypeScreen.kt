package com.ridvan.target.ui.courselist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.CourseIconPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListByTypeScreen(
    onCourseClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseListByTypeViewModel = viewModel(),
) {
    val examTypeName by viewModel.examTypeName.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var deletingCourse by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$examTypeName Courses") },
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
                        onClick = { onCourseClick(course.id) },
                        onEdit = { editingCourse = course },
                        onDelete = { deletingCourse = course },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        CourseDialog(
            title = "Add course",
            initialName = "",
            initialIcon = null,
            onConfirm = { name, icon ->
                viewModel.addCourse(name, icon)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingCourse?.let { course ->
        CourseDialog(
            title = "Edit course",
            initialName = course.name,
            initialIcon = course.icon,
            onConfirm = { name, icon ->
                viewModel.updateCourse(course, name, icon)
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
private fun CourseRow(course: Course, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.icon) },
        headlineContent = { Text(course.name) },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit course")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete course")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CourseDialog(
    title: String,
    initialName: String,
    initialIcon: String?,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Icon",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                CourseIconPicker(
                    selectedKey = icon,
                    onSelect = { icon = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, icon) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
