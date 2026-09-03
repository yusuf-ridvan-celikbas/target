package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.Exam

data class ExamWithType(
    @Embedded val exam: Exam,
    val examTypeName: String,
    val languageName: String?,
)
