package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.QuestionBankTopicStat

data class QuestionBankTopicStatProgress(
    @Embedded val stat: QuestionBankTopicStat,
    val topicName: String,
    val questionBankName: String,
    val testsCompleted: Int,
    val questionsCompleted: Int,
) {
    val remainingTests: Int
        get() = (stat.testCount ?: 0) - testsCompleted

    val remainingQuestions: Int
        get() = (stat.questionCount ?: 0) - questionsCompleted
}
