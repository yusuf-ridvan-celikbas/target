package com.ridvan.target.ui.mockexam

import java.util.Locale

/** Turkish exam net: correct − wrong ÷ 4. */
fun mockExamNet(correct: Int, wrong: Int): Double = correct - wrong / 4.0

fun mockExamBlank(questions: Int, correct: Int, wrong: Int): Int = (questions - correct - wrong).coerceAtLeast(0)

fun formatNet(net: Double): String = String.format(Locale.getDefault(), "%.2f", net)
