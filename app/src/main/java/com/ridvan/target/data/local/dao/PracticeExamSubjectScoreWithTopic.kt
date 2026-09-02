package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PracticeExamSubjectScore

data class PracticeExamSubjectScoreWithTopic(
    @Embedded val score: PracticeExamSubjectScore,
    val subjectName: String,
)
