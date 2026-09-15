package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Insert
    suspend fun insert(section: Section): Long

    @Update
    suspend fun update(section: Section)

    @Delete
    suspend fun delete(section: Section)

    @Query("SELECT * FROM sections WHERE examId = :examId ORDER BY orderIndex ASC")
    fun getByExamId(examId: Long): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE id = :sectionId")
    fun getById(sectionId: Long): Flow<Section?>

    @Query(
        """
        SELECT sections.* FROM sections
        JOIN exams ON exams.id = sections.examId
        WHERE exams.userId = :userId
        ORDER BY sections.examId, sections.orderIndex ASC
        """
    )
    fun getByUserId(userId: Long): Flow<List<Section>>
}
