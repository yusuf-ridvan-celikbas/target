package com.ridvan.target.data.local.dao

import androidx.room.Embedded
import com.ridvan.target.data.local.entity.PlannerEvent

data class PlannerEventWithLinks(
    @Embedded val event: PlannerEvent,
    val courseName: String?,
    val topicName: String?,
)
