package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PracticeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeLogDao {
    @Insert
    suspend fun insert(log: PracticeLog): Long

    @Update
    suspend fun update(log: PracticeLog)

    @Delete
    suspend fun delete(log: PracticeLog)

    @Query("SELECT * FROM practice_logs WHERE studyResourceTopicId = :studyResourceTopicId ORDER BY loggedAt DESC")
    fun getByStudyResourceTopicId(studyResourceTopicId: Long): Flow<List<PracticeLog>>

    @Query(
        """
        SELECT COALESCE(SUM(practice_logs.testsSolved), 0) AS totalTestsSolved,
               COALESCE(SUM(practice_logs.solvedCount), 0) AS totalSolved,
               COALESCE(SUM(practice_logs.unsolvedCount), 0) AS totalUnsolved,
               COALESCE(SUM(practice_logs.durationMinutes), 0) AS totalDurationMinutes
        FROM practice_logs
        JOIN study_resource_topics ON study_resource_topics.id = practice_logs.studyResourceTopicId
        WHERE study_resource_topics.studyResourceId IN (:studyResourceIds)
        """
    )
    fun getProgressTotals(studyResourceIds: List<Long>): Flow<PracticeLogTotals>

    @Query(
        """
        SELECT topics.id AS topicId,
               COALESCE(SUM(practice_logs.testsSolved), 0) AS totalTestsSolved,
               COALESCE(SUM(practice_logs.solvedCount), 0) AS totalSolved,
               COALESCE(SUM(practice_logs.unsolvedCount), 0) AS totalUnsolved,
               COALESCE(SUM(practice_logs.durationMinutes), 0) AS totalDurationMinutes
        FROM topics
        LEFT JOIN study_resource_topics ON study_resource_topics.topicId = topics.id
        LEFT JOIN practice_logs ON practice_logs.studyResourceTopicId = study_resource_topics.id
        WHERE topics.id IN (:topicIds)
        GROUP BY topics.id
        """
    )
    fun getProgressByTopicIds(topicIds: List<Long>): Flow<List<TopicProgressByTopic>>

    @Query(
        """
        SELECT practice_logs.*, topics.id AS topicId, topics.name AS topicName, topics.courseId AS courseId
        FROM practice_logs
        JOIN study_resource_topics ON study_resource_topics.id = practice_logs.studyResourceTopicId
        JOIN topics ON topics.id = study_resource_topics.topicId
        JOIN courses ON courses.id = topics.courseId
        WHERE courses.userId = :userId
        ORDER BY practice_logs.loggedAt ASC
        """
    )
    fun getAllForUser(userId: Long): Flow<List<PracticeLogWithTopicContext>>
}
