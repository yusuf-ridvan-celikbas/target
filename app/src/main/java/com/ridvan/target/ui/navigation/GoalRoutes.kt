package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** Plan tab → Goals: every goal with its progress. */
@Serializable
object GoalsRoute

/** One goal: progress, pace, and its logs, readings or steps. */
@Serializable
data class GoalDetailRoute(val goalId: Long)
