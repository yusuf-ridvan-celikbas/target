package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.QuestionBankTopicStat

data class QuestionBankTopicStatWithTopic(
    @Embedded val stat: QuestionBankTopicStat,
    val topicName: String,
)
