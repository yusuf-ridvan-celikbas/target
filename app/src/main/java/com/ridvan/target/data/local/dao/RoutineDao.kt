package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Routine
import com.ridvan.target.data.local.entity.RoutineStep
import com.ridvan.target.data.local.entity.RoutineStepCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Query("SELECT * FROM routines WHERE userId = :userId ORDER BY name COLLATE NOCASE")
    fun getByUserId(userId: Long): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun getById(id: Long): Flow<Routine?>

    @Query("SELECT routine_steps.* FROM routine_steps JOIN routines ON routines.id = routine_steps.routineId WHERE routines.userId = :userId")
    fun getAllStepsByUserId(userId: Long): Flow<List<RoutineStep>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex, id")
    fun getSteps(routineId: Long): Flow<List<RoutineStep>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM routine_steps WHERE routineId = :routineId")
    suspend fun nextOrderIndex(routineId: Long): Int

    @Insert
    suspend fun insertStep(step: RoutineStep): Long

    @Update
    suspend fun updateSteps(steps: List<RoutineStep>)

    @Delete
    suspend fun deleteStep(step: RoutineStep)

    @Query(
        """
        SELECT routine_step_completions.* FROM routine_step_completions
        JOIN routine_steps ON routine_steps.id = routine_step_completions.routineStepId
        JOIN routines ON routines.id = routine_steps.routineId
        WHERE routines.userId = :userId
        """
    )
    fun getAllCompletionsByUserId(userId: Long): Flow<List<RoutineStepCompletion>>

    @Query(
        """
        SELECT routine_step_completions.* FROM routine_step_completions
        JOIN routine_steps ON routine_steps.id = routine_step_completions.routineStepId
        WHERE routine_steps.routineId = :routineId
        """
    )
    fun getCompletions(routineId: Long): Flow<List<RoutineStepCompletion>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: RoutineStepCompletion)

    @Query("DELETE FROM routine_step_completions WHERE routineStepId = :stepId AND date = :date")
    suspend fun deleteCompletion(stepId: Long, date: Long)
}
