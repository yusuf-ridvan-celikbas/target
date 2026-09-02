package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.PracticeExamSubjectScore
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeExamSubjectScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: PracticeExamSubjectScore): Long

    @Update
    suspend fun update(score: PracticeExamSubjectScore)

    @Delete
    suspend fun delete(score: PracticeExamSubjectScore)

    @Query(
        """
        SELECT practice_exam_subject_scores.*, topics.name AS subjectName
        FROM practice_exam_subject_scores
        JOIN topics ON topics.id = practice_exam_subject_scores.subjectTopicId
        WHERE practice_exam_subject_scores.practiceExamId = :practiceExamId
        ORDER BY topics.name ASC
        """
    )
    fun getByPracticeExamId(practiceExamId: Long): Flow<List<PracticeExamSubjectScoreWithTopic>>
}
