package com.ridvan.target.ui.courselist

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
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun CourseListScreen(
    shellNavigation: ShellNavigation,
    onCourseTypeClick: (ExamType) -> Unit,
    onLanguageTypeClick: () -> Unit,
    viewModel: CourseListViewModel = viewModel(),
) {
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()

    AppShell(navigation = shellNavigation, title = stringResource(R.string.course_list_title)) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(examTypes, key = { it.id }) { examType ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.course_type_bucket_title, examTypeDisplayName(examType.name))) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (examType.name == LANGUAGE_EXAM_TYPE_NAME) onLanguageTypeClick() else onCourseTypeClick(examType)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
