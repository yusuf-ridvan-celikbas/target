package com.ridvan.target.ui.sports

import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.Workout
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SportsHistoryTest {
    private val zone = TimeZone.getTimeZone("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        Calendar.getInstance(zone).apply { clear(); set(year, month - 1, day, hour, 0, 0) }.timeInMillis

    private fun workout(id: Long, start: Long, sport: Sport = Sport.RUNNING) =
        Workout(id = id, userId = 1, sport = sport, startedAt = start, durationSeconds = 1800, distanceMeters = null, notes = null, effort = null, isLive = false)

    @Test fun monthFilterIncludesBoundariesAndSortsNewestFirst() {
        val month = monthStartOf(at(2026, 10, 15), zone)
        val list = listOf(
            workout(1, at(2026, 9, 30, 23)),
            workout(2, at(2026, 10, 1, 0)),
            workout(3, at(2026, 10, 31, 23)),
            workout(4, at(2026, 11, 1, 0)),
        )
        assertEquals(listOf(3L, 2L), historyFor(list, month, null, zone).map { it.id })
    }

    @Test fun sportFilterAndShiftMonth() {
        val month = monthStartOf(at(2026, 1, 20), zone)
        assertEquals(at(2025, 12, 1, 0), shiftMonth(month, -1, zone))
        val list = listOf(workout(1, at(2026, 1, 5), Sport.YOGA), workout(2, at(2026, 1, 6), Sport.RUNNING))
        assertEquals(listOf(1L), historyFor(list, month, Sport.YOGA, zone).map { it.id })
    }

    @Test fun groupsByDayNewestFirst() {
        val groups = groupByDay(listOf(workout(1, at(2026, 10, 14, 8)), workout(2, at(2026, 10, 15, 7)), workout(3, at(2026, 10, 15, 18))), zone)
        assertEquals(listOf(at(2026, 10, 15, 0), at(2026, 10, 14, 0)), groups.map { it.first })
        assertEquals(listOf(3L, 2L), groups[0].second.map { it.id })
    }
}
