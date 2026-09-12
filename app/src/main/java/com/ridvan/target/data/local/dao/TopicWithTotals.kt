package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.Topic

data class TopicWithTotals(
    @Embedded val topic: Topic,
    val totalTestCount: Int,
    val totalQuestionCount: Int,
)
