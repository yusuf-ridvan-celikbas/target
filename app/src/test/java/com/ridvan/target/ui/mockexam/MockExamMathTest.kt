package com.ridvan.target.ui.mockexam

import org.junit.Assert.assertEquals
import org.junit.Test

class MockExamMathTest {
    @Test
    fun netSubtractsAQuarterOfWrong() {
        assertEquals(31.0, mockExamNet(33, 8), 1e-9)
    }

    @Test
    fun scoresMatchTheReferenceCalculator() {
        // ales-puan.hesaplama.net, term 2026-2
        assertEquals(73.23287, alesScore(AlesScoreType.QUANTITATIVE, 22.0, 31.0), 1e-3)
        assertEquals(77.47453, alesScore(AlesScoreType.VERBAL, 22.0, 31.0), 1e-3)
        assertEquals(75.95478, alesScore(AlesScoreType.EQUAL_WEIGHT, 22.0, 31.0), 1e-3)
        assertEquals(68.12782, alesScore(AlesScoreType.QUANTITATIVE, 20.0, 20.0), 1e-3)
        assertEquals(75.81166, alesScore(AlesScoreType.VERBAL, 20.0, 30.0), 1e-3)
    }

    @Test
    fun scoreIsCappedAt100() {
        assertEquals(100.0, alesScore(AlesScoreType.EQUAL_WEIGHT, 50.0, 50.0), 1e-9)
    }
}
