package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.Exam

data class ExamWithProgress(
    @Embedded val exam: Exam,
    val totalTopics: Int,
    val doneTopics: Int,
) {
    val completionPercent: Int
        get() = if (totalTopics == 0) 0 else (doneTopics * 100) / totalTopics
}
