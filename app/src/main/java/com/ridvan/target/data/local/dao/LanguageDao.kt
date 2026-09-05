package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Language
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Insert
    suspend fun insert(language: Language): Long

    @Update
    suspend fun update(language: Language)

    @Delete
    suspend fun delete(language: Language)

    @Query("SELECT * FROM languages WHERE userId = :userId ORDER BY name ASC")
    fun getByUserId(userId: Long): Flow<List<Language>>

    @Query("SELECT * FROM languages WHERE id = :languageId")
    fun getById(languageId: Long): Flow<Language?>
}
