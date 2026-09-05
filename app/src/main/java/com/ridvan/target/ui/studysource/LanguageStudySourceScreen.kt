package com.ridvan.target.ui.studysource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LanguageStudySourceScreen(
    onBack: () -> Unit,
    viewModel: LanguageStudySourceViewModel = viewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val studySources by viewModel.studySources.collectAsStateWithLifecycle()

    StudySourceListContent(
        title = language?.name.orEmpty(),
        studySources = studySources,
        onAdd = viewModel::addStudySource,
        onUpdate = viewModel::updateStudySource,
        onDelete = viewModel::deleteStudySource,
        onBack = onBack,
    )
}
