package com.ridvan.target.ui.focustimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.FocusPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusPresetListViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val focusPresetDao = targetApplication.database.focusPresetDao()
    private val userId = targetApplication.preferences.currentUserId

    val presets: StateFlow<List<FocusPreset>> = (userId?.let { focusPresetDao.getByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPreset(name: String, workMinutes: Int, breakMinutes: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null || workMinutes <= 0 || breakMinutes <= 0) return
        viewModelScope.launch {
            focusPresetDao.insert(FocusPreset(userId = userId, name = trimmed, workMinutes = workMinutes, breakMinutes = breakMinutes))
        }
    }

    fun updatePreset(preset: FocusPreset, name: String, workMinutes: Int, breakMinutes: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || workMinutes <= 0 || breakMinutes <= 0) return
        viewModelScope.launch {
            focusPresetDao.update(preset.copy(name = trimmed, workMinutes = workMinutes, breakMinutes = breakMinutes))
        }
    }

    fun deletePreset(preset: FocusPreset) {
        viewModelScope.launch { focusPresetDao.delete(preset) }
    }
}
