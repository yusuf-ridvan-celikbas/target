package com.ridvan.target.ui.examdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.ui.common.AddOrEditExamDialog
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    onSectionClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamDetailViewModel = viewModel(),
) {
    val exam by viewModel.exam.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val availableCoursesToAdd by viewModel.availableCoursesToAdd.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exam?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit exam")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete exam")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(innerPadding)) {
            exam?.let { currentExam ->
                item { ExamSummary(currentExam.examDate, currentExam.studyStartDate, currentExam.hasSections) }
            }

            item { SectionHeader("Courses", onAddClick = { showAddCourseDialog = true }) }
            if (courses.isEmpty()) {
                item { EmptyHint("No courses yet.") }
            } else {
                items(courses, key = { "course-${it.examCourse.id}" }) { course ->
                    CourseRow(course, onRemove = { viewModel.removeCourse(course.examCourse) })
                    HorizontalDivider()
                }
            }

            if (exam?.hasSections == true) {
                item { SectionHeader("Sections", onAddClick = { showAddSectionDialog = true }) }
                if (sections.isEmpty()) {
                    item { EmptyHint("No sections yet.") }
                } else {
                    items(sections, key = { "section-${it.id}" }) { section ->
                        SectionRow(section, onClick = { onSectionClick(section.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showEditDialog && exam != null) {
        AddOrEditExamDialog(
            examTypes = examTypes,
            initial = exam,
            onConfirm = { name, examTypeId, hasSections, examDate, studyStartDate ->
                viewModel.updateExam(name, examTypeId, hasSections, examDate, studyStartDate)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete exam?") },
            text = { Text("This removes the exam and everything filed under it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExam()
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showAddCourseDialog) {
        AddCourseDialog(
            availableCourses = availableCoursesToAdd,
            onConfirm = { name, selectedIds ->
                if (name.isNotBlank()) viewModel.addCourse(name)
                if (selectedIds.isNotEmpty()) viewModel.addExistingCourses(selectedIds)
                showAddCourseDialog = false
            },
            onDismiss = { showAddCourseDialog = false },
        )
    }

    if (showAddSectionDialog) {
        AddSectionDialog(
            onConfirm = { name, date ->
                viewModel.addSection(name, date)
                showAddSectionDialog = false
            },
            onDismiss = { showAddSectionDialog = false },
        )
    }
}

@Composable
private fun ExamSummary(examDate: Long?, studyStartDate: Long?, hasSections: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (!hasSections) {
            Text("Exam date: ${examDate?.let { formatDate(it) } ?: "Not set"}")
        }
        Text("Study start: ${studyStartDate?.let { formatDate(it) } ?: "Not set"}")
    }
}

@Composable
private fun SectionHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) { Text("+ Add") }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun CourseRow(course: ExamCourseWithCourse, onRemove: () -> Unit) {
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
private fun SectionRow(section: Section, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(section.name) },
        supportingContent = { Text(section.date?.let { formatDate(it) } ?: "No date set") },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AddCourseDialog(
    availableCourses: List<Course>,
    onConfirm: (newName: String, selectedIds: Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add course") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New course name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (availableCourses.isNotEmpty()) {
                    Text(
                        "Or pick existing courses",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        availableCourses.forEach { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (course.id in selected) selected - course.id else selected + course.id
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = course.id in selected, onCheckedChange = null)
                                CourseIconAvatar(course.icon, modifier = Modifier.padding(end = 8.dp))
                                Text(course.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selected) },
                enabled = name.isNotBlank() || selected.isNotEmpty(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSectionDialog(onConfirm: (name: String, date: Long?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add section") },
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
            TextButton(onClick = { onConfirm(name, date) }, enabled = name.isNotBlank()) { Text("Add") }
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
