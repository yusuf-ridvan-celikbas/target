package com.ridvan.target.ui.sports

import com.ridvan.target.data.local.dao.LiftRow
import com.ridvan.target.data.local.entity.GoalPeriod
import com.ridvan.target.data.local.entity.GoalTargetType
import com.ridvan.target.data.local.entity.Sport
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutSet
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SportsMathTest {
    private val zone = TimeZone.getTimeZone("UTC")

    @Before fun fixLocale() = Locale.setDefault(Locale.US)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        Calendar.getInstance(zone).apply { clear(); set(year, month - 1, day, hour, 0, 0) }.timeInMillis

    private fun workout(
        sport: Sport = Sport.RUNNING, start: Long = at(2026, 10, 14), seconds: Int = 1800, meters: Int? = null, id: Long = 0,
    ) = Workout(id = id, userId = 1, sport = sport, startedAt = start, durationSeconds = seconds, distanceMeters = meters)

    @Test fun paceIsPerKmOrPer100mForSwimming() {
        assertEquals(300.0, paceSeconds(Sport.RUNNING, 1500, 5000)!!, 0.001)
        assertEquals(120.0, paceSeconds(Sport.SWIMMING, 1200, 1000)!!, 0.001)
        assertNull(paceSeconds(Sport.RUNNING, 1500, null))
        assertNull(paceSeconds(Sport.RUNNING, 1500, 0))
        assertEquals("5:00", formatClock(300.0))
        assertEquals("5:30", formatClock(329.6))
        assertEquals("1:05:09", formatHms(3909))
        assertEquals("42:10", formatHms(2530))
    }

    @Test fun formatsAndParsesNumbers() {
        assertEquals("5", formatKm(5000))
        assertEquals("10.25", formatKm(10250))
        assertEquals("0.75", formatKm(750))
        assertEquals(5.2, parseDecimal("5,2")!!, 0.0)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("0"))
        assertEquals("62.5", formatWeight(62.5))
        assertEquals("60", formatWeight(60.0))
    }

    @Test fun totalsPerSportSortedByTime() {
        val list = listOf(
            workout(Sport.RUNNING, seconds = 1800, meters = 5000),
            workout(Sport.RUNNING, seconds = 1800, meters = 4000),
            workout(Sport.GYM, seconds = 1200),
        )
        val totals = totalsBySport(list)
        assertEquals(Sport.RUNNING, totals[0].first)
        assertEquals(SportTotals(2, 3600, 9000), totals[0].second)
        assertEquals(60, totals[0].second.minutes)
        assertEquals(Sport.GYM, totals[1].first)
        assertEquals(SportTotals(3, 4800, 9000), totalsOf(list))
    }

    @Test fun weekStartsOnMondayAndMonthOnFirst() {
        val wed = at(2026, 10, 14)
        assertEquals(at(2026, 10, 12, 0), periodStart(SportsPeriod.WEEK, wed, zone))
        assertEquals(at(2026, 10, 12, 0), periodStart(SportsPeriod.WEEK, at(2026, 10, 18, 23), zone))
        assertEquals(at(2026, 10, 12, 0), periodStart(SportsPeriod.WEEK, at(2026, 10, 12, 0), zone))
        assertEquals(at(2026, 10, 1, 0), periodStart(SportsPeriod.MONTH, wed, zone))
        assertEquals(at(2026, 10, 19, 0), periodEnd(SportsPeriod.WEEK, at(2026, 10, 12, 0), zone))
        assertEquals(at(2026, 11, 1, 0), periodEnd(SportsPeriod.MONTH, at(2026, 10, 1, 0), zone))
    }

    @Test fun dailyMinutesBucketsByDay() {
        val start = at(2026, 10, 12, 0)
        val list = listOf(
            workout(start = at(2026, 10, 12, 7), seconds = 1800),
            workout(start = at(2026, 10, 12, 18), seconds = 600),
            workout(start = at(2026, 10, 14), seconds = 3600),
            workout(start = at(2026, 10, 20), seconds = 3600), // next week: excluded
        )
        val minutes = dailyMinutes(list, start, periodEnd(SportsPeriod.WEEK, start, zone), zone)
        assertEquals(listOf(40, 0, 60, 0, 0, 0, 0), minutes)
    }

    @Test fun goalProgressByTypeSportAndPeriod() {
        val now = at(2026, 10, 14)
        val list = listOf(
            workout(Sport.RUNNING, start = at(2026, 10, 12), seconds = 1800, meters = 6000),
            workout(Sport.RUNNING, start = at(2026, 10, 13), seconds = 2400, meters = 6000),
            workout(Sport.CYCLING, start = at(2026, 10, 13), seconds = 3600, meters = 20000),
            workout(Sport.RUNNING, start = at(2026, 10, 5), seconds = 1800, meters = 5000), // earlier week, same month
        )
        fun goal(sport: Sport?, period: GoalPeriod, type: GoalTargetType, value: Double) = SportGoal(1, 1, sport, period, type, value)

        val runKm = goalProgress(goal(Sport.RUNNING, GoalPeriod.WEEK, GoalTargetType.DISTANCE_KM, 20.0), list, now, zone)
        assertEquals(12.0, runKm.current, 0.001)
        assertEquals(0.6f, runKm.fraction, 0.001f)
        assertFalse(runKm.reached)

        val anyMinutes = goalProgress(goal(null, GoalPeriod.WEEK, GoalTargetType.MINUTES, 100.0), list, now, zone)
        assertEquals(130.0, anyMinutes.current, 0.001)
        assertEquals(1f, anyMinutes.fraction, 0f)
        assertTrue(anyMinutes.reached)

        val monthCount = goalProgress(goal(Sport.RUNNING, GoalPeriod.MONTH, GoalTargetType.WORKOUTS, 5.0), list, now, zone)
        assertEquals(3.0, monthCount.current, 0.0)
    }

    @Test fun dayStreakCountsBackFromTodayOrYesterday() {
        val now = at(2026, 10, 14)
        val days = listOf(14, 13, 12, 10).map { workout(start = at(2026, 10, it)) }
        assertEquals(3, streaks(days, now, zone).days)
        // Nothing yet today: the streak from yesterday is still alive.
        assertEquals(2, streaks(days.drop(1), now, zone).days)
        // Two days of nothing: lost.
        assertEquals(0, streaks(days.drop(2), now, zone).days)
        assertEquals(0, streaks(emptyList(), now, zone).days)
        // Several workouts on one day count once.
        assertEquals(1, streaks(listOf(workout(start = at(2026, 10, 14, 7)), workout(start = at(2026, 10, 14, 19))), now, zone).days)
    }

    @Test fun weekStreakCountsConsecutiveWeeks() {
        val now = at(2026, 10, 14) // Wednesday of the week starting Mon 12 Oct
        val list = listOf(at(2026, 10, 13), at(2026, 10, 7), at(2026, 9, 30), at(2026, 9, 14)).map { workout(start = it) }
        assertEquals(3, streaks(list, now, zone).weeks) // this, last, the one before; 14 Sep leaves a gap
        // Sunday 11 Oct and Monday 12 Oct are different weeks.
        assertEquals(2, streaks(listOf(workout(start = at(2026, 10, 11)), workout(start = at(2026, 10, 12))), now, zone).weeks)
    }

    @Test fun fastestFiveKUsesRunsOfFiveKmOrMore() {
        val slowLong = workout(id = 1, seconds = 3000, meters = 10000) // 5:00/km -> 25:00
        val fastLong = workout(id = 2, seconds = 2700, meters = 10000) // 4:30/km -> 22:30
        val fastShort = workout(id = 3, seconds = 600, meters = 3000) // too short: ignored
        val ride = workout(Sport.CYCLING, id = 4, seconds = 1200, meters = 10000) // not a run
        val best = fastestFiveK(listOf(slowLong, fastLong, fastShort, ride))!!
        assertEquals(2L, best.first.id)
        assertEquals(1350.0, best.second, 0.001)
        assertNull(fastestFiveK(listOf(fastShort, ride)))
    }

    @Test fun longestDistancePerSport() {
        val list = listOf(
            workout(Sport.RUNNING, id = 1, meters = 5000),
            workout(Sport.RUNNING, id = 2, meters = 12000),
            workout(Sport.CYCLING, id = 3, meters = 40000),
            workout(Sport.SWIMMING, id = 4),
        )
        assertEquals(2L, longestDistance(list, Sport.RUNNING)!!.id)
        assertEquals(3L, longestDistance(list, Sport.CYCLING)!!.id)
        assertNull(longestDistance(list, Sport.SWIMMING))
    }

    @Test fun heaviestLiftPerExerciseName() {
        val rows = listOf(
            LiftRow("Bench press", 10, 60.0, 1, 1),
            LiftRow("bench press ", 5, 80.0, 2, 2),
            LiftRow("Bench press", 8, 80.0, 3, 3),
            LiftRow("Squat", 5, 100.0, 4, 4),
            LiftRow("Pull-up", 10, 0.0, 5, 5), // bodyweight: ignored
        )
        val best = heaviestLifts(rows)
        assertEquals(listOf("squat", "bench press"), best.map { it.name.lowercase() })
        val bench = best.first { it.name.lowercase() == "bench press" }
        assertEquals(80.0, bench.weightKg, 0.0)
        assertEquals(8, bench.reps) // the tie goes to more reps
        assertNotNull(best.firstOrNull())
    }

    @Test fun identicalSetsCollapse() {
        fun set(reps: Int, kg: Double) = WorkoutSet(exerciseId = 1, reps = reps, weightKg = kg)
        val groups = groupSets(listOf(set(10, 60.0), set(10, 60.0), set(10, 60.0), set(8, 62.5)))
        assertEquals(listOf(SetGroup(3, 10, 60.0), SetGroup(1, 8, 62.5)), groups)
        assertEquals(emptyList<SetGroup>(), groupSets(emptyList()))
    }
}
