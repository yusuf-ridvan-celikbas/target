package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ExamListRoute

@Serializable
data class ExamDetailRoute(val examId: Long)

@Serializable
data class TopicDetailRoute(val topicId: Long)
