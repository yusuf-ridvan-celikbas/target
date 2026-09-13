package com.ridvan.target.ui.courselist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.examTypeDisplayName
import com.ridvan.target.ui.languagelist.LanguageListViewModel
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun CourseListScreen(
    shellNavigation: ShellNavigation,
    onCourseTypeClick: (ExamType) -> Unit,
    onLanguageTypeClick: () -> Unit,
    onCourseShortcutClick: (Long) -> Unit,
    onLanguageShortcutClick: (Long) -> Unit,
    viewModel: CourseListViewModel = viewModel(),
    languageListViewModel: LanguageListViewModel = viewModel(),
) {
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val allCourses by viewModel.courses.collectAsStateWithLifecycle()
    val languages by languageListViewModel.languages.collectAsStateWithLifecycle()
    val expandedTypes = remember { mutableStateMapOf<Long, Boolean>() }

    AppShell(navigation = shellNavigation, title = stringResource(R.string.course_list_title)) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            examTypes.forEach { examType ->
                val isLanguageType = examType.name == LANGUAGE_EXAM_TYPE_NAME
                val expanded = expandedTypes[examType.id] ?: false
                item(key = "bucket-${examType.id}") {
                    ExamTypeBucketRow(
                        title = stringResource(R.string.course_type_bucket_title, examTypeDisplayName(examType.name)),
                        expanded = expanded,
                        onToggleExpand = { expandedTypes[examType.id] = !expanded },
                        onClick = { if (isLanguageType) onLanguageTypeClick() else onCourseTypeClick(examType) },
                    )
                    HorizontalDivider()
                }
                if (expanded) {
                    if (isLanguageType) {
                        if (languages.isEmpty()) {
                            item(key = "empty-${examType.id}") { BucketEmptyHint() }
                        } else {
                            items(languages, key = { "lang-${it.id}" }) { language ->
                                LanguageShortcutRow(language = language, onClick = { onLanguageShortcutClick(language.id) })
                                HorizontalDivider()
                            }
                        }
                    } else {
                        val coursesForType = allCourses.filter { it.examTypeId == examType.id }
                        if (coursesForType.isEmpty()) {
                            item(key = "empty-${examType.id}") { BucketEmptyHint() }
                        } else {
                            items(coursesForType, key = { "course-${it.id}" }) { course ->
                                CourseShortcutRow(course = course, onClick = { onCourseShortcutClick(course.id) })
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamTypeBucketRow(title: String, expanded: Boolean, onToggleExpand: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse_x, title) else stringResource(R.string.cd_expand_x, title),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun CourseShortcutRow(course: Course, onClick: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.icon) },
        headlineContent = { Text(courseDisplayName(course.name)) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun LanguageShortcutRow(language: Language, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(language.name) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun BucketEmptyHint() {
    Text(
        stringResource(R.string.bucket_expand_empty_hint),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
