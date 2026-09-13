package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PracticeExamEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeExamEntryDao {
    @Insert
    suspend fun insert(entry: PracticeExamEntry): Long

    @Update
    suspend fun update(entry: PracticeExamEntry)

    @Delete
    suspend fun delete(entry: PracticeExamEntry)

    @Query("SELECT * FROM practice_exam_entries WHERE id = :id")
    fun getById(id: Long): Flow<PracticeExamEntry?>

    @Query("SELECT * FROM practice_exam_entries WHERE studyResourceId = :studyResourceId ORDER BY createdAt ASC")
    fun getByStudyResourceId(studyResourceId: Long): Flow<List<PracticeExamEntry>>

    @Query(
        """
        SELECT practice_exam_entries.*,
               study_resources.name AS studyResourceName,
               study_resources.publisher AS publisher,
               study_resources.courseId AS courseId,
               courses.name AS courseName
        FROM practice_exam_entries
        JOIN study_resources ON study_resources.id = practice_exam_entries.studyResourceId
        LEFT JOIN courses ON courses.id = study_resources.courseId
        WHERE (study_resources.courseId IS NOT NULL AND courses.userId = :userId)
           OR (study_resources.languageId IS NOT NULL AND study_resources.languageId IN (
                SELECT id FROM languages WHERE languages.userId = :userId
           ))
        ORDER BY practice_exam_entries.createdAt ASC
        """
    )
    fun getAllForUser(userId: Long): Flow<List<PracticeExamEntryWithContext>>
}
