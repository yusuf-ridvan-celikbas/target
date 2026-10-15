package com.ridvan.target.ui.navigation

import kotlinx.serialization.Serializable

/** Sports: summary, goals, personal bests and recent workouts — a page of the Focus tab. */
@Serializable
object SportsRoute

/** The live workout stopwatch. */
@Serializable
object LiveWorkoutRoute

@Serializable
data class WorkoutDetailRoute(val workoutId: Long)

/** Every logged workout by day, with a month switcher and sport filter. */
@Serializable
object SportsHistoryRoute
