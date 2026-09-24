package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.FocusSession

data class FocusSessionWithLinks(
    @Embedded val session: FocusSession,
    val courseName: String?,
    val languageName: String?,
    val topicName: String?,
)
