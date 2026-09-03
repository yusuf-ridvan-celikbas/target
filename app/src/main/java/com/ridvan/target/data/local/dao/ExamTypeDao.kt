package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ridvan.target.data.local.entity.ExamType
import kotlinx.coroutines.flow.Flow

const val LANGUAGE_EXAM_TYPE_NAME = "Language Exam"

private val DEFAULT_EXAM_TYPE_NAMES = listOf(
    "University Entrance Exam",
    "High School Entrance Exam",
    LANGUAGE_EXAM_TYPE_NAME,
    "Vocational Exam",
)

@Dao
interface ExamTypeDao {
    @Query("SELECT * FROM exam_types ORDER BY id ASC")
    fun getAll(): Flow<List<ExamType>>

    @Query("SELECT COUNT(*) FROM exam_types")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(types: List<ExamType>)

    @Transaction
    suspend fun seedDefaultsIfEmpty() {
        if (count() == 0) {
            insertAll(DEFAULT_EXAM_TYPE_NAMES.map { ExamType(name = it) })
        }
    }
}
