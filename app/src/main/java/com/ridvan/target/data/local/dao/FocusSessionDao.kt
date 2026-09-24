package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    @Delete
    suspend fun delete(session: FocusSession)

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId ORDER BY startedAt DESC")
    fun getByUserId(userId: Long): Flow<List<FocusSession>>
}
