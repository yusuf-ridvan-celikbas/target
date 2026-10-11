package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.TripPackingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPackingItemDao {
    @Insert
    suspend fun insert(item: TripPackingItem): Long

    @Insert
    suspend fun insertAll(items: List<TripPackingItem>)

    @Update
    suspend fun update(item: TripPackingItem)

    @Delete
    suspend fun delete(item: TripPackingItem)

    @Query("UPDATE trip_packing_items SET isPacked = :packed WHERE id = :id")
    suspend fun setPacked(id: Long, packed: Boolean)

    @Query("UPDATE trip_packing_items SET isPacked = 0 WHERE tripId = :tripId")
    suspend fun unpackAll(tripId: Long)

    @Query("SELECT * FROM trip_packing_items WHERE tripId = :tripId ORDER BY name COLLATE NOCASE ASC")
    fun getByTripId(tripId: Long): Flow<List<TripPackingItem>>
}
