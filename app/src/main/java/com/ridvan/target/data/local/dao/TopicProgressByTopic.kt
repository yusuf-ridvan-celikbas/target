package com.ridvan.target.data.local.dao

data class TopicProgressByTopic(
    val topicId: Long,
    val totalTestsSolved: Int,
    val totalSolved: Int,
    val totalUnsolved: Int,
    val totalDurationMinutes: Int,
)
