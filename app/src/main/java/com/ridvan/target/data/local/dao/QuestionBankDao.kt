package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.QuestionBank
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankDao {
    @Insert
    suspend fun insert(questionBank: QuestionBank): Long

    @Update
    suspend fun update(questionBank: QuestionBank)

    @Delete
    suspend fun delete(questionBank: QuestionBank)

    @Query("SELECT * FROM question_banks WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun getByTopicId(topicId: Long): Flow<List<QuestionBank>>
}
