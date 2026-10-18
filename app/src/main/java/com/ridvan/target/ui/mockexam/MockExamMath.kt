package com.ridvan.target.ui.mockexam

import java.util.Locale

/** Turkish exam net: correct − wrong ÷ 4. */
fun mockExamNet(correct: Int, wrong: Int): Double = correct - wrong / 4.0

fun mockExamBlank(questions: Int, correct: Int, wrong: Int): Int = (questions - correct - wrong).coerceAtLeast(0)

fun formatNet(net: Double): String = String.format(Locale.getDefault(), "%.2f", net)

/**
 * ALES score types with ÖSYM's 2026-2 term conversion: score = base + Quantitative net × quantitative
 * + Verbal net × verbal, capped at 100. Fitted to ales-puan.hesaplama.net (matches it to 4 decimals);
 * the real conversion shifts a little every term.
 */
enum class AlesScoreType(val base: Double, val quantitative: Double, val verbal: Double) {
    QUANTITATIVE(45.56100, 0.81185475, 0.31648600),
    VERBAL(38.00059, 0.30187425, 1.05911925),
    EQUAL_WEIGHT(43.31196, 0.56035075, 0.65532600),
}

fun alesScore(type: AlesScoreType, quantitativeNet: Double, verbalNet: Double): Double =
    (type.base + type.quantitative * quantitativeNet + type.verbal * verbalNet).coerceIn(0.0, 100.0)
