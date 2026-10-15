package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** Every routine — a page of the Plan tab. */
@Serializable
object RoutineListRoute

/** One routine: its details, steps and the checklist for a day ([dateMillis] = that day's start; 0 = today). */
@Serializable
data class RoutineDetailRoute(val routineId: Long, val dateMillis: Long = 0)
