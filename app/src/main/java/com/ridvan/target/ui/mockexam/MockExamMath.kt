package com.ridvan.target.ui.mockexam

import com.ridvan.target.data.local.entity.CourseCategory
import com.ridvan.target.data.local.entity.MockExamPart
import com.ridvan.target.data.local.entity.ScoringSystem
import com.ridvan.target.data.local.entity.ScoringTest
import java.util.Locale

/** Turkish exam net: correct − wrong ÷ 4. */
fun mockExamNet(correct: Int, wrong: Int): Double = correct - wrong / 4.0

fun mockExamBlank(questions: Int, correct: Int, wrong: Int): Int = (questions - correct - wrong).coerceAtLeast(0)

fun formatNet(net: Double): String = String.format(Locale.getDefault(), "%.2f", net)


/**
 * Score types and their own approximations of ÖSYM's conversions (the real ones shift every term):
 * score = base + Σ net × weight, capped at [max]. ALES uses one term's fitted constants; TYT is
 * 100 + ~3.3/3.4 points per net (500 at full marks); YKS SAY/SÖZ/EA is 100 + 40% of the TYT points
 * above 100 + 3 points per AYT net of the score type's 80 questions (also 500 at full marks).
 */
enum class ScoreType(
    val system: ScoringSystem,
    val base: Double,
    val weights: Map<ScoringTest, Double>,
    val max: Double,
) {
    ALES_SAY(ScoringSystem.ALES, 45.561, mapOf(ScoringTest.ALES_QUANTITATIVE to 0.81185, ScoringTest.ALES_VERBAL to 0.31649), 100.0),
    ALES_SOZ(ScoringSystem.ALES, 38.001, mapOf(ScoringTest.ALES_QUANTITATIVE to 0.30187, ScoringTest.ALES_VERBAL to 1.05912), 100.0),
    ALES_EA(ScoringSystem.ALES, 43.312, mapOf(ScoringTest.ALES_QUANTITATIVE to 0.56035, ScoringTest.ALES_VERBAL to 0.65533), 100.0),
    TYT(ScoringSystem.TYT, 100.0, TYT_WEIGHTS, 500.0),
    YKS_SAY(ScoringSystem.AYT, 100.0, yksWeights(ScoringTest.AYT_MATH, ScoringTest.AYT_PHYSICS, ScoringTest.AYT_CHEMISTRY, ScoringTest.AYT_BIOLOGY), 500.0),
    YKS_SOZ(
        ScoringSystem.AYT, 100.0,
        yksWeights(
            ScoringTest.AYT_LITERATURE, ScoringTest.AYT_HISTORY_1, ScoringTest.AYT_GEOGRAPHY_1, ScoringTest.AYT_HISTORY_2,
            ScoringTest.AYT_GEOGRAPHY_2, ScoringTest.AYT_PHILOSOPHY, ScoringTest.AYT_RELIGION,
        ),
        500.0,
    ),
    YKS_EA(ScoringSystem.AYT, 100.0, yksWeights(ScoringTest.AYT_MATH, ScoringTest.AYT_LITERATURE, ScoringTest.AYT_HISTORY_1, ScoringTest.AYT_GEOGRAPHY_1), 500.0),
}

private val TYT_WEIGHTS: Map<ScoringTest, Double>
    get() = mapOf(
        ScoringTest.TYT_TURKISH to 3.3,
        ScoringTest.TYT_SOCIAL to 3.4,
        ScoringTest.TYT_MATH to 3.3,
        ScoringTest.TYT_SCIENCE to 3.4,
    )

/** TYT counts 40% (of its points above 100), the score type's AYT tests 3 points per net. */
private fun yksWeights(vararg aytTests: ScoringTest): Map<ScoringTest, Double> =
    TYT_WEIGHTS.mapValues { it.value * 0.4 } + aytTests.associateWith { 3.0 }

fun ScoringSystem.scoreTypes(): List<ScoreType> = ScoreType.entries.filter { it.system == this }

fun ScoringSystem.tests(): List<ScoringTest> = ScoringTest.entries.filter { it.system == this }

/** Nets keyed by test; a test no part counts as is simply missing (0). */
fun score(type: ScoreType, nets: Map<ScoringTest, Double>): Double =
    (type.base + type.weights.entries.sumOf { (test, weight) -> weight * (nets[test] ?: 0.0) }).coerceIn(0.0, type.max)

/** Sums each test's net over the parts that count as it. */
fun netsByTest(parts: List<MockExamPart>): Map<ScoringTest, Double> = parts
    .filter { it.scoringTest != null }
    .groupBy { it.scoringTest!! }
    .mapValues { (_, list) -> list.sumOf { mockExamNet(it.correctCount, it.wrongCount) } }

/** First guess at what a course counts as, from its category (ALES) or its name (TYT/AYT, Turkish or English). */
fun guessScoringTest(system: ScoringSystem, courseName: String?, category: CourseCategory?): ScoringTest? {
    if (system == ScoringSystem.ALES) return when (category) {
        CourseCategory.QUANTITATIVE -> ScoringTest.ALES_QUANTITATIVE
        CourseCategory.VERBAL -> ScoringTest.ALES_VERBAL
        null -> null
    }
    val name = courseName.orEmpty().lowercase(Locale.ROOT)
    fun has(vararg keys: String) = keys.any { it in name }
    return if (system == ScoringSystem.TYT) when {
        has("türk", "turk") -> ScoringTest.TYT_TURKISH
        has("sos", "social") -> ScoringTest.TYT_SOCIAL
        has("mat") -> ScoringTest.TYT_MATH
        has("fen", "scien") -> ScoringTest.TYT_SCIENCE
        else -> null
    } else when {
        has("mat") -> ScoringTest.AYT_MATH
        has("fiz", "phys") -> ScoringTest.AYT_PHYSICS
        has("kim", "chem") -> ScoringTest.AYT_CHEMISTRY
        has("biy", "biol") -> ScoringTest.AYT_BIOLOGY
        has("edeb", "türk", "turk", "lit") -> ScoringTest.AYT_LITERATURE
        has("tarih", "hist") -> if (has("2")) ScoringTest.AYT_HISTORY_2 else ScoringTest.AYT_HISTORY_1
        has("coğ", "cog", "geog") -> if (has("2")) ScoringTest.AYT_GEOGRAPHY_2 else ScoringTest.AYT_GEOGRAPHY_1
        has("fels", "phil") -> ScoringTest.AYT_PHILOSOPHY
        has("din", "relig") -> ScoringTest.AYT_RELIGION
        else -> null
    }
}
