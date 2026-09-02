package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ridvan.target.data.local.entity.ExamCourse
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamCourseDao {
    @Insert
    suspend fun insert(examCourse: ExamCourse): Long

    @Delete
    suspend fun delete(examCourse: ExamCourse)

    @Query(
        """
        SELECT exam_courses.*, courses.name AS courseName
        FROM exam_courses
        JOIN courses ON courses.id = exam_courses.courseId
        WHERE exam_courses.examId = :examId
        ORDER BY courses.name ASC
        """
    )
    fun getByExamId(examId: Long): Flow<List<ExamCourseWithCourse>>
}
