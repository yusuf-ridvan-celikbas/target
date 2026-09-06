package com.ridvan.target.ui.studyresource

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.StudyResource
import com.ridvan.target.data.local.entity.StudyResourceType
import com.ridvan.target.ui.navigation.LanguageStudyResourceRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageStudyResourceViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val languageId: Long = savedStateHandle.toRoute<LanguageStudyResourceRoute>().languageId
    private val targetApplication = application as TargetApplication
    private val languageDao = targetApplication.database.languageDao()
    private val studyResourceDao = targetApplication.database.studyResourceDao()

    val language: StateFlow<Language?> = languageDao.getById(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val studyResources: StateFlow<List<StudyResource>> = studyResourceDao.getByLanguageId(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addStudyResource(name: String, type: StudyResourceType, publisher: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            studyResourceDao.insert(StudyResource(name = trimmed, languageId = languageId, type = type, publisher = publisher))
        }
    }
}
