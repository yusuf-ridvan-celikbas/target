package com.ridvan.target.ui.studysourcedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.StudySource
import com.ridvan.target.data.local.entity.StudySourceType
import com.ridvan.target.ui.navigation.StudySourceDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudySourceDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val studySourceId: Long = savedStateHandle.toRoute<StudySourceDetailRoute>().studySourceId
    private val database = (application as TargetApplication).database
    private val studySourceDao = database.studySourceDao()
    private val courseDao = database.courseDao()
    private val languageDao = database.languageDao()

    val studySource: StateFlow<StudySource?> = studySourceDao.getById(studySourceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val subjectName: StateFlow<String> = studySource.filterNotNull().flatMapLatest { source ->
        when {
            source.courseId != null -> courseDao.getById(source.courseId).map { it?.name.orEmpty() }
            source.languageId != null -> languageDao.getById(source.languageId).map { it?.name.orEmpty() }
            else -> flowOf("")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun updateStudySource(name: String, type: StudySourceType, publisher: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = studySource.value ?: return@launch
            studySourceDao.update(current.copy(name = trimmed, type = type, publisher = publisher))
        }
    }

    fun deleteStudySource() {
        viewModelScope.launch {
            studySource.value?.let { studySourceDao.delete(it) }
        }
    }
}
