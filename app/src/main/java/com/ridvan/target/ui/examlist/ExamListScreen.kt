package com.ridvan.target.ui.examlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME
import com.ridvan.target.data.local.entity.Section
import com.ridvan.target.ui.common.AddFab
import com.ridvan.target.ui.common.AddOrEditExamDialog
import com.ridvan.target.ui.common.daysUntilLabel
import com.ridvan.target.ui.common.examTypeDisplayName
import com.ridvan.target.ui.common.formatDate
import com.ridvan.target.ui.shell.AppShell
import com.ridvan.target.ui.shell.ShellDestination
import com.ridvan.target.ui.shell.ShellNavigation

@Composable
fun ExamListScreen(
    shellNavigation: ShellNavigation,
    onExamClick: (Long) -> Unit,
    viewModel: ExamListViewModel = viewModel(),
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val examsWithSections by viewModel.examsWithSections.collectAsStateWithLifecycle()
    val examTypes by viewModel.examTypes.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    AppShell(
        navigation = shellNavigation,
        currentDestination = ShellDestination.EXAMS,
        title = stringResource(R.string.exam_list_title),
        floatingActionButton = {
            AddFab(onClick = { showAddDialog = true })
        },
    ) { innerPadding ->
        if (exams.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.exam_list_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(examsWithSections, key = { it.first.exam.id }) { (examWithType, sections) ->
                    ExamRow(examWithType, sections, onClick = { onExamClick(examWithType.exam.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddOrEditExamDialog(
            examTypes = examTypes,
            languages = languages,
            onConfirm = { name, examTypeId, hasSections, examDate, studyStartDate, languageId ->
                viewModel.addExam(name, examTypeId, hasSections, examDate, studyStartDate, languageId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun ExamRow(item: ExamWithType, sections: List<Section>, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.exam.name) },
        supportingContent = {
            val displayTypeName = examTypeDisplayName(item.examTypeName)
            val typeText = if (item.examTypeName == LANGUAGE_EXAM_TYPE_NAME) {
                "$displayTypeName · ${item.languageName ?: stringResource(R.string.common_no_language_set)}"
            } else {
                displayTypeName
            }
            Column {
                if (item.exam.hasSections) {
                    val sectionNames = if (sections.isEmpty()) {
                        stringResource(R.string.exam_row_no_sections)
                    } else {
                        sections.joinToString(", ") { it.name }
                    }
                    Text("$typeText · $sectionNames")
                    val nearest = sections.filter { it.date != null }.minByOrNull { it.date!! }
                    if (nearest != null) {
                        Text(
                            stringResource(R.string.exam_countdown_section, daysUntilLabel(nearest.date!!), nearest.name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    val dateText = item.exam.examDate?.let { formatDate(it) } ?: stringResource(R.string.exam_row_no_date_set)
                    Text("$typeText · $dateText")
                    item.exam.examDate?.let { date ->
                        Text(
                            daysUntilLabel(date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
