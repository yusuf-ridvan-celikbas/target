package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ridvan.target.data.local.entity.LectureNoteReadEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureNoteReadEventDao {
    @Insert
    suspend fun insert(event: LectureNoteReadEvent): Long

    @Query("SELECT * FROM lecture_note_read_events WHERE lectureNoteId = :lectureNoteId ORDER BY readAt ASC")
    fun getByLectureNoteId(lectureNoteId: Long): Flow<List<LectureNoteReadEvent>>
}
