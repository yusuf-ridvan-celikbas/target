package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PracticeLog

data class PracticeLogWithTopicContext(
    @Embedded val practiceLog: PracticeLog,
    val topicId: Long,
    val topicName: String,
    val courseId: Long,
)
