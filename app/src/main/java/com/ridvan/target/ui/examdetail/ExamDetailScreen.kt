package com.ridvan.target.ui.examdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.export.ProgressCsvExporter
import com.ridvan.target.data.local.dao.ExamCourseWithCourse
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.ui.common.AddOrEditExamDialog
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.HelpTooltip
import com.ridvan.target.ui.common.courseCategoryGroupLabel
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.examDisplayLabel
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.common.shareCsvFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    onSectionClick: (Long) -> Unit,
    onCourseClick: (Long) -> Unit,
    onLanguageClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamDetailViewModel = viewModel(),
) {
    val exam by viewModel.exam.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val availableCoursesToAdd by viewModel.availableCoursesToAdd.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportHeaders = listOf(
        stringResource(R.string.csv_header_course),
        stringResource(R.string.csv_header_topic),
        stringResource(R.string.csv_header_target_tests),
        stringResource(R.string.csv_header_target_questions),
        stringResource(R.string.csv_header_tests_solved),
        stringResource(R.string.csv_header_correct),
        stringResource(R.string.csv_header_wrong),
        stringResource(R.string.csv_header_remaining_tests),
        stringResource(R.string.csv_header_remaining_questions),
        stringResource(R.string.csv_header_net),
        stringResource(R.string.csv_header_accuracy),
        stringResource(R.string.csv_header_duration_minutes),
    )
    val exportChooserTitle = stringResource(R.string.export_csv_chooser_title)

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var coursesExpanded by remember { mutableStateOf(false) }
    var sectionsExpanded by remember { mutableStateOf(false) }
    val expandedCourseGroups = remember { mutableStateMapOf<CourseCategory?, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exam?.let { examDisplayLabel(it) }.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_exam))
                    }
                    if (exam?.hasSections != true) {
                        IconButton(onClick = {
                            viewModel.fetchProgressExportRows { rows ->
                                val file = ProgressCsvExporter.writeProgressCsv(
                                    context, exam?.name.orEmpty(), exportHeaders, rows,
                                )
                                shareCsvFile(context, file, exportChooserTitle)
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_export_progress_csv))
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_exam))
                    }
                },
            )
        },
    ) { innerPadding ->
        val currentExam = exam
        val isLanguageExam = currentExam != null &&
            examTypes.firstOrNull { it.id == currentExam.examTypeId }?.name == LANGUAGE_EXAM_TYPE_NAME

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(innerPadding)) {
            currentExam?.let {
                val languageId = currentExam.languageId
                val languageName = languages.firstOrNull { language -> language.id == languageId }?.name
                item {
                    ExamSummary(
                        currentExam.examDate,
                        currentExam.studyStartDate,
                        currentExam.hasSections,
                        isLanguageExam,
                        languageName,
                        currentExam.level,
                        onLanguageClick = { languageId?.let(onLanguageClick) },
                    )
                }
            }

            if (!isLanguageExam) {
                val hasSections = currentExam?.hasSections == true
                item {
                    GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SectionHeader(
                            stringResource(R.string.label_courses),
                            onAddClick = { showAddCourseDialog = true },
                            expanded = if (hasSections) null else coursesExpanded,
                            onToggleExpand = if (hasSections) null else ({ coursesExpanded = !coursesExpanded }),
                        )
                        if (!hasSections && coursesExpanded) {
                            if (courses.isEmpty()) {
                                EmptyHint(stringResource(R.string.examdetail_no_courses))
                            } else {
                                examCourseGroups(courses).forEach { (category, groupCourses) ->
                                    val groupExpanded = expandedCourseGroups[category] ?: false
                                    CourseGroupHeader(
                                        category,
                                        expanded = groupExpanded,
                                        onToggleExpand = { expandedCourseGroups[category] = !groupExpanded },
                                    )
                                    if (groupExpanded) {
                                        groupCourses.forEach { course ->
                                            CourseRow(
                                                course,
                                                onClick = { onCourseClick(course.examCourse.courseId) },
                                                onRemove = { viewModel.removeCourse(course.examCourse) },
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (exam?.hasSections == true) {
                item {
                    GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SectionHeader(
                            stringResource(R.string.label_sections),
                            onAddClick = { showAddSectionDialog = true },
                            expanded = sectionsExpanded,
                            onToggleExpand = { sectionsExpanded = !sectionsExpanded },
                        )
                        if (sectionsExpanded) {
                            if (sections.isEmpty()) {
                                EmptyHint(stringResource(R.string.examdetail_no_sections))
                            } else {
                                sections.forEach { section ->
                                    SectionRow(section, onClick = { onSectionClick(section.id) })
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && exam != null) {
        AddOrEditExamDialog(
            examTypes = examTypes,
            languages = languages,
            initial = exam,
            onConfirm = { name, examTypeId, hasSections, examDate, studyStartDate, languageId, level ->
                viewModel.updateExam(name, examTypeId, hasSections, examDate, studyStartDate, languageId, level)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.examdetail_delete_title)) },
            text = { Text(stringResource(R.string.examdetail_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExam()
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
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
private fun ExamSummary(
    examDate: Long?,
    studyStartDate: Long?,
    hasSections: Boolean,
    isLanguageExam: Boolean,
    languageName: String?,
    level: String?,
    onLanguageClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (!hasSections) {
            Text(stringResource(R.string.examdetail_exam_date, examDate?.let { formatDate(it) } ?: stringResource(R.string.common_not_set)))
        }
        Text(stringResource(R.string.examdetail_study_start, studyStartDate?.let { formatDate(it) } ?: stringResource(R.string.common_not_set)))
        if (isLanguageExam) {
            val languageModifier = if (languageName != null) Modifier.clickable(onClick = onLanguageClick) else Modifier
            val languagePrefix = stringResource(R.string.examdetail_language_prefix)
            val noLanguageSet = stringResource(R.string.common_no_language_set)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append(languagePrefix)
                        if (languageName != null) {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ) {
                                append(languageName)
                            }
                        } else {
                            append(noLanguageSet)
                        }
                    },
                    modifier = languageModifier,
                )
                HelpTooltip(R.string.help_tooltip_exam_language, R.string.cd_help_exam_language)
            }
            Text(stringResource(R.string.examdetail_level, level?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_not_set)))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onAddClick: () -> Unit,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        TextButton(onClick = onAddClick) { Text(stringResource(R.string.action_add_prefixed)) }
        if (expanded != null && onToggleExpand != null) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse_courses) else stringResource(R.string.cd_expand_courses),
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
}

private fun examCourseGroups(courses: List<ExamCourseWithCourse>): List<Pair<CourseCategory?, List<ExamCourseWithCourse>>> {
    val byCategory = courses.groupBy { it.courseCategory }
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
private fun CourseRow(course: ExamCourseWithCourse, onClick: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.courseIcon) },
        headlineContent = { Text(courseDisplayName(course.courseName)) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_remove_course))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SectionRow(section: Section, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(section.name) },
        supportingContent = { Text(section.date?.let { formatDate(it) } ?: stringResource(R.string.common_not_set)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
        title = { Text(stringResource(R.string.dialog_add_course_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_new_course_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (availableCourses.isNotEmpty()) {
                    Text(
                        stringResource(R.string.label_pick_existing_courses),
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
                                Text(courseDisplayName(course.name))
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
            ) { Text(stringResource(R.string.common_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
        title = { Text(stringResource(R.string.dialog_add_section_title)) },
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
            TextButton(onClick = { onConfirm(name, date) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.common_add)) }
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
