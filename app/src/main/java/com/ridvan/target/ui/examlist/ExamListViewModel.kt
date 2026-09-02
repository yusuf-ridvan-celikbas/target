package com.ridvan.target.ui.examlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamWithProgress
import com.ridvan.target.data.local.entity.Exam
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamListViewModel(application: Application) : AndroidViewModel(application) {
    private val examDao: ExamDao = (application as TargetApplication).database.examDao()

    val exams: StateFlow<List<ExamWithProgress>> = examDao.getAllWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addExam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { examDao.insert(Exam(name = trimmed)) }
    }
}
