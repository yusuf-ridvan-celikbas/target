package com.ridvan.target.ui.examlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.ExamDao
import com.ridvan.target.data.local.dao.ExamWithType
import com.ridvan.target.data.local.dao.ExamTypeDao
import com.ridvan.target.data.local.entity.Exam
import com.ridvan.target.data.local.entity.ExamType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamListViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val database = targetApplication.database
    private val examDao: ExamDao = database.examDao()
    private val examTypeDao: ExamTypeDao = database.examTypeDao()
    private val userId = targetApplication.preferences.currentUserId

    val exams: StateFlow<List<ExamWithType>> =
        (userId?.let { examDao.getAllWithTypeByUserId(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val examTypes: StateFlow<List<ExamType>> = examTypeDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addExam(name: String, examTypeId: Long, hasSections: Boolean, examDate: Long?, studyStartDate: Long?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || userId == null) return
        viewModelScope.launch {
            examDao.insert(
                Exam(
                    name = trimmed,
                    examTypeId = examTypeId,
                    userId = userId,
                    hasSections = hasSections,
                    examDate = if (hasSections) null else examDate,
                    studyStartDate = studyStartDate,
                ),
            )
        }
    }
}
