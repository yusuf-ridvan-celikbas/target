package com.ridvan.target.ui.mockexam

import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.ScoringSystem
import com.ridvan.target.data.local.entity.ScoringTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockExamMathTest {
    private fun part(test: ScoringTest?, correct: Int, wrong: Int) =
        MockExamPart(mockExamId = 1, courseId = null, questionCount = 50, correctCount = correct, wrongCount = wrong, scoringTest = test)

    @Test
    fun netSubtractsAQuarterOfWrong() {
        assertEquals(31.0, mockExamNet(33, 8), 1e-9)
    }

    @Test
    fun netsSumPerTestAndSkipUncounted() {
        val nets = netsByTest(
            listOf(part(ScoringTest.ALES_VERBAL, 20, 4), part(ScoringTest.ALES_VERBAL, 10, 0), part(null, 40, 0))
        )
        assertEquals(mapOf(ScoringTest.ALES_VERBAL to 29.0), nets)
    }

    @Test
    fun alesScores() {
        val nets = mapOf(ScoringTest.ALES_QUANTITATIVE to 22.0, ScoringTest.ALES_VERBAL to 31.0)
        assertEquals(73.23, score(ScoreType.ALES_SAY, nets), 0.01)
        assertEquals(77.47, score(ScoreType.ALES_SOZ, nets), 0.01)
        assertEquals(75.95, score(ScoreType.ALES_EA, nets), 0.01)
        val full = mapOf(ScoringTest.ALES_QUANTITATIVE to 50.0, ScoringTest.ALES_VERBAL to 50.0)
        assertEquals(100.0, score(ScoreType.ALES_EA, full), 1e-9)
    }

    @Test
    fun tytIs100AtZeroAnd500AtFullMarks() {
        assertEquals(100.0, score(ScoreType.TYT, emptyMap()), 1e-9)
        val full = ScoringSystem.TYT.tests().associateWith { it.questionCount.toDouble() }
        assertEquals(500.0, score(ScoreType.TYT, full), 1e-9)
    }

    @Test
    fun yksTypesReach500WithFullTytAndTheirAytTests() {
        val all = ScoringTest.entries.filter { it.system != ScoringSystem.ALES }.associateWith { it.questionCount.toDouble() }
        ScoringSystem.AYT.scoreTypes().forEach { type ->
            val own = all.filterKeys { it.system == ScoringSystem.TYT || it in type.weights }
            assertEquals(type.name, 500.0, score(type, own), 1e-6)
        }
    }

    @Test
    fun guessesFromCategoryAndName() {
        assertEquals(ScoringTest.ALES_VERBAL, guessScoringTest(ScoringSystem.ALES, "Turkish", CourseCategory.VERBAL))
        assertNull(guessScoringTest(ScoringSystem.ALES, "Turkish", null))
        assertEquals(ScoringTest.TYT_TURKISH, guessScoringTest(ScoringSystem.TYT, "Türkçe", null))
        assertEquals(ScoringTest.AYT_HISTORY_2, guessScoringTest(ScoringSystem.AYT, "Tarih-2", null))
        assertEquals(ScoringTest.AYT_PHYSICS, guessScoringTest(ScoringSystem.AYT, "Physics", null))
    }
}
