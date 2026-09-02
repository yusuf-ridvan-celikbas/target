package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PracticeExam
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeExamDao {
    @Insert
    suspend fun insert(practiceExam: PracticeExam): Long

    @Update
    suspend fun update(practiceExam: PracticeExam)

    @Delete
    suspend fun delete(practiceExam: PracticeExam)

    @Query("SELECT * FROM practice_exams WHERE examId = :examId ORDER BY scheduledDate ASC")
    fun getByExamId(examId: Long): Flow<List<PracticeExam>>
}
