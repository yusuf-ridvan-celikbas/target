package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.QuestionBankTopicStat
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankTopicStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: QuestionBankTopicStat): Long

    @Update
    suspend fun update(stat: QuestionBankTopicStat)

    @Delete
    suspend fun delete(stat: QuestionBankTopicStat)

    @Query(
        """
        SELECT question_bank_topic_stats.*, topics.name AS topicName
        FROM question_bank_topic_stats
        JOIN topics ON topics.id = question_bank_topic_stats.topicId
        WHERE question_bank_topic_stats.questionBankId = :questionBankId
        ORDER BY topics.name ASC
        """
    )
    fun getByQuestionBankId(questionBankId: Long): Flow<List<QuestionBankTopicStatWithTopic>>
}
