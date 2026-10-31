package com.ridvan.target.ui.common

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun dayStreakStaysAliveUntilTodayEnds() {
        assertEquals(Streak(3, 3, true), dayStreak(setOf(8, 9, 10), today = 10))
        assertEquals(Streak(2, 2, false), dayStreak(setOf(8, 9), today = 10))
        assertEquals(Streak(0, 2, false), dayStreak(setOf(7, 8), today = 10))
        assertEquals(Streak(1, 4, true), dayStreak(setOf(1, 2, 3, 4, 10), today = 10))
        assertEquals(Streak.NONE, dayStreak(emptySet(), today = 10))
    }

    @Test
    fun scheduledStreakSkipsUnfinishedToday() {
        assertEquals(Streak(2, 2, false), scheduledStreak(listOf(false, true, true, false), lastIsToday = true))
        assertEquals(Streak(0, 2, false), scheduledStreak(listOf(true, true, false), lastIsToday = false))
        assertEquals(Streak(3, 3, true), scheduledStreak(listOf(true, true, true), lastIsToday = true))
        assertEquals(Streak.NONE, scheduledStreak(emptyList(), lastIsToday = false))
    }

    @Test
    fun studyDaysApplyTheDailyMinimum() {
        val entries = listOf(0L to 10, 3_600_000L to 25, day to 20, 2 * day to 0)
        assertEquals(setOf(0L, 1L, 2L), studyDays(entries, minMinutes = 0, zone = utc))
        assertEquals(setOf(0L), studyDays(entries, minMinutes = 30, zone = utc))
    }
}
