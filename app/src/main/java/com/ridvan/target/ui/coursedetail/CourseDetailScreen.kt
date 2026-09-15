package com.ridvan.target.ui.coursedetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.export.CourseQuestionBankCsvExporter
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.ui.common.CourseIconPicker
import com.ridvan.target.ui.common.SegmentedToggle
import com.ridvan.target.ui.common.SegmentedToggleOption
import com.ridvan.target.ui.common.courseCategoryValueLabel
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.examTypeDisplayName
import com.ridvan.target.ui.common.readTextFromUri
import com.ridvan.target.ui.common.shareCsvFile

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
    val context = LocalContext.current
    val exportHeaders = listOf(
        stringResource(R.string.csv_header_resource),
        stringResource(R.string.csv_header_publisher),
        stringResource(R.string.csv_header_topic),
        stringResource(R.string.csv_header_test_count),
        stringResource(R.string.csv_header_question_count),
    )
    val exportChooserTitle = stringResource(R.string.export_csv_chooser_title)
    var importOutcome by remember { mutableStateOf<QuestionBankImportOutcome?>(null) }
    var importReadFailed by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val content = readTextFromUri(context, uri)
        if (content == null) {
            importReadFailed = true
        } else {
            viewModel.importQuestionBankCsv(content) { outcome -> importOutcome = outcome }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course?.name?.let { courseDisplayName(it) }.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_course))
                    }
                    IconButton(onClick = {
                        viewModel.fetchQuestionBankExportRows { rows ->
                            val file = CourseQuestionBankCsvExporter.writeCourseQuestionBankCsv(
                                context, course?.name.orEmpty(), exportHeaders, rows,
                            )
                            shareCsvFile(context, file, exportChooserTitle)
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_export_course_csv))
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.cd_import_course_csv))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_course))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp)) {
            val examTypeName = examTypes.firstOrNull { it.id == course?.examTypeId }?.name?.let { examTypeDisplayName(it) }
                ?: stringResource(R.string.common_not_set)
            Text(stringResource(R.string.coursedetail_exam_type, examTypeName))
            Text(stringResource(R.string.coursedetail_category, courseCategoryValueLabel(course?.category)))
            Button(
                onClick = onStudyResourcesClick,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.label_study_resources))
            }
            Button(
                onClick = onTopicsClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.label_topics))
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
            title = { Text(stringResource(R.string.coursedetail_delete_title)) },
            text = { Text(stringResource(R.string.coursedetail_delete_message, course?.name?.let { courseDisplayName(it) }.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse()
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (importReadFailed) {
        AlertDialog(
            onDismissRequest = { importReadFailed = false },
            title = { Text(stringResource(R.string.import_result_title)) },
            text = { Text(stringResource(R.string.import_result_unreadable)) },
            confirmButton = {
                TextButton(onClick = { importReadFailed = false }) { Text(stringResource(R.string.common_ok)) }
            },
        )
    }

    importOutcome?.let { outcome ->
        AlertDialog(
            onDismissRequest = { importOutcome = null },
            title = { Text(stringResource(R.string.import_result_title)) },
            text = {
                Text(
                    when (outcome) {
                        is QuestionBankImportOutcome.Imported -> stringResource(
                            R.string.import_result_summary,
                            outcome.result.rowsProcessed,
                            outcome.result.resourcesCreated,
                            outcome.result.topicsCreated,
                            outcome.result.attachmentsCreated,
                            outcome.result.attachmentsUpdated,
                        )
                        QuestionBankImportOutcome.EmptyOrInvalid -> stringResource(R.string.import_result_invalid)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { importOutcome = null }) { Text(stringResource(R.string.common_ok)) }
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
        title = { Text(stringResource(R.string.dialog_edit_course_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
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
            TextButton(
                onClick = { examTypeId?.let { onConfirm(name, icon, it, category) } },
                enabled = name.isNotBlank() && examTypeId != null,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun CourseCategoryField(selected: CourseCategory?, onSelect: (CourseCategory?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelSmall)
        SegmentedToggle(
            options = listOf(
                SegmentedToggleOption<CourseCategory?>(null, stringResource(R.string.category_unset)),
                SegmentedToggleOption(CourseCategory.QUANTITATIVE, stringResource(R.string.category_quantitative)),
                SegmentedToggleOption(CourseCategory.VERBAL, stringResource(R.string.category_verbal)),
            ),
            selected = selected,
            onSelect = onSelect,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun CourseExamTypeField(examTypes: List<ExamType>, selectedId: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(stringResource(R.string.exam_field_type_label), style = MaterialTheme.typography.labelSmall)
            Text(
                examTypes.firstOrNull { it.id == selectedId }?.name?.let { examTypeDisplayName(it) } ?: stringResource(R.string.common_select),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            examTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(examTypeDisplayName(type.name)) },
                    onClick = {
                        onSelect(type.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
