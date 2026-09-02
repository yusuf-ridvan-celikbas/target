package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.LectureNote
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureNoteDao {
    @Insert
    suspend fun insert(lectureNote: LectureNote): Long

    @Update
    suspend fun update(lectureNote: LectureNote)

    @Delete
    suspend fun delete(lectureNote: LectureNote)

    @Query("SELECT * FROM lecture_notes WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun getByTopicId(topicId: Long): Flow<List<LectureNote>>
}
