package com.ridvan.target.ui.studysource

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.data.local.entity.StudySource
import com.ridvan.target.ui.navigation.LanguageStudySourceRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageStudySourceViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val languageId: Long = savedStateHandle.toRoute<LanguageStudySourceRoute>().languageId
    private val targetApplication = application as TargetApplication
    private val languageDao = targetApplication.database.languageDao()
    private val studySourceDao = targetApplication.database.studySourceDao()

    val language: StateFlow<Language?> = languageDao.getById(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val studySources: StateFlow<List<StudySource>> = studySourceDao.getByLanguageId(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addStudySource(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { studySourceDao.insert(StudySource(name = trimmed, languageId = languageId)) }
    }

    fun updateStudySource(studySource: StudySource, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { studySourceDao.update(studySource.copy(name = trimmed)) }
    }

    fun deleteStudySource(studySource: StudySource) {
        viewModelScope.launch { studySourceDao.delete(studySource) }
    }
}
