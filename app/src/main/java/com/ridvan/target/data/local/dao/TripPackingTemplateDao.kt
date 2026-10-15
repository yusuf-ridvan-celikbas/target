package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ridvan.target.data.local.entity.TripPackingTemplate
import com.ridvan.target.data.local.entity.TripPackingTemplateItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPackingTemplateDao {
    @Insert
    suspend fun insert(template: TripPackingTemplate): Long

    @Insert
    suspend fun insertItems(items: List<TripPackingTemplateItem>)

    @Query("DELETE FROM trip_packing_templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM trip_packing_templates WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun getByUserId(userId: Long): Flow<List<TripPackingTemplate>>

    @Query("SELECT * FROM trip_packing_template_items WHERE templateId = :templateId")
    suspend fun getItems(templateId: Long): List<TripPackingTemplateItem>

    @Transaction
    suspend fun saveWithItems(template: TripPackingTemplate, items: List<TripPackingTemplateItem>) {
        val id = insert(template)
        insertItems(items.map { it.copy(templateId = id) })
    }
}
