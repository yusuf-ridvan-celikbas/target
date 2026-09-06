package com.ridvan.target.ui.studyresource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CourseStudyResourceScreen(
    onResourceClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseStudyResourceViewModel = viewModel(),
) {
    val course by viewModel.course.collectAsStateWithLifecycle()
    val studyResources by viewModel.studyResources.collectAsStateWithLifecycle()

    StudyResourceListContent(
        title = course?.name.orEmpty(),
        studyResources = studyResources,
        onAdd = viewModel::addStudyResource,
        onResourceClick = onResourceClick,
        onBack = onBack,
    )
}
