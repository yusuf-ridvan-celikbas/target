package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ridvan.target.data.local.entity.PlannerEventCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerEventCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: PlannerEventCompletion)

    @Query("DELETE FROM planner_event_completions WHERE plannerEventId = :plannerEventId AND occurrenceDate = :occurrenceDate")
    suspend fun deleteByEventAndDate(plannerEventId: Long, occurrenceDate: Long)

    @Query(
        """
        SELECT planner_event_completions.* FROM planner_event_completions
        JOIN planner_events ON planner_events.id = planner_event_completions.plannerEventId
        WHERE planner_events.userId = :userId
        """
    )
    fun getAllByUserId(userId: Long): Flow<List<PlannerEventCompletion>>
}
