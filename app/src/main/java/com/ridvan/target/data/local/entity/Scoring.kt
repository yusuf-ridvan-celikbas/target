package com.ridvan.target.data.local.entity

/** Score systems a deneme can be scored with. */
enum class ScoringSystem { ALES, TYT, AYT }

/** The tests each score system adds up, with their question counts. */
enum class ScoringTest(val system: ScoringSystem, val questionCount: Int) {
    ALES_QUANTITATIVE(ScoringSystem.ALES, 50),
    ALES_VERBAL(ScoringSystem.ALES, 50),
    TYT_TURKISH(ScoringSystem.TYT, 40),
    TYT_SOCIAL(ScoringSystem.TYT, 20),
    TYT_MATH(ScoringSystem.TYT, 40),
    TYT_SCIENCE(ScoringSystem.TYT, 20),
    AYT_MATH(ScoringSystem.AYT, 40),
    AYT_PHYSICS(ScoringSystem.AYT, 14),
    AYT_CHEMISTRY(ScoringSystem.AYT, 13),
    AYT_BIOLOGY(ScoringSystem.AYT, 13),
    AYT_LITERATURE(ScoringSystem.AYT, 24),
    AYT_HISTORY_1(ScoringSystem.AYT, 10),
    AYT_GEOGRAPHY_1(ScoringSystem.AYT, 6),
    AYT_HISTORY_2(ScoringSystem.AYT, 11),
    AYT_GEOGRAPHY_2(ScoringSystem.AYT, 11),
    AYT_PHILOSOPHY(ScoringSystem.AYT, 12),
    AYT_RELIGION(ScoringSystem.AYT, 6),
}
