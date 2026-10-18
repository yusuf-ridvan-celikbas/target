package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** The running countdown of a combined mock exam (deneme). */
@Serializable
data class MockExamTimerRoute(val mockExamId: Long)

/** One combined mock exam: results per course, topics, time used. */
@Serializable
data class MockExamDetailRoute(val mockExamId: Long)

/** Per-question marking (result + topic) of one deneme part. */
@Serializable
data class MockExamQuestionsRoute(val partId: Long)

/** A deneme opened from Statistics (its own route so the Stats tab keeps its own back stack). */
@Serializable
data class StatsMockExamDetailRoute(val mockExamId: Long)
