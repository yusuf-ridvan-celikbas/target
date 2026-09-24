package com.ridvan.target.ui.focustimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.FocusSessionWithLinks
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The full Focus Timer history; filtering happens in FocusHistoryScreen over this one fetched list. */
class FocusHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val focusSessionDao = targetApplication.database.focusSessionDao()
    private val userId = targetApplication.preferences.currentUserId

    val history: StateFlow<List<FocusSessionWithLinks>> = (userId?.let { focusSessionDao.getAllWithLinksByUserId(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch { focusSessionDao.delete(session) }
    }
}
