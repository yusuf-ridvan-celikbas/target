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
}
