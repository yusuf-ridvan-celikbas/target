package com.ridvan.target.ui.languagelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.Language
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageListViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val languageDao = targetApplication.database.languageDao()
    private val userId = targetApplication.preferences.currentUserId

    val languages: StateFlow<List<Language>> = (userId?.let { languageDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addLanguage(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null) return
        viewModelScope.launch { languageDao.insert(Language(name = trimmed, userId = userId)) }
    }
}
