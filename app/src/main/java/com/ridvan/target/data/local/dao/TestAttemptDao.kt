package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.TestAttempt
import kotlinx.coroutines.flow.Flow

@Dao
interface TestAttemptDao {
    @Insert
    suspend fun insert(attempt: TestAttempt): Long

    @Update
    suspend fun update(attempt: TestAttempt)

    @Delete
    suspend fun delete(attempt: TestAttempt)

    @Query("SELECT * FROM test_attempts WHERE topicId = :topicId ORDER BY startedAt DESC")
    fun getByTopicId(topicId: Long): Flow<List<TestAttempt>>

    @Query(
        "SELECT * FROM test_attempts WHERE topicId = :topicId AND startedAt >= :sinceEpochMillis " +
            "ORDER BY startedAt DESC"
    )
    fun getRecentByTopicId(topicId: Long, sinceEpochMillis: Long): Flow<List<TestAttempt>>
}
