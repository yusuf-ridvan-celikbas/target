package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ridvan.target.data.local.entity.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE topicId = :topicId ORDER BY date DESC")
    fun getByTopicId(topicId: Long): Flow<List<DailyLog>>
}
