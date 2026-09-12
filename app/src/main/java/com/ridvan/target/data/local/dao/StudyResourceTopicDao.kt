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
        SELECT study_resource_topics.*, study_resources.name AS studyResourceName, study_resources.publisher AS studyResourcePublisher
        FROM study_resource_topics
        JOIN study_resources ON study_resources.id = study_resource_topics.studyResourceId
        WHERE study_resource_topics.topicId = :topicId
        ORDER BY study_resources.name ASC
        """
    )
    fun getByTopicId(topicId: Long): Flow<List<StudyResourceTopicWithStudyResource>>
}
