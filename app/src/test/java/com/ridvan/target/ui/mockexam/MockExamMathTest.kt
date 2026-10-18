package com.ridvan.target.ui.mockexam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockExamMathTest {
    @Test
    fun netSubtractsAQuarterOfWrong() {
        assertEquals(37.0, mockExamNet(40, 12), 1e-9)
    }

    @Test
    fun weightedNetsFollowScoreTypeWeights() {
        val q = 40.0
        val v = 20.0
        assertEquals(35.0, alesWeightedNet(AlesScoreType.QUANTITATIVE, q, v), 1e-9)
        assertEquals(25.0, alesWeightedNet(AlesScoreType.VERBAL, q, v), 1e-9)
        assertEquals(30.0, alesWeightedNet(AlesScoreType.EQUAL_WEIGHT, q, v), 1e-9)
    }

    @Test
    fun estimateIsClampedTo50To100() {
        assertEquals(80.0, alesEstimate(50.0, 1.0, 30.0), 1e-9)
        assertEquals(100.0, alesEstimate(50.0, 2.0, 30.0), 1e-9)
        assertEquals(50.0, alesEstimate(45.0, 1.0, -5.0), 1e-9)
    }

    @Test
    fun parseDecimalAcceptsCommaAndDot() {
        assertEquals(0.75, parseDecimal("0,75")!!, 1e-9)
        assertEquals(42.5, parseDecimal(" 42.5 ")!!, 1e-9)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
    }
}
