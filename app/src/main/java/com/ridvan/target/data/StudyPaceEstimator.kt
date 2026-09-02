package com.ridvan.target.data

import com.ridvan.target.data.local.entity.TestAttempt
import kotlin.math.ceil
import kotlin.math.roundToInt

data class StudyPaceEstimate(
    val remainingTests: Int,
    val estimatedDays: Int,
    val estimatedMinutes: Int,
)

fun estimateRemainingPace(
    recentAttempts: List<TestAttempt>,
    windowDays: Int,
    remainingTests: Int,
): StudyPaceEstimate? {
    if (remainingTests <= 0 || recentAttempts.isEmpty()) return null

    val testsPerDay = recentAttempts.size / windowDays.toDouble()
    val avgMinutesPerTest = recentAttempts
        .map { (it.finishedAt - it.startedAt) / 60_000.0 }
        .average()

    return StudyPaceEstimate(
        remainingTests = remainingTests,
        estimatedDays = ceil(remainingTests / testsPerDay).toInt(),
        estimatedMinutes = (remainingTests * avgMinutesPerTest).roundToInt(),
    )
}
