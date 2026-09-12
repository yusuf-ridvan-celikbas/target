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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.CourseIconPicker
import com.ridvan.target.ui.common.courseCategoryGroupLabel
import com.ridvan.target.ui.common.examTypeDisplayName

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.course_type_bucket_title, examTypeDisplayName(examTypeName))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                Text(stringResource(R.string.course_type_empty))
            }
        } else {
            val expandedGroups = remember { mutableStateMapOf<CourseCategory?, Boolean>() }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                courseGroups(courses).forEach { (category, groupCourses) ->
                    val expanded = expandedGroups[category] ?: false
                    item {
                        CourseGroupHeader(
                            category,
                            expanded = expanded,
                            onToggleExpand = { expandedGroups[category] = !expanded },
                        )
                    }
                    if (expanded) {
                        items(groupCourses, key = { it.id }) { course ->
                            CourseRow(course = course, onClick = { onCourseClick(course.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CourseDialog(
            title = stringResource(R.string.dialog_add_course_title),
            initialName = "",
            initialIcon = null,
            onConfirm = { name, icon ->
                viewModel.addCourse(name, icon)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

private fun courseGroups(courses: List<Course>): List<Pair<CourseCategory?, List<Course>>> {
    val byCategory = courses.groupBy { it.category }
    return listOfNotNull(
        byCategory[CourseCategory.QUANTITATIVE]?.let { CourseCategory.QUANTITATIVE to it },
        byCategory[CourseCategory.VERBAL]?.let { CourseCategory.VERBAL to it },
        byCategory[null]?.let { null to it },
    )
}

@Composable
private fun CourseGroupHeader(category: CourseCategory?, expanded: Boolean, onToggleExpand: () -> Unit) {
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
private fun CourseRow(course: Course, onClick: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.icon) },
        headlineContent = { Text(course.name) },
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
                    label = { Text(stringResource(R.string.common_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.label_icon),
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
            TextButton(onClick = { onConfirm(name, icon) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
