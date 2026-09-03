package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.SectionCourse

data class SectionCourseWithCourse(
    @Embedded val sectionCourse: SectionCourse,
    val courseName: String,
    val courseIcon: String?,
)
