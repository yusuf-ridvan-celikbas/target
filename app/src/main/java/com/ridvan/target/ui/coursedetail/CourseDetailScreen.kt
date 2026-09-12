package com.ridvan.target.ui.coursedetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.ui.common.CourseIconPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    onStudyResourcesClick: () -> Unit,
    onTopicsClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: CourseDetailViewModel = viewModel(),
) {
    val course by viewModel.course.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit course")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete course")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            val examTypeName = examTypes.firstOrNull { it.id == course?.examTypeId }?.name ?: "Not set"
            Text("Exam type: $examTypeName")
            Text("Category: ${categoryLabel(course?.category)}")
            Button(
                onClick = onStudyResourcesClick,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("Study Resources")
            }
            Button(
                onClick = onTopicsClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Topics")
            }
        }
    }

    if (showEditDialog && course != null) {
        CourseEditDialog(
            examTypes = examTypes.filterNot { it.name == LANGUAGE_EXAM_TYPE_NAME },
            initialName = course!!.name,
            initialIcon = course!!.icon,
            initialExamTypeId = course!!.examTypeId,
            initialCategory = course!!.category,
            onConfirm = { name, icon, examTypeId, category ->
                viewModel.updateCourse(name, icon, examTypeId, category)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete course?") },
            text = { Text("This removes \"${course?.name}\" from any exams or sections it's assigned to.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse()
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

@Composable
private fun CourseEditDialog(
    examTypes: List<ExamType>,
    initialName: String,
    initialIcon: String?,
    initialExamTypeId: Long?,
    initialCategory: CourseCategory?,
    onConfirm: (name: String, icon: String?, examTypeId: Long, category: CourseCategory?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var examTypeId by remember { mutableStateOf(initialExamTypeId ?: examTypes.firstOrNull()?.id) }
    var category by remember { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit course") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CourseExamTypeField(
                    examTypes = examTypes,
                    selectedId = examTypeId,
                    onSelect = { examTypeId = it },
                )
                CourseCategoryField(
                    selected = category,
                    onSelect = { category = it },
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
            TextButton(
                onClick = { examTypeId?.let { onConfirm(name, icon, it, category) } },
                enabled = name.isNotBlank() && examTypeId != null,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun categoryLabel(category: CourseCategory?): String = when (category) {
    CourseCategory.QUANTITATIVE -> "Quantitative"
    CourseCategory.VERBAL -> "Verbal"
    null -> "Not set"
}

@Composable
private fun CourseCategoryField(selected: CourseCategory?, onSelect: (CourseCategory?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Category", style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            CategoryToggleSegment(
                text = "Unset",
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = Modifier.weight(1f),
            )
            CategoryToggleSegment(
                text = "Quantitative",
                selected = selected == CourseCategory.QUANTITATIVE,
                onClick = { onSelect(CourseCategory.QUANTITATIVE) },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            CategoryToggleSegment(
                text = "Verbal",
                selected = selected == CourseCategory.VERBAL,
                onClick = { onSelect(CourseCategory.VERBAL) },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun CategoryToggleSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "categoryToggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "categoryToggleContentColor",
    )
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun CourseExamTypeField(examTypes: List<ExamType>, selectedId: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text("Exam type", style = MaterialTheme.typography.labelSmall)
            Text(
                examTypes.firstOrNull { it.id == selectedId }?.name ?: "Select",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            examTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelect(type.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
