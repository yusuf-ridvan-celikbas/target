package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PlannerEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerEventDao {
    @Insert
    suspend fun insert(event: PlannerEvent): Long

    @Update
    suspend fun update(event: PlannerEvent)

    @Delete
    suspend fun delete(event: PlannerEvent)

    @Query("SELECT * FROM planner_events WHERE id = :id")
    fun getById(id: Long): Flow<PlannerEvent?>

    // Joined so agenda rows can show a Study entry's course/topic name without a per-row lookup —
    // fetch-broadly-and-expand-in-Kotlin still applies (see getAllWithLinksByUserId's caller),
    // this just enriches the same broad fetch with the two extra display columns it needs.
    @Query(
        """
        SELECT planner_events.*, courses.name AS courseName, topics.name AS topicName
        FROM planner_events
        LEFT JOIN courses ON courses.id = planner_events.courseId
        LEFT JOIN topics ON topics.id = planner_events.topicId
        WHERE planner_events.userId = :userId
        ORDER BY planner_events.startDate ASC
        """
    )
    fun getAllWithLinksByUserId(userId: Long): Flow<List<PlannerEventWithLinks>>
}
