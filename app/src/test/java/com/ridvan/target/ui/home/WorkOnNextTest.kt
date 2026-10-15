package com.ridvan.target.ui.home

import com.ridvan.target.data.local.dao.WorkOnNextRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkOnNextTest {
    private fun row(id: Long, target: Int = 0, logged: Int = 0, correct: Int = 0, wrong: Int = 0, topic: Long = id) =
        WorkOnNextRow(id, topic, "T$topic", "Math", null, target, logged, correct, wrong)

    @Test
    fun weakestAccuracyFirstThenMostRemaining() {
        val result = rankWorkOnNext(
            listOf(
                row(1, correct = 6, wrong = 6), // 50%
                row(2, correct = 8, wrong = 4), // 66%
                row(3, correct = 19, wrong = 1), // 95%, not weak
                row(4, target = 200, logged = 80), // 120 left
                row(5, target = 50, logged = 10), // 40 left
            ),
        )
        assertEquals(listOf(1L, 2L, 4L), result.map { it.topicId })
        assertEquals(50, result[0].accuracyPercent)
        assertNull(result[2].accuracyPercent)
        assertEquals(120, result[2].questionsLeft)
    }

    @Test
    fun fewAnswersAreNotRankedByAccuracyAndEmptyIsEmpty() {
        assertEquals(emptyList<WorkOnNextItem>(), rankWorkOnNext(emptyList()))
        val r = rankWorkOnNext(listOf(row(1, target = 10, logged = 5, correct = 0, wrong = 4)))
        assertNull(r.single().accuracyPercent)
    }

    @Test
    fun topicInTwoBanksIsMergedWithoutDirectProgressRoute() {
        val r = rankWorkOnNext(listOf(row(1, topic = 7, correct = 3, wrong = 3), row(2, topic = 7, correct = 3, wrong = 3)))
        assertEquals(1, r.size)
        assertNull(r[0].studyResourceTopicId)
        assertEquals(50, r[0].accuracyPercent)
    }
}
