package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert
    suspend fun insert(course: Course): Long

    @Update
    suspend fun update(course: Course)

    @Delete
    suspend fun delete(course: Course)

    @Query("SELECT * FROM courses WHERE userId = :userId ORDER BY name ASC")
    fun getByUserId(userId: Long): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :courseId")
    fun getById(courseId: Long): Flow<Course?>
}
