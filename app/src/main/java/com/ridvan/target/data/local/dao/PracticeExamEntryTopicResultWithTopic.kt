package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PracticeExamEntryTopicResult

data class PracticeExamEntryTopicResultWithTopic(
    @Embedded val topicResult: PracticeExamEntryTopicResult,
    val topicName: String,
)
