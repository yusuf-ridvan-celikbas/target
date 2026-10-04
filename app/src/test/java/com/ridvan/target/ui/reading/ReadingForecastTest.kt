package com.ridvan.target.ui.reading

import com.ridvan.target.data.local.entity.Book
import com.ridvan.target.data.local.entity.FocusSession
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingForecastTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, dayOfMonth)
    }.timeInMillis

    private val today = day(2026, 10, 10)

    private fun session(start: Long, startPage: Int, endPage: Int?, minutes: Int = 30, bookId: Long = 1) = FocusSession(
        presetName = "Reading",
        workMinutes = 30,
        breakMinutes = 0,
        startedAt = start + 9 * 60 * 60 * 1000,
        endedAt = start + 9 * 60 * 60 * 1000 + minutes * 60_000L,
        cyclesCompleted = 1,
        totalWorkMinutes = minutes,
        totalBreakMinutes = 0,
        bookId = bookId,
        startPage = startPage,
        endPage = endPage,
    )

    @Test
    fun `finish date counts today as a reading day`() {
        assertEquals(today, finishDate(10, 10.0, today))
        assertEquals(addDays(today, 2), finishDate(25, 10.0, today))
        assertEquals(today, finishDate(0, 10.0, today))
    }

    @Test
    fun `pages per day needed includes today and the goal day`() {
        assertEquals(10, pagesPerDayNeeded(100, addDays(today, 9), today))
        assertEquals(100, pagesPerDayNeeded(100, today, today))
        assertEquals(34, pagesPerDayNeeded(100, addDays(today, 2), today))
        assertNull(pagesPerDayNeeded(100, addDays(today, -1), today))
    }

    @Test
    fun `minutes come from the speed and need one`() {
        assertEquals(30, minutesFor(15, 30.0))
        assertEquals(1, minutesFor(1, 120.0))
        assertNull(minutesFor(15, null))
    }

    @Test
    fun `pace averages over the days since the book was started`() {
        // Started 4 days ago (4 days ago .. today = 5 days), 50 pages so far.
        val sessions = listOf(
            session(addDays(today, -4), 0, 30),
            session(addDays(today, -1), 30, 50),
        )
        val pace = readingPace(sessions, sessions, today)
        assertEquals(10.0, pace.pagesPerDay!!, 0.001)
    }

    @Test
    fun `pace only looks at the recent window`() {
        val sessions = listOf(
            session(addDays(today, -40), 0, 200),
            session(addDays(today, -3), 200, 228),
        )
        val pace = readingPace(sessions, sessions, today)
        assertEquals(2.0, pace.pagesPerDay!!, 0.001)
    }

    @Test
    fun `pace falls back to all reading when the book has none recently`() {
        val other = listOf(session(addDays(today, -13), 0, 140, bookId = 2))
        val pace = readingPace(emptyList(), other, today)
        assertEquals(10.0, pace.pagesPerDay!!, 0.001)
        assertNull(readingPace(emptyList(), emptyList(), today).pagesPerDay)
    }

    @Test
    fun `speed uses the book once it has enough minutes`() {
        val book = listOf(session(addDays(today, -1), 0, 40, minutes = 60))
        val all = book + session(addDays(today, -2), 0, 10, minutes = 60, bookId = 2)
        assertEquals(40.0, readingPace(book, all, today).pagesPerHour!!, 0.001)
        val shortBook = listOf(session(addDays(today, -1), 0, 10, minutes = 10))
        // Too little time on this book, so the overall speed: 20 pages in 70 minutes.
        assertEquals(20 * 60 / 70.0, readingPace(shortBook, shortBook + all.last(), today).pagesPerHour!!, 0.001)
    }

    @Test
    fun `daily goal is fixed from the start of today and counts today's pages`() {
        val book = Book(id = 1, title = "B", totalPages = 200, goalDate = addDays(today, 9))
        val sessions = listOf(
            session(addDays(today, -2), 0, 100),
            session(today, 100, 104),
        )
        val goal = dailyGoal(book, sessions, today)!!
        // 100 pages left at the start of today over 10 days.
        assertEquals(10, goal.target)
        assertEquals(4, goal.readToday)
        assertEquals(6, goal.remainingToday)
        assertFalse(goal.done)
    }

    @Test
    fun `daily goal is done once today's target is read, and passed after the date`() {
        val book = Book(id = 1, title = "B", totalPages = 100, goalDate = addDays(today, 4))
        val done = dailyGoal(book, listOf(session(today, 0, 20)), today)!!
        assertTrue(done.done)
        val late = dailyGoal(book.copy(goalDate = addDays(today, -1)), emptyList(), today)!!
        assertTrue(late.passed)
        assertNull(dailyGoal(book.copy(goalDate = null), emptyList(), today))
        assertNull(dailyGoal(book.copy(isFinished = true), emptyList(), today))
    }

    @Test
    fun `rates drop the trailing zero for whole numbers`() {
        val saved = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.US)
        try {
            assertEquals("30", formatRate(30.0))
            assertEquals("30", formatRate(29.96))
            assertEquals("18.3", formatRate(18.33))
            assertEquals("0.5", formatRate(0.5))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }
}
