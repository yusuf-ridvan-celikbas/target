package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    @Update
    suspend fun update(session: FocusSession)

    @Delete
    suspend fun delete(session: FocusSession)

    @Query(
        """
        SELECT focus_sessions.*, courses.name AS courseName, languages.name AS languageName, topics.name AS topicName
        FROM focus_sessions
        LEFT JOIN courses ON courses.id = focus_sessions.courseId
        LEFT JOIN languages ON languages.id = focus_sessions.languageId
        LEFT JOIN topics ON topics.id = focus_sessions.topicId
        WHERE focus_sessions.id = :id
        """
    )
    fun getByIdWithLinks(id: Long): Flow<FocusSessionWithLinks?>

    @Query(
        """
        SELECT focus_sessions.*, courses.name AS courseName, languages.name AS languageName, topics.name AS topicName
        FROM focus_sessions
        LEFT JOIN courses ON courses.id = focus_sessions.courseId
        LEFT JOIN languages ON languages.id = focus_sessions.languageId
        LEFT JOIN topics ON topics.id = focus_sessions.topicId
        WHERE focus_sessions.userId = :userId
        ORDER BY focus_sessions.startedAt DESC
        """
    )
    fun getAllWithLinksByUserId(userId: Long): Flow<List<FocusSessionWithLinks>>
}
