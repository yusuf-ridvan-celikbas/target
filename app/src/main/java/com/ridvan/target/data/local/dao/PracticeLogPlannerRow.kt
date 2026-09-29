package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PracticeLog

/** A logged Practice Session plus the names the Planner shows for it (topic, its course or language, the resource). */
data class PracticeLogPlannerRow(
    @Embedded val practiceLog: PracticeLog,
    val topicName: String,
    val courseName: String?,
    val languageName: String?,
    val studyResourceName: String,
)
