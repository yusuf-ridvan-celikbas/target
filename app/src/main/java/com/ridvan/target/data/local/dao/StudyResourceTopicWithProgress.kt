package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.StudyResourceTopic

data class StudyResourceTopicWithProgress(
    @Embedded val studyResourceTopic: StudyResourceTopic,
    val topicName: String,
    val loggedTests: Int,
    val loggedQuestions: Int,
)
