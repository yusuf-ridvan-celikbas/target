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

    @Query("SELECT * FROM topics WHERE examId = :examId AND parentTopicId IS NULL ORDER BY orderIndex ASC")
    fun getTopLevelByExamId(examId: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE parentTopicId = :parentTopicId ORDER BY orderIndex ASC")
    fun getChildren(parentTopicId: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :topicId")
    fun getById(topicId: Long): Flow<Topic?>
}
