package com.ridvan.target.ui.studyresource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.ui.common.courseDisplayName

@Composable
fun CourseStudyResourceScreen(
    onResourceClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseStudyResourceViewModel = viewModel(),
) {
    val course by viewModel.course.collectAsStateWithLifecycle()
    val studyResources by viewModel.filteredStudyResources.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val targetTotals by viewModel.targetTotals.collectAsStateWithLifecycle()
    val progressTotals by viewModel.progressTotals.collectAsStateWithLifecycle()
    val practiceExamTotals by viewModel.practiceExamTotals.collectAsStateWithLifecycle()

    StudyResourceListContent(
        title = course?.name?.let { courseDisplayName(it) }.orEmpty(),
        studyResources = studyResources,
        selectedType = selectedType,
        onTypeSelect = viewModel::setType,
        onAdd = viewModel::addStudyResource,
        onResourceClick = onResourceClick,
        onBack = onBack,
        summary = when (selectedType) {
            StudyResourceType.PRACTICE_EXAM -> {
                {
                    val totalQuestions = practiceExamTotals.totalCorrect + practiceExamTotals.totalWrong
                    val accuracy = if (totalQuestions == 0) 0 else practiceExamTotals.totalCorrect * 100 / totalQuestions
                    val durationText = stringResource(
                        R.string.duration_format,
                        practiceExamTotals.totalDurationMinutes / 60,
                        practiceExamTotals.totalDurationMinutes % 60,
                    )
                    Text(
                        stringResource(
                            R.string.practice_exam_entries_summary,
                            practiceExamTotals.totalEntries,
                            practiceExamTotals.totalCorrect,
                            practiceExamTotals.totalWrong,
                            accuracy,
                            durationText,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            StudyResourceType.LECTURE_TEXTBOOK, StudyResourceType.LECTURE_NOTES -> null
            StudyResourceType.QUESTION_BANK, null -> {
                {
                    val remainingTests = (targetTotals.totalTestCount - progressTotals.totalTestsSolved).coerceAtLeast(0)
                    val remainingQuestions = (targetTotals.totalQuestionCount - (progressTotals.totalSolved + progressTotals.totalUnsolved)).coerceAtLeast(0)
                    val netScore = progressTotals.totalSolved - progressTotals.totalUnsolved / 4.0
                    val durationText = stringResource(
                        R.string.duration_format,
                        progressTotals.totalDurationMinutes / 60,
                        progressTotals.totalDurationMinutes % 60,
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.label_target_counts, targetTotals.totalTestCount, targetTotals.totalQuestionCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.label_remaining_counts, remainingTests, remainingQuestions),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(
                                R.string.progress_aggregate,
                                progressTotals.totalTestsSolved,
                                progressTotals.totalSolved,
                                progressTotals.totalUnsolved,
                                "%.2f".format(netScore),
                                durationText,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}
