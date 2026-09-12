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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.data.local.dao.SectionCourseWithCourse
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.courseCategoryGroupLabel
import com.ridvan.target.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    onCourseClick: (Long) -> Unit,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_section))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_section))
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
                stringResource(R.string.sectiondetail_date_prefix, section?.date?.let { formatDate(it) } ?: stringResource(R.string.common_not_set)),
                modifier = Modifier.padding(16.dp),
            )
            if (assignedCourses.isEmpty()) {
                Text(stringResource(R.string.sectiondetail_no_courses), modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                val expandedGroups = remember { mutableStateMapOf<CourseCategory?, Boolean>() }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    sectionCourseGroups(assignedCourses).forEach { (category, groupCourses) ->
                        val expanded = expandedGroups[category] ?: false
                        item {
                            SectionCourseGroupHeader(
                                category,
                                expanded = expanded,
                                onToggleExpand = { expandedGroups[category] = !expanded },
                            )
                        }
                        if (expanded) {
                            items(groupCourses, key = { it.sectionCourse.id }) { course ->
                                AssignedCourseRow(
                                    course,
                                    onClick = { onCourseClick(course.sectionCourse.courseId) },
                                    onRemove = { viewModel.removeCourse(course.sectionCourse) },
                                )
                                HorizontalDivider()
                            }
                        }
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
            title = { Text(stringResource(R.string.sectiondetail_delete_title)) },
            text = { Text(stringResource(R.string.sectiondetail_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSection()
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
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

private fun sectionCourseGroups(courses: List<SectionCourseWithCourse>): List<Pair<CourseCategory?, List<SectionCourseWithCourse>>> {
    val byCategory = courses.groupBy { it.courseCategory }
    return listOfNotNull(
        byCategory[CourseCategory.QUANTITATIVE]?.let { CourseCategory.QUANTITATIVE to it },
        byCategory[CourseCategory.VERBAL]?.let { CourseCategory.VERBAL to it },
        byCategory[null]?.let { null to it },
    )
}

@Composable
private fun SectionCourseGroupHeader(category: CourseCategory?, expanded: Boolean, onToggleExpand: () -> Unit) {
    val label = courseCategoryGroupLabel(category)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleExpand) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) stringResource(R.string.cd_collapse_x, label) else stringResource(R.string.cd_expand_x, label),
            )
        }
    }
}

@Composable
private fun AssignedCourseRow(course: SectionCourseWithCourse, onClick: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.courseIcon) },
        headlineContent = { Text(course.courseName) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_remove_course))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
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
        title = { Text(stringResource(R.string.dialog_add_courses_title)) },
        text = {
            if (available.isEmpty()) {
                Text(stringResource(R.string.sectiondetail_all_assigned))
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
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) { Text(stringResource(R.string.common_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
        title = { Text(stringResource(R.string.dialog_edit_section_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_section_name)) },
                    singleLine = true,
                )
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showDatePicker = true }) {
                    Text(stringResource(R.string.label_date))
                    Text(date?.let { formatDate(it) } ?: stringResource(R.string.common_tap_to_set))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, date) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
