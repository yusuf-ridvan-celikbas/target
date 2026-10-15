package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ridvan.target.data.local.entity.SportGoal
import com.ridvan.target.data.local.entity.Workout
import com.ridvan.target.data.local.entity.WorkoutExercise
import com.ridvan.target.data.local.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

/** An exercise with its sets, as edited in the form and stored. */
data class ExerciseWithSets(val exercise: WorkoutExercise, val sets: List<WorkoutSet>)

/** One logged set joined to its exercise name and workout date, for personal bests. */
data class LiftRow(val exerciseName: String, val reps: Int, val weightKg: Double, val startedAt: Long, val workoutId: Long)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    @Insert
    suspend fun insertExercise(exercise: WorkoutExercise): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercises(workoutId: Long)

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY startedAt DESC")
    fun getByUserId(userId: Long): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getById(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex, id")
    fun getExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex, id")
    suspend fun getExercisesOnce(workoutId: Long): List<WorkoutExercise>

    @Query(
        """
        SELECT s.* FROM workout_sets s JOIN workout_exercises e ON e.id = s.exerciseId
        WHERE e.workoutId = :workoutId ORDER BY s.orderIndex, s.id
        """
    )
    fun getSets(workoutId: Long): Flow<List<WorkoutSet>>

    @Query(
        """
        SELECT s.* FROM workout_sets s JOIN workout_exercises e ON e.id = s.exerciseId
        WHERE e.workoutId = :workoutId ORDER BY s.orderIndex, s.id
        """
    )
    suspend fun getSetsOnce(workoutId: Long): List<WorkoutSet>

    @Query(
        """
        SELECT e.name AS exerciseName, s.reps AS reps, s.weightKg AS weightKg, w.startedAt AS startedAt, w.id AS workoutId
        FROM workout_sets s JOIN workout_exercises e ON e.id = s.exerciseId JOIN workouts w ON w.id = e.workoutId
        WHERE w.userId = :userId AND s.weightKg > 0
        """
    )
    fun getLiftRows(userId: Long): Flow<List<LiftRow>>

    @Insert
    suspend fun insertGoal(goal: SportGoal): Long

    @Delete
    suspend fun deleteGoal(goal: SportGoal)

    @Query("SELECT * FROM sport_goals WHERE userId = :userId ORDER BY id")
    fun getGoals(userId: Long): Flow<List<SportGoal>>

    /** Replaces a workout's exercises with [exercises]; ids are regenerated. */
    @Transaction
    suspend fun replaceExercises(workoutId: Long, exercises: List<ExerciseWithSets>) {
        deleteExercises(workoutId)
        exercises.forEachIndexed { index, item ->
            val exerciseId = insertExercise(WorkoutExercise(workoutId = workoutId, name = item.exercise.name, orderIndex = index))
            insertSets(item.sets.mapIndexed { i, set -> set.copy(id = 0, exerciseId = exerciseId, orderIndex = i) })
        }
    }

    /** Inserts [workout] (keeping its id when non-zero, e.g. for Undo) with its exercises. */
    @Transaction
    suspend fun insertWithExercises(workout: Workout, exercises: List<ExerciseWithSets>): Long {
        val id = insert(workout)
        replaceExercises(id, exercises)
        return id
    }

    /** Exercises with their sets, for Undo of a delete. */
    @Transaction
    suspend fun loadExercises(workoutId: Long): List<ExerciseWithSets> {
        val sets = getSetsOnce(workoutId).groupBy { it.exerciseId }
        return getExercisesOnce(workoutId).map { ExerciseWithSets(it, sets[it.id].orEmpty()) }
    }
}
