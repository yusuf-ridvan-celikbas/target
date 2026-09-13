package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.StudyResourceTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyResourceTopicDao {
    @Insert
    suspend fun insert(studyResourceTopic: StudyResourceTopic): Long

    @Update
    suspend fun update(studyResourceTopic: StudyResourceTopic)

    @Delete
    suspend fun delete(studyResourceTopic: StudyResourceTopic)

    @Query(
        """
        SELECT study_resource_topics.*, topics.name AS topicName
        FROM study_resource_topics
        JOIN topics ON topics.id = study_resource_topics.topicId
        WHERE study_resource_topics.studyResourceId = :studyResourceId
        ORDER BY study_resource_topics.orderIndex ASC
        """
    )
    fun getByStudyResourceId(studyResourceId: Long): Flow<List<StudyResourceTopicWithTopic>>

    @Query(
        """
        SELECT study_resource_topics.*, topics.name AS topicName
        FROM study_resource_topics
        JOIN topics ON topics.id = study_resource_topics.topicId
        WHERE study_resource_topics.id = :id
        """
    )
    fun getById(id: Long): Flow<StudyResourceTopicWithTopic?>

    @Query(
        """
        SELECT study_resource_topics.*, study_resources.name AS studyResourceName, study_resources.publisher AS studyResourcePublisher
        FROM study_resource_topics
        JOIN study_resources ON study_resources.id = study_resource_topics.studyResourceId
        WHERE study_resource_topics.topicId = :topicId
        ORDER BY study_resources.name ASC, study_resources.publisher ASC, study_resources.id ASC
        """
    )
    fun getByTopicId(topicId: Long): Flow<List<StudyResourceTopicWithStudyResource>>

    @Query(
        """
        SELECT study_resource_topics.*, topics.name AS topicName,
               COALESCE(SUM(practice_logs.testsSolved), 0) AS loggedTests,
               COALESCE(SUM(practice_logs.solvedCount + practice_logs.unsolvedCount), 0) AS loggedQuestions
        FROM study_resource_topics
        JOIN topics ON topics.id = study_resource_topics.topicId
        LEFT JOIN practice_logs ON practice_logs.studyResourceTopicId = study_resource_topics.id
        WHERE study_resource_topics.studyResourceId = :studyResourceId
        GROUP BY study_resource_topics.id
        ORDER BY study_resource_topics.orderIndex ASC
        """
    )
    fun getByStudyResourceIdWithProgress(studyResourceId: Long): Flow<List<StudyResourceTopicWithProgress>>

    @Query(
        """
        SELECT COALESCE(SUM(testCount), 0) AS totalTestCount, COALESCE(SUM(questionCount), 0) AS totalQuestionCount
        FROM study_resource_topics
        WHERE studyResourceId IN (:studyResourceIds)
        """
    )
    fun getTargetTotals(studyResourceIds: List<Long>): Flow<StudyResourceTargetTotals>
}
