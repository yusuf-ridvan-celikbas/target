package com.ridvan.target.ui.topiclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.ExamType
import com.ridvan.target.ui.common.examTypeDisplayName
import com.ridvan.target.ui.courselist.CourseListViewModel
import com.ridvan.target.ui.shell.AppShell
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
    viewModel: CourseListViewModel = viewModel(),
) {
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val courseExamTypes = examTypes.filterNot { it.name == LANGUAGE_EXAM_TYPE_NAME }

    AppShell(navigation = shellNavigation, title = stringResource(R.string.label_topics)) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(courseExamTypes, key = { it.id }) { examType ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.course_type_bucket_title, examTypeDisplayName(examType.name))) },
                    modifier = Modifier.fillMaxWidth().clickable { onCourseTypeClick(examType) },
                )
                HorizontalDivider()
            }
        }
    }
}
