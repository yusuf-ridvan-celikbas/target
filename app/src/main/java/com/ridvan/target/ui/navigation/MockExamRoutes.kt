package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** The running countdown of a combined mock exam (deneme). */
@Serializable
data class MockExamTimerRoute(val mockExamId: Long)

/** One combined mock exam: results per course, topics, time used. */
@Serializable
data class MockExamDetailRoute(val mockExamId: Long)
