package com.ridvan.target.data.local.dao

data class PracticeExamTopicAggregate(
    val topicId: Long,
    val topicName: String,
    val courseId: Long,
    val courseName: String,
    val totalQuestionCount: Int,
    val totalCorrectCount: Int,
    val totalWrongCount: Int,
)
