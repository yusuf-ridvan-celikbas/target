package com.ridvan.target.ui.studyresource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LanguageStudyResourceScreen(
    onResourceClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LanguageStudyResourceViewModel = viewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val studyResources by viewModel.filteredStudyResources.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()

    StudyResourceListContent(
        title = language?.name.orEmpty(),
        studyResources = studyResources,
        selectedType = selectedType,
        onTypeSelect = viewModel::setType,
        onAdd = viewModel::addStudyResource,
        onResourceClick = onResourceClick,
        onBack = onBack,
    )
}
