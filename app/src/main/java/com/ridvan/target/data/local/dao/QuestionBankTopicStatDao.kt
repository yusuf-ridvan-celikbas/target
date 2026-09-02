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
        SELECT question_bank_topic_stats.*,
               topics.name AS topicName,
               question_banks.name AS questionBankName,
               COALESCE(attempts.testsCompleted, 0) AS testsCompleted,
               COALESCE(attempts.questionsCompleted, 0) AS questionsCompleted
        FROM question_bank_topic_stats
        JOIN topics ON topics.id = question_bank_topic_stats.topicId
        JOIN question_banks ON question_banks.id = question_bank_topic_stats.questionBankId
        LEFT JOIN (
            SELECT questionBankId, topicId,
                   COUNT(*) AS testsCompleted,
                   SUM(questionsSolved) AS questionsCompleted
            FROM test_attempts
            WHERE questionBankId IS NOT NULL
            GROUP BY questionBankId, topicId
        ) attempts ON attempts.questionBankId = question_bank_topic_stats.questionBankId
            AND attempts.topicId = question_bank_topic_stats.topicId
        WHERE question_bank_topic_stats.questionBankId = :questionBankId
        ORDER BY topics.name ASC
        """
    )
    fun getByQuestionBankId(questionBankId: Long): Flow<List<QuestionBankTopicStatProgress>>

    @Query(
        """
        SELECT question_bank_topic_stats.*,
               topics.name AS topicName,
               question_banks.name AS questionBankName,
               COALESCE(attempts.testsCompleted, 0) AS testsCompleted,
               COALESCE(attempts.questionsCompleted, 0) AS questionsCompleted
        FROM question_bank_topic_stats
        JOIN topics ON topics.id = question_bank_topic_stats.topicId
        JOIN question_banks ON question_banks.id = question_bank_topic_stats.questionBankId
        LEFT JOIN (
            SELECT questionBankId, topicId,
                   COUNT(*) AS testsCompleted,
                   SUM(questionsSolved) AS questionsCompleted
            FROM test_attempts
            WHERE questionBankId IS NOT NULL
            GROUP BY questionBankId, topicId
        ) attempts ON attempts.questionBankId = question_bank_topic_stats.questionBankId
            AND attempts.topicId = question_bank_topic_stats.topicId
        WHERE question_bank_topic_stats.topicId = :topicId
        ORDER BY question_banks.name ASC
        """
    )
    fun getByTopicId(topicId: Long): Flow<List<QuestionBankTopicStatProgress>>
}
