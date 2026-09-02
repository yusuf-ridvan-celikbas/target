package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.ExamCourse

data class ExamCourseWithCourse(
    @Embedded val examCourse: ExamCourse,
    val courseName: String,
)
