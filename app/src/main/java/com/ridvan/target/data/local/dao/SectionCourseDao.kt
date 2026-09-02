package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ridvan.target.data.local.entity.SectionCourse
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionCourseDao {
    @Insert
    suspend fun insert(sectionCourse: SectionCourse): Long

    @Delete
    suspend fun delete(sectionCourse: SectionCourse)

    @Query(
        """
        SELECT section_courses.*, courses.name AS courseName
        FROM section_courses
        JOIN courses ON courses.id = section_courses.courseId
        WHERE section_courses.sectionId = :sectionId
        ORDER BY courses.name ASC
        """
    )
    fun getBySectionId(sectionId: Long): Flow<List<SectionCourseWithCourse>>
}
