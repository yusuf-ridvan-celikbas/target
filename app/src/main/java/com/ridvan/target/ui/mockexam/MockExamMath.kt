package com.ridvan.target.ui.mockexam

import com.ridvan.target.data.local.entity.Exam
import java.util.Locale

/** Turkish exam net: correct − wrong ÷ 4. */
fun mockExamNet(correct: Int, wrong: Int): Double = correct - wrong / 4.0

fun mockExamBlank(questions: Int, correct: Int, wrong: Int): Int = (questions - correct - wrong).coerceAtLeast(0)

fun formatNet(net: Double): String = String.format(Locale.getDefault(), "%.2f", net)

/** ALES score types and how much the Quantitative (Math) and Verbal nets weigh in each. */
enum class AlesScoreType(val quantitativeWeight: Double, val verbalWeight: Double) {
    QUANTITATIVE(0.75, 0.25),
    VERBAL(0.25, 0.75),
    EQUAL_WEIGHT(0.5, 0.5),
}

fun alesWeightedNet(type: AlesScoreType, quantitativeNet: Double, verbalNet: Double): Double =
    type.quantitativeWeight * quantitativeNet + type.verbalWeight * verbalNet

/**
 * ÖSYM converts weighted nets with term-specific means/deviations the app can't know offline,
 * so the estimate uses the user's own base + coefficient and is kept inside ALES's 50–100 range.
 */
fun alesEstimate(base: Double, coefficient: Double, weightedNet: Double): Double =
    (base + coefficient * weightedNet).coerceIn(50.0, 100.0)

/** Accepts "0,75" or "0.75"; null when blank or not a number. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** The user's base/coefficient pair for a score type, or null while either is missing. */
fun Exam.alesConversion(type: AlesScoreType): Pair<Double, Double>? {
    val (base, coef) = when (type) {
        AlesScoreType.QUANTITATIVE -> alesSayBase to alesSayCoef
        AlesScoreType.VERBAL -> alesSozBase to alesSozCoef
        AlesScoreType.EQUAL_WEIGHT -> alesEaBase to alesEaCoef
    }
    return if (base != null && coef != null) base to coef else null
}
