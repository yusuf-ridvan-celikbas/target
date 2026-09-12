package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Insert
    suspend fun insert(topic: Topic): Long

    @Update
    suspend fun update(topic: Topic)

    @Delete
    suspend fun delete(topic: Topic)

    @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY name ASC")
    fun getByCourseId(courseId: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :id")
    fun getById(id: Long): Flow<Topic?>

    @Query(
        """
        SELECT topics.*,
               COALESCE(SUM(study_resource_topics.testCount), 0) AS totalTestCount,
               COALESCE(SUM(study_resource_topics.questionCount), 0) AS totalQuestionCount
        FROM topics
        LEFT JOIN study_resource_topics ON study_resource_topics.topicId = topics.id
        WHERE topics.courseId = :courseId
        GROUP BY topics.id
        ORDER BY topics.name ASC
        """
    )
    fun getTopicTotalsByCourseId(courseId: Long): Flow<List<TopicWithTotals>>
}
