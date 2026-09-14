package com.ridvan.target.ui.topicprogress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.local.dao.StudyResourceTopicWithTopic
import com.ridvan.target.data.local.entity.PracticeLog
import com.ridvan.target.ui.navigation.TopicProgressRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicProgressViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val studyResourceTopicId: Long = savedStateHandle.toRoute<TopicProgressRoute>().studyResourceTopicId
    private val database = (application as TargetApplication).database
    private val studyResourceTopicDao = database.studyResourceTopicDao()
    private val practiceLogDao = database.practiceLogDao()

    val attached: StateFlow<StudyResourceTopicWithTopic?> = studyResourceTopicDao.getById(studyResourceTopicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sessions: StateFlow<List<PracticeLog>> = practiceLogDao.getByStudyResourceTopicId(studyResourceTopicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateTargetCounts(testCount: Int, questionCount: Int) {
        viewModelScope.launch {
            val current = attached.value?.studyResourceTopic ?: return@launch
            studyResourceTopicDao.update(current.copy(testCount = testCount, questionCount = questionCount))
        }
    }

    fun detachTopic(onDetached: () -> Unit) {
        viewModelScope.launch {
            attached.value?.studyResourceTopic?.let { studyResourceTopicDao.delete(it) }
            onDetached()
        }
    }

    fun logSession(testsSolved: Int, questionCount: Int, solvedCount: Int, unsolvedCount: Int, durationMinutes: Int) {
        viewModelScope.launch {
            practiceLogDao.insert(
                PracticeLog(
                    studyResourceTopicId = studyResourceTopicId,
                    testsSolved = testsSolved,
                    questionCount = questionCount,
                    solvedCount = solvedCount,
                    unsolvedCount = unsolvedCount,
                    durationMinutes = durationMinutes,
                )
            )
        }
    }

    fun updateSession(log: PracticeLog, testsSolved: Int, questionCount: Int, solvedCount: Int, unsolvedCount: Int, durationMinutes: Int) {
        viewModelScope.launch {
            practiceLogDao.update(
                log.copy(
                    testsSolved = testsSolved,
                    questionCount = questionCount,
                    solvedCount = solvedCount,
                    unsolvedCount = unsolvedCount,
                    durationMinutes = durationMinutes,
                )
            )
        }
    }

    fun deleteSession(log: PracticeLog) {
        viewModelScope.launch { practiceLogDao.delete(log) }
    }
}
