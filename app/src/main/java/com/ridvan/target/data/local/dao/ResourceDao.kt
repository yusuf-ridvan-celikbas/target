package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Resource
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Insert
    suspend fun insert(resource: Resource): Long

    @Update
    suspend fun update(resource: Resource)

    @Delete
    suspend fun delete(resource: Resource)

    @Query("SELECT * FROM resources WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun getByTopicId(topicId: Long): Flow<List<Resource>>
}
