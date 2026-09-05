package com.ridvan.target.ui.languagedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Language
import com.ridvan.target.ui.navigation.LanguageDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val languageId: Long = savedStateHandle.toRoute<LanguageDetailRoute>().languageId
    private val targetApplication = application as TargetApplication
    private val languageDao = targetApplication.database.languageDao()

    val language: StateFlow<Language?> = languageDao.getById(languageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateLanguage(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = language.value ?: return@launch
            languageDao.update(current.copy(name = trimmed))
        }
    }

    fun deleteLanguage() {
        viewModelScope.launch {
            language.value?.let { languageDao.delete(it) }
        }
    }
}
