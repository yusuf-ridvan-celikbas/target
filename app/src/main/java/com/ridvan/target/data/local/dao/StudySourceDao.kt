package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.StudySource
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySourceDao {
    @Insert
    suspend fun insert(studySource: StudySource): Long

    @Update
    suspend fun update(studySource: StudySource)

    @Delete
    suspend fun delete(studySource: StudySource)

    @Query("SELECT * FROM study_sources WHERE courseId = :courseId ORDER BY name ASC")
    fun getByCourseId(courseId: Long): Flow<List<StudySource>>

    @Query("SELECT * FROM study_sources WHERE languageId = :languageId ORDER BY name ASC")
    fun getByLanguageId(languageId: Long): Flow<List<StudySource>>
}
