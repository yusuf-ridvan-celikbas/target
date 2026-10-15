package com.ridvan.target.ui.routines

import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineLogicTest {
    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month - 1, dayOfMonth) }.timeInMillis

    @Test fun parsesDaysIgnoringJunk() {
        assertEquals(setOf(1, 3, 7), parseRepeatDays("1, 3,7,9,x"))
        assertEquals("1,3,7", repeatDaysToString(setOf(7, 1, 3)))
    }

    @Test fun isoDayMondayToSunday() {
        assertEquals(4, isoDayOf(day(2026, 10, 15))) // a Thursday
        assertEquals(1, isoDayOf(day(2026, 10, 12))) // Monday
        assertEquals(7, isoDayOf(day(2026, 10, 18))) // Sunday
    }

    @Test fun runsOnRespectsDaysActiveAndCreation() {
        val created = day(2026, 10, 15)
        val routine = Routine(userId = 1, name = "r", repeatDays = "4,5", createdAt = created)
        assertTrue(routine.runsOn(day(2026, 10, 15)))
        assertFalse(routine.runsOn(day(2026, 10, 16 - 3))) // Monday, not a repeat day
        assertFalse(routine.runsOn(day(2026, 10, 8))) // Thursday before it existed
        assertFalse(routine.copy(isActive = false).runsOn(day(2026, 10, 15)))
    }

    @Test fun progressCountsOnlyThatDayAndRoutine() {
        val steps = listOf(RoutineStep(id = 1, routineId = 1, name = "a"), RoutineStep(id = 2, routineId = 1, name = "b"))
        val today = day(2026, 10, 15)
        val completions = listOf(
            RoutineStepCompletion(routineStepId = 1, date = today),
            RoutineStepCompletion(routineStepId = 2, date = today - 86_400_000L),
            RoutineStepCompletion(routineStepId = 9, date = today),
        )
        assertEquals(RoutineProgress(1, 2), routineProgress(steps, completions, today))
    }
}
