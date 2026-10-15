package com.ridvan.target.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ridvan.target.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getById(userId: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): User?

    @Query("SELECT * FROM users ORDER BY preferredName ASC")
    fun getAll(): Flow<List<User>>

    @Query("UPDATE users SET enabledModules = :modules WHERE id = :userId")
    suspend fun setEnabledModules(userId: Long, modules: String?)

    @Query("UPDATE users SET recoveryHash = :hash, recoverySalt = :salt WHERE id = :userId")
    suspend fun setRecovery(userId: Long, hash: String, salt: String)

    @Query("UPDATE users SET passwordHash = :hash, passwordSalt = :salt WHERE id = :userId")
    suspend fun setPassword(userId: Long, hash: String, salt: String)

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun countByUsername(username: String): Int
}
