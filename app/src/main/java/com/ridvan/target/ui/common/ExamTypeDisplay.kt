package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.dao.LANGUAGE_EXAM_TYPE_NAME

/**
 * Maps a seeded ExamType.name (always stored/compared in English — see LANGUAGE_EXAM_TYPE_NAME)
 * to its localized display string. Only for rendering; never use this for comparisons.
 */
@Composable
fun examTypeDisplayName(name: String): String = when (name) {
    "University Entrance Exam" -> stringResource(R.string.exam_type_university_entrance)
    "High School Entrance Exam" -> stringResource(R.string.exam_type_high_school_entrance)
    LANGUAGE_EXAM_TYPE_NAME -> stringResource(R.string.exam_type_language_exam)
    "Vocational Exam" -> stringResource(R.string.exam_type_vocational_exam)
    else -> name
}
