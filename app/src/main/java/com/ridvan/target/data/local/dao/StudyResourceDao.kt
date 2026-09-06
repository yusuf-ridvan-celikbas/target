package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.StudyResource
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyResourceDao {
    @Insert
    suspend fun insert(studyResource: StudyResource): Long

    @Update
    suspend fun update(studyResource: StudyResource)

    @Delete
    suspend fun delete(studyResource: StudyResource)

    @Query("SELECT * FROM study_resources WHERE courseId = :courseId ORDER BY name ASC")
    fun getByCourseId(courseId: Long): Flow<List<StudyResource>>

    @Query("SELECT * FROM study_resources WHERE languageId = :languageId ORDER BY name ASC")
    fun getByLanguageId(languageId: Long): Flow<List<StudyResource>>

    @Query("SELECT * FROM study_resources WHERE id = :id")
    fun getById(id: Long): Flow<StudyResource?>
}
