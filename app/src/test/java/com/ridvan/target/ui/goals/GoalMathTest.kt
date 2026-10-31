package com.ridvan.target.ui.goals

import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalKind
import com.ridvan.target.data.local.entity.GoalLog
import com.ridvan.target.data.local.entity.GoalMilestone
import com.ridvan.target.data.local.entity.GoalSource
import com.ridvan.target.data.local.entity.HabitPeriod
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.Sport
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMathTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val day = 24L * 60 * 60 * 1000

    // 1970-01-05 is a Monday (day 4).
    private val monday = 4 * day

    private fun goal(kind: GoalKind, target: Double? = null, start: Double? = null, deadline: Long? = null, period: HabitPeriod? = null, source: GoalSource? = null) =
        Goal(id = 1, userId = 1, name = "g", kind = kind, targetValue = target, startValue = start, period = period, source = source, startDate = monday, deadline = deadline)

    private fun log(dayOffset: Int, amount: Double, createdAt: Long = 0) = GoalLog(goalId = 1, date = monday + dayOffset * day, amount = amount, createdAt = createdAt)

    @Test
    fun targetSumsLogsAndReaches() {
        val g = goal(GoalKind.TARGET, target = 10.0)
        val p = goalProgress(g, listOf(log(0, 4.0), log(1, 3.5)), emptyList(), GoalSourceData(), monday + day, utc)
        assertEquals(7.5, p.current, 0.0)
        assertEquals(0.75f, p.fraction, 0.001f)
        assertFalse(p.reached)
        assertTrue(goalProgress(g, listOf(log(0, 12.0)), emptyList(), GoalSourceData(), monday, utc).reached)
    }

    @Test
    fun autoSourceCountsFromStartDate() {
        val g = goal(GoalKind.TARGET, target = 10.0, source = GoalSource.WORKOUT_KM)
        val workouts = listOf(
            Workout(userId = 1, sport = Sport.RUNNING, startedAt = monday - day, durationSeconds = 600, distanceMeters = 5000),
            Workout(userId = 1, sport = Sport.RUNNING, startedAt = monday + day, durationSeconds = 600, distanceMeters = 3500),
        )
        assertEquals(3.5, goalProgress(g, emptyList(), emptyList(), GoalSourceData(workouts = workouts), monday + day, utc).current, 0.001)
    }

    @Test
    fun measureGoesEitherDirection() {
        val down = goal(GoalKind.MEASURE, target = 75.0, start = 85.0)
        val p = goalProgress(down, listOf(log(0, 82.0, 1), log(2, 80.0, 2)), emptyList(), GoalSourceData(), monday, utc)
        assertEquals(80.0, p.current, 0.0)
        assertEquals(0.5f, p.fraction, 0.001f)
        assertEquals(0f, goalProgress(down, emptyList(), emptyList(), GoalSourceData(), monday, utc).fraction, 0f)
    }

    @Test
    fun milestonesCountDoneSteps() {
        val steps = listOf(GoalMilestone(goalId = 1, name = "a", orderIndex = 0, doneAt = 5), GoalMilestone(goalId = 1, name = "b", orderIndex = 1))
        val p = goalProgress(goal(GoalKind.MILESTONES), emptyList(), steps, GoalSourceData(), monday, utc)
        assertEquals(1.0, p.current, 0.0)
        assertEquals(0.5f, p.fraction, 0.001f)
    }

    @Test
    fun habitWeekCountAndStreak() {
        val g = goal(GoalKind.HABIT, target = 2.0, period = HabitPeriod.WEEK)
        // Two the week before, two last week, one this week (week of monday + 14 days).
        val logs = listOf(log(0, 1.0), log(2, 1.0), log(7, 1.0), log(9, 1.0), log(14, 1.0))
        val now = monday + 15 * day
        assertEquals(1, habitCountInPeriod(g, logs, now, utc))
        assertEquals(2, habitStreak(g, logs, now, utc))
        assertEquals(3, habitStreak(g, logs + log(15, 1.0), now, utc))
    }

    @Test
    fun paceNeedsPerDayAndOnTrack() {
        // 10 days (monday .. monday+9), target 100; on day 5 (elapsed 5/10) with 40 done -> behind, 60 left over 5 days.
        val g = goal(GoalKind.TARGET, target = 100.0, deadline = monday + 9 * day)
        val now = monday + 5 * day
        val pace = goalPace(g, goalProgress(g, listOf(log(0, 40.0)), emptyList(), GoalSourceData(), now, utc), now, utc)!!
        assertEquals(5, pace.daysLeft)
        assertEquals(12.0, pace.needPerDay!!, 0.001)
        assertFalse(pace.onTrack)
        val overdue = goalPace(g, goalProgress(g, emptyList(), emptyList(), GoalSourceData(), monday + 12 * day, utc), monday + 12 * day, utc)!!
        assertTrue(overdue.overdue)
        assertNull(goalPace(goal(GoalKind.TARGET, target = 1.0), goalProgress(goal(GoalKind.TARGET, target = 1.0), emptyList(), emptyList(), GoalSourceData(), now, utc), now, utc))
    }
}
