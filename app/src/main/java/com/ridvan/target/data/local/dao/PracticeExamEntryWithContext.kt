package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PracticeExamEntry

data class PracticeExamEntryWithContext(
    @Embedded val entry: PracticeExamEntry,
    val studyResourceName: String,
    val publisher: String?,
    val courseId: Long?,
    val courseName: String?,
)
