package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PracticeExamEntryTopicResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeExamEntryTopicResultDao {
    @Insert
    suspend fun insert(topicResult: PracticeExamEntryTopicResult): Long

    @Update
    suspend fun update(topicResult: PracticeExamEntryTopicResult)

    @Delete
    suspend fun delete(topicResult: PracticeExamEntryTopicResult)

    @Query(
        """
        SELECT practice_exam_entry_topic_results.*, topics.name AS topicName
        FROM practice_exam_entry_topic_results
        JOIN topics ON topics.id = practice_exam_entry_topic_results.topicId
        WHERE practice_exam_entry_topic_results.practiceExamEntryId = :entryId
        ORDER BY topics.name ASC
        """
    )
    fun getByEntryId(entryId: Long): Flow<List<PracticeExamEntryTopicResultWithTopic>>

    @Query(
        """
        SELECT topics.id AS topicId, topics.name AS topicName, topics.courseId AS courseId, courses.name AS courseName,
               COALESCE(SUM(practice_exam_entry_topic_results.questionCount), 0) AS totalQuestionCount,
               COALESCE(SUM(practice_exam_entry_topic_results.correctCount), 0) AS totalCorrectCount,
               COALESCE(SUM(practice_exam_entry_topic_results.wrongCount), 0) AS totalWrongCount
        FROM practice_exam_entry_topic_results
        JOIN topics ON topics.id = practice_exam_entry_topic_results.topicId
        JOIN courses ON courses.id = topics.courseId
        WHERE courses.userId = :userId
        GROUP BY topics.id
        """
    )
    fun getTopicAggregatesForUser(userId: Long): Flow<List<PracticeExamTopicAggregate>>
}
