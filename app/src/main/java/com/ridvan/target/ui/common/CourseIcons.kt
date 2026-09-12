package com.ridvan.target.ui.common

import androidx.annotation.StringRes
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
import com.ridvan.target.R

data class CourseIconOption(val key: String, @StringRes val labelRes: Int, val icon: ImageVector)

object CourseIcons {
    val options: List<CourseIconOption> = listOf(
        CourseIconOption("functions", R.string.icon_math, Icons.Filled.Functions),
        CourseIconOption("calculate", R.string.icon_calculus, Icons.Filled.Calculate),
        CourseIconOption("science", R.string.icon_science, Icons.Filled.Science),
        CourseIconOption("biotech", R.string.icon_biology, Icons.Filled.Biotech),
        CourseIconOption("public", R.string.icon_geography, Icons.Filled.Public),
        CourseIconOption("history_edu", R.string.icon_history, Icons.Filled.HistoryEdu),
        CourseIconOption("language", R.string.icon_language, Icons.Filled.Language),
        CourseIconOption("menu_book", R.string.icon_literature, Icons.AutoMirrored.Filled.MenuBook),
        CourseIconOption("palette", R.string.icon_art, Icons.Filled.Palette),
        CourseIconOption("music_note", R.string.icon_music, Icons.Filled.MusicNote),
        CourseIconOption("computer", R.string.icon_computer_science, Icons.Filled.Computer),
        CourseIconOption("gavel", R.string.icon_law, Icons.Filled.Gavel),
        CourseIconOption("psychology", R.string.icon_psychology, Icons.Filled.Psychology),
        CourseIconOption("fitness_center", R.string.icon_pe, Icons.Filled.FitnessCenter),
        CourseIconOption("account_balance", R.string.icon_economics, Icons.Filled.AccountBalance),
        CourseIconOption("add", R.string.icon_other, Icons.Filled.Add),
        CourseIconOption("star", R.string.icon_favorite, Icons.Filled.Star),
    )

    val default: ImageVector = Icons.AutoMirrored.Filled.MenuBook

    fun iconFor(key: String?): ImageVector = options.firstOrNull { it.key == key }?.icon ?: default
}
