package com.ridvan.target.ui.studysource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CourseStudySourceScreen(
    onSourceClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseStudySourceViewModel = viewModel(),
) {
    val course by viewModel.course.collectAsStateWithLifecycle()
    val studySources by viewModel.studySources.collectAsStateWithLifecycle()

    StudySourceListContent(
        title = course?.name.orEmpty(),
        studySources = studySources,
        onAdd = viewModel::addStudySource,
        onSourceClick = onSourceClick,
        onBack = onBack,
    )
}
