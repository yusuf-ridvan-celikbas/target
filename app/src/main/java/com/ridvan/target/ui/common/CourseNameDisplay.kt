package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R

/**
 * Translates a Course's free-text name for display when it matches a known, common academic
 * subject — e.g. "Geometry" -> "Geometri" in Turkish. Course names are otherwise free text
 * (see CLAUDE.md), so anything not in this fixed list is shown exactly as the user typed it.
 * Never use this for comparisons or for an editable field's initial value — only for read-only
 * display, the same rule as examTypeDisplayName().
 */
@Composable
fun courseDisplayName(name: String): String = when (name.trim()) {
    "Mathematics" -> stringResource(R.string.course_name_mathematics)
    "Advanced Mathematics" -> stringResource(R.string.course_name_advanced_mathematics)
    "Geometry" -> stringResource(R.string.course_name_geometry)
    "Physics" -> stringResource(R.string.course_name_physics)
    "Chemistry" -> stringResource(R.string.course_name_chemistry)
    "Biology" -> stringResource(R.string.course_name_biology)
    "Science" -> stringResource(R.string.course_name_science)
    "Turkish" -> stringResource(R.string.course_name_turkish)
    "History" -> stringResource(R.string.course_name_history)
    "Geography" -> stringResource(R.string.course_name_geography)
    "Literature" -> stringResource(R.string.course_name_literature)
    "Philosophy" -> stringResource(R.string.course_name_philosophy)
    "Social Studies" -> stringResource(R.string.course_name_social_studies)
    "Religious Culture and Moral Knowledge" -> stringResource(R.string.course_name_religious_culture)
    "Art" -> stringResource(R.string.course_name_art)
    "Music" -> stringResource(R.string.course_name_music)
    "Computer Science" -> stringResource(R.string.course_name_computer_science)
    "Law" -> stringResource(R.string.course_name_law)
    "Psychology" -> stringResource(R.string.course_name_psychology)
    "PE" -> stringResource(R.string.course_name_pe)
    "Economics" -> stringResource(R.string.course_name_economics)
    "English" -> stringResource(R.string.course_name_english)
    else -> name
}
