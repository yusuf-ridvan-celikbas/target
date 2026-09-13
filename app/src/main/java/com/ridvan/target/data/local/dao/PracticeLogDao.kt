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
}
