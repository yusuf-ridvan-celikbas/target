package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.FocusPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusPresetDao {
    @Insert
    suspend fun insert(preset: FocusPreset): Long

    @Update
    suspend fun update(preset: FocusPreset)

    @Delete
    suspend fun delete(preset: FocusPreset)

    @Query("SELECT * FROM focus_presets WHERE userId = :userId ORDER BY name ASC")
    fun getByUserId(userId: Long): Flow<List<FocusPreset>>
}
