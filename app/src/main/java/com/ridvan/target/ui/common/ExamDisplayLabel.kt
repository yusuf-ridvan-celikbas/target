package com.ridvan.target.ui.common

import com.ridvan.target.data.local.entity.Exam

/**
 * "Goethe" + level "A2" -> "Goethe A2" for a Language Exam whose level is set;
 * just the raw name otherwise. Plain string composition, not a translation, so
 * this is safe to use anywhere the underlying name would otherwise be shown
 * (list rows, screen titles, Home's Upcoming card).
 */
fun examDisplayLabel(exam: Exam): String {
    val level = exam.level?.trim()
    return if (!level.isNullOrEmpty()) "${exam.name} $level" else exam.name
}
