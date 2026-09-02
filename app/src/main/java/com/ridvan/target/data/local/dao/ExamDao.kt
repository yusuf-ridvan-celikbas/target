package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Insert
    suspend fun insert(exam: Exam): Long

    @Update
    suspend fun update(exam: Exam)

    @Delete
    suspend fun delete(exam: Exam)

    @Query("SELECT * FROM exams WHERE id = :examId")
    fun getById(examId: Long): Flow<Exam?>

    @Query(
        """
        SELECT exams.*,
               COUNT(topics.id) AS totalTopics,
               SUM(CASE WHEN topics.status = 'DONE' THEN 1 ELSE 0 END) AS doneTopics
        FROM exams
        LEFT JOIN topics ON topics.examId = exams.id
        GROUP BY exams.id
        ORDER BY exams.createdAt DESC
        """
    )
    fun getAllWithProgress(): Flow<List<ExamWithProgress>>
}
