package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R
import com.ridvan.target.data.local.entity.CourseCategory

/** For a group-header bucketing courses by category — null bucket reads "Uncategorized". */
@Composable
fun courseCategoryGroupLabel(category: CourseCategory?): String = when (category) {
    CourseCategory.QUANTITATIVE -> stringResource(R.string.category_quantitative)
    CourseCategory.VERBAL -> stringResource(R.string.category_verbal)
    null -> stringResource(R.string.category_uncategorized)
}

/** For displaying a single course's own category value — unset reads "Not set". */
@Composable
fun courseCategoryValueLabel(category: CourseCategory?): String = when (category) {
    CourseCategory.QUANTITATIVE -> stringResource(R.string.category_quantitative)
    CourseCategory.VERBAL -> stringResource(R.string.category_verbal)
    null -> stringResource(R.string.common_not_set)
}
