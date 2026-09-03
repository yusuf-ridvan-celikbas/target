package com.ridvan.target.ui.sectiondetail

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.data.local.dao.SectionCourseWithCourse
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    onBack: () -> Unit,
    viewModel: SectionDetailViewModel = viewModel(),
) {
    val section by viewModel.section.collectAsStateWithLifecycle()
    val assignedCourses by viewModel.assignedCourses.collectAsStateWithLifecycle()
    val availableCourses by viewModel.availableCourses.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddCoursesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit section")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete section")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCoursesDialog = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                "Date: ${section?.date?.let { formatDate(it) } ?: "Not set"}",
                modifier = Modifier.padding(16.dp),
            )
            if (assignedCourses.isEmpty()) {
                Text("No courses assigned yet.", modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(assignedCourses, key = { it.sectionCourse.id }) { course ->
                        AssignedCourseRow(course, onRemove = { viewModel.removeCourse(course.sectionCourse) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showEditDialog && section != null) {
        EditSectionDialog(
            initialName = section!!.name,
            initialDate = section!!.date,
            onConfirm = { name, date ->
                viewModel.updateSection(name, date)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete section?") },
            text = { Text("This removes the section and its course assignments.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSection()
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showAddCoursesDialog) {
        PickCoursesDialog(
            available = availableCourses,
            onConfirm = { courseIds ->
                viewModel.assignCourses(courseIds)
                showAddCoursesDialog = false
            },
            onDismiss = { showAddCoursesDialog = false },
        )
    }
}

@Composable
private fun AssignedCourseRow(course: SectionCourseWithCourse, onRemove: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.courseIcon) },
        headlineContent = { Text(course.courseName) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove course")
            }
        },
    )
}

@Composable
private fun PickCoursesDialog(
    available: List<ExamCourseWithCourse>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add courses") },
        text = {
            if (available.isEmpty()) {
                Text("All of the exam's courses are already assigned to this section.")
            } else {
                Column {
                    available.forEach { item ->
                        val courseId = item.examCourse.courseId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (courseId in selected) selected - courseId else selected + courseId
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = courseId in selected, onCheckedChange = null)
                            CourseIconAvatar(item.courseIcon, modifier = Modifier.padding(end = 8.dp))
                            Text(item.courseName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSectionDialog(
    initialName: String,
    initialDate: Long?,
    onConfirm: (name: String, date: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var date by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit section") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section name") },
                    singleLine = true,
                )
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showDatePicker = true }) {
                    Text("Date")
                    Text(date?.let { formatDate(it) } ?: "Tap to set")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, date) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
