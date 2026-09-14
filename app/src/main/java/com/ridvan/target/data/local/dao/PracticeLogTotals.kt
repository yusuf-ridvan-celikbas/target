package com.ridvan.target.data.local.dao

data class PracticeLogTotals(
    val totalTestsSolved: Int,
    val totalSolved: Int,
    val totalUnsolved: Int,
    val totalDurationMinutes: Int,
    val totalQuestionsLogged: Int,
)
