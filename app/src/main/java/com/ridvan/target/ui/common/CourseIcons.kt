package com.ridvan.target.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class CourseIconOption(val key: String, val label: String, val icon: ImageVector)

object CourseIcons {
    val options: List<CourseIconOption> = listOf(
        CourseIconOption("functions", "Math", Icons.Filled.Functions),
        CourseIconOption("calculate", "Calculus", Icons.Filled.Calculate),
        CourseIconOption("science", "Science", Icons.Filled.Science),
        CourseIconOption("biotech", "Biology", Icons.Filled.Biotech),
        CourseIconOption("public", "Geography", Icons.Filled.Public),
        CourseIconOption("history_edu", "History", Icons.Filled.HistoryEdu),
        CourseIconOption("language", "Language", Icons.Filled.Language),
        CourseIconOption("menu_book", "Literature", Icons.AutoMirrored.Filled.MenuBook),
        CourseIconOption("palette", "Art", Icons.Filled.Palette),
        CourseIconOption("music_note", "Music", Icons.Filled.MusicNote),
        CourseIconOption("computer", "Computer Science", Icons.Filled.Computer),
        CourseIconOption("gavel", "Law", Icons.Filled.Gavel),
        CourseIconOption("psychology", "Psychology", Icons.Filled.Psychology),
        CourseIconOption("fitness_center", "PE", Icons.Filled.FitnessCenter),
        CourseIconOption("account_balance", "Economics", Icons.Filled.AccountBalance),
        CourseIconOption("add", "Other (+)", Icons.Filled.Add),
        CourseIconOption("star", "Favorite", Icons.Filled.Star),
    )

    val default: ImageVector = Icons.AutoMirrored.Filled.MenuBook

    fun iconFor(key: String?): ImageVector = options.firstOrNull { it.key == key }?.icon ?: default
}
