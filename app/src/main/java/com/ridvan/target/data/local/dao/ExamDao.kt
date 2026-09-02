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
        SELECT exams.*, exam_types.name AS examTypeName
        FROM exams
        JOIN exam_types ON exam_types.id = exams.examTypeId
        WHERE exams.userId = :userId
        ORDER BY exams.createdAt DESC
        """
    )
    fun getAllWithTypeByUserId(userId: Long): Flow<List<ExamWithType>>
}
