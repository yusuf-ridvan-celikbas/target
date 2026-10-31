package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ridvan.target.data.local.entity.Goal
import com.ridvan.target.data.local.entity.GoalLog
import com.ridvan.target.data.local.entity.GoalMilestone
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY completedAt IS NOT NULL, deadline IS NULL, deadline, createdAt")
    fun getByUserId(userId: Long): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getById(id: Long): Flow<Goal?>

    @Insert
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("SELECT goal_logs.* FROM goal_logs JOIN goals ON goals.id = goal_logs.goalId WHERE goals.userId = :userId")
    fun getAllLogsByUserId(userId: Long): Flow<List<GoalLog>>

    @Query("SELECT * FROM goal_logs WHERE goalId = :goalId ORDER BY date DESC, createdAt DESC")
    fun getLogs(goalId: Long): Flow<List<GoalLog>>

    @Insert
    suspend fun insertLog(log: GoalLog): Long

    @Update
    suspend fun updateLog(log: GoalLog)

    @Delete
    suspend fun deleteLog(log: GoalLog)

    @Query("SELECT goal_milestones.* FROM goal_milestones JOIN goals ON goals.id = goal_milestones.goalId WHERE goals.userId = :userId ORDER BY orderIndex")
    fun getAllMilestonesByUserId(userId: Long): Flow<List<GoalMilestone>>

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY orderIndex, id")
    fun getMilestones(goalId: Long): Flow<List<GoalMilestone>>

    @Insert
    suspend fun insertMilestone(milestone: GoalMilestone): Long

    @Update
    suspend fun updateMilestones(milestones: List<GoalMilestone>)

    @Delete
    suspend fun deleteMilestone(milestone: GoalMilestone)

    /** Goals with a deadline, for the reminder. */
    @Query("SELECT * FROM goals WHERE userId = :userId AND deadline IS NOT NULL AND completedAt IS NULL")
    suspend fun getOpenWithDeadline(userId: Long): List<Goal>

    @Transaction
    suspend fun insertWithMilestones(goal: Goal, milestones: List<String>): Long {
        val id = insert(goal)
        milestones.forEachIndexed { index, name -> insertMilestone(GoalMilestone(goalId = id, name = name, orderIndex = index)) }
        return id
    }
}
