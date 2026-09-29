package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("SELECT * FROM books WHERE id = :id")
    fun getById(id: Long): Flow<Book?>

    /** Unfinished books first, then by title. */
    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY isFinished ASC, title COLLATE NOCASE ASC")
    fun getByUserId(userId: Long): Flow<List<Book>>

    @Query("UPDATE books SET isFinished = 1 WHERE id = :id")
    suspend fun markFinished(id: Long)
}
