package com.ridvan.target.ui.topiclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.Course
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.ui.common.CourseIconAvatar
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.common.courseDisplayName
import com.ridvan.target.ui.common.examTypeDisplayName
import com.ridvan.target.ui.courselist.CourseListViewModel
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

/**
 * Topics are Course-only (see CLAUDE.md) — unlike CourseListScreen/StudyResourceHomeScreen,
 * this bucket menu excludes the Language Exam Courses entry entirely rather than routing it
 * somewhere, since a Language has no Topics list to drill into.
 */
@Composable
fun TopicHomeScreen(
    shellNavigation: ShellNavigation,
    onCourseTypeClick: (ExamType) -> Unit,
    onCourseShortcutClick: (Long) -> Unit,
    viewModel: CourseListViewModel = viewModel(),
) {
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val courseExamTypes = examTypes.filterNot { it.name == LANGUAGE_EXAM_TYPE_NAME }
    val allCourses by viewModel.courses.collectAsStateWithLifecycle()
    val expandedTypes = remember { mutableStateMapOf<Long, Boolean>() }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.TOPICS,
        title = stringResource(R.string.label_topics),
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            courseExamTypes.forEach { examType ->
                val expanded = expandedTypes[examType.id] ?: false
                item(key = "bucket-${examType.id}") {
                    GroupedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ExamTypeBucketRow(
                            title = stringResource(R.string.course_type_bucket_title, examTypeDisplayName(examType.name)),
                            expanded = expanded,
                            onToggleExpand = { expandedTypes[examType.id] = !expanded },
                            onClick = { onCourseTypeClick(examType) },
                        )
                        if (expanded) {
                            val coursesForType = allCourses.filter { it.examTypeId == examType.id }
                            if (coursesForType.isEmpty()) {
                                BucketEmptyHint()
                            } else {
                                coursesForType.forEach { course ->
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun CourseShortcutRow(course: Course, onClick: () -> Unit) {
    ListItem(
        leadingContent = { CourseIconAvatar(course.icon) },
        headlineContent = { Text(courseDisplayName(course.name)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
