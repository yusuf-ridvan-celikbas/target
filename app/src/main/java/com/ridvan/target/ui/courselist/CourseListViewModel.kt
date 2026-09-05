package com.ridvan.target.ui.courselist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.entity.ExamType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CourseListViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val examTypeDao = targetApplication.database.examTypeDao()

    val examTypes: StateFlow<List<ExamType>> = examTypeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
