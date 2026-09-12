package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.StudyResourceTopic

data class StudyResourceTopicWithStudyResource(
    @Embedded val studyResourceTopic: StudyResourceTopic,
    val studyResourceName: String,
    val studyResourcePublisher: String?,
)
